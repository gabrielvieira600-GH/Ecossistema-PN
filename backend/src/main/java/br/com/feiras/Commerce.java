package br.com.feiras;

import static br.com.feiras.Db.*;

import java.awt.geom.*;
import java.math.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Commerce {
  final Db db;

  public Commerce(Db db) {
    this.db = db;
  }

  public record Selection(
      List<String> ids,
      Map<String, Long> versions,
      boolean assembly,
      String quoteHash,
      boolean acceptTerms) {}

  private List<Map<String, Object>> select(Selection request) {
    if (request.ids() == null
        || request.ids().isEmpty()
        || request.ids().size() > 20
        || new HashSet<>(request.ids()).size() != request.ids().size())
      throw error(HttpStatus.BAD_REQUEST, "Selecione de 1 a 20 estandes distintos.");
    List<String> ids = new ArrayList<>(request.ids());
    Collections.sort(ids);
    String fair = db.fairOf(ids.get(0));
    db.lockFair(fair);
    expireFair(fair);
    List<Map<String, Object>> selected = new ArrayList<>();
    String pavilion = null;
    for (String id : ids) {
      var b = db.one("SELECT * FROM booth WHERE id=? AND active=TRUE", id);
      if (pavilion == null) pavilion = (String) b.get("pavilion_id");
      else if (!pavilion.equals(b.get("pavilion_id")))
        throw error(HttpStatus.BAD_REQUEST, "Selecione estandes do mesmo pavilhão.");
      if (request.versions() == null
          || !Objects.equals(request.versions().get(id), ((Number) b.get("version")).longValue()))
        throw error(HttpStatus.CONFLICT, "O estande mudou. Atualize a seleção e o orçamento.");
      selected.add(b);
    }
    for (var b : selected)
      if (b.get("group_id") != null) {
        var group =
            db.sql.queryForList(
                "SELECT id FROM booth WHERE group_id=? AND active=TRUE", b.get("group_id"));
        for (var member : group)
          if (!ids.contains(member.get("id")))
            throw error(HttpStatus.CONFLICT, "Selecione todos os estandes desta união.");
      }
    return selected;
  }

  private BigDecimal decimal(Map<String, Object> row, String key) {
    return (BigDecimal) row.get(key);
  }

  private BigDecimal money(BigDecimal amount) {
    return amount.setScale(2, RoundingMode.HALF_UP);
  }

  public Map<String, Object> quoteData(
      List<Map<String, Object>> booths, boolean assembly, Auth.User user) {
    String fairId = db.fairOf((String) booths.get(0).get("id"));
    var fair = db.one("SELECT * FROM fair WHERE id=?", fairId);
    var pavilion = db.one("SELECT * FROM pavilion WHERE id=?", booths.get(0).get("pavilion_id"));
    boolean ready = (Boolean) fair.get("published") && (Boolean) pavilion.get("reviewed");
    BigDecimal area = BigDecimal.ZERO;
    List<Map<String, Object>> items = new ArrayList<>();
    for (var b : booths) {
      if (b.get("area") == null || !Boolean.TRUE.equals(b.get("verified"))) ready = false;
      else area = area.add(decimal(b, "area"));
      var item = new LinkedHashMap<String, Object>();
      item.put("id", b.get("id"));
      item.put("code", b.get("code"));
      item.put("area", b.get("area"));
      item.put("version", b.get("version"));
      items.add(item);
    }
    BigDecimal space = money(area.multiply(decimal(fair, "rate"))),
        mounting =
            assembly
                ? money(area.multiply(decimal(fair, "assembly_rate")))
                : new BigDecimal("0.00"),
        fee = decimal(fair, "fixed_fee");
    BigDecimal subtotal = space.add(mounting).add(fee),
        tax = money(subtotal.multiply(decimal(fair, "tax_percent")).divide(new BigDecimal("100")));
    var quote = new LinkedHashMap<String, Object>();
    quote.put("fair", fair.get("name"));
    quote.put("fairId", fairId);
    quote.put("pavilion", pavilion.get("name"));
    quote.put("company", user.company());
    quote.put("userId", user.id());
    quote.put("items", items);
    quote.put("area", area);
    quote.put("rate", fair.get("rate"));
    quote.put("space", space);
    quote.put("assemblyRate", fair.get("assembly_rate"));
    quote.put("assembly", assembly);
    quote.put("mounting", mounting);
    quote.put("fee", fee);
    quote.put("taxPercent", fair.get("tax_percent"));
    quote.put("tax", tax);
    quote.put("total", subtotal.add(tax));
    quote.put("terms", fair.get("terms"));
    quote.put("fairVersion", fair.get("version"));
    quote.put("ready", ready);
    quote.put("reserveHours", fair.get("reserve_hours"));
    quote.put("hash", Auth.digest(db.encode(quote)));
    return quote;
  }

  @Transactional
  public Map<String, Object> quote(Selection request, Auth.User user) {
    return quoteData(select(request), request.assembly(), user);
  }

  private void sellable(List<Map<String, Object>> booths) {
    var fair = db.one("SELECT published FROM fair WHERE id=?", db.fairOf((String) booths.get(0).get("id")));
    if (!Boolean.TRUE.equals(fair.get("published")))
      throw error(HttpStatus.CONFLICT, "A comercialização desta feira está fechada. Aguarde a publicação das condições.");
    for (var b : booths) {
      if (!(Boolean) b.get("verified") || b.get("area") == null)
        throw error(
            HttpStatus.CONFLICT,
            "O administrador precisa conferir a metragem e a geometria deste estande.");
      var p = db.one("SELECT reviewed FROM pavilion WHERE id=?", b.get("pavilion_id"));
      if (!(Boolean) p.get("reviewed"))
        throw error(HttpStatus.CONFLICT, "Esta planta ainda está em conferência administrativa.");
    }
  }

  @Transactional
  public void reserve(Selection request, Auth.User user, boolean merging) {
    var booths = select(request);
    sellable(booths);
    if (booths.size() > 1 && !connected(booths))
      throw error(
          HttpStatus.BAD_REQUEST,
          "Só é possível unir estandes com uma lateral compartilhada, sem atravessar ruas.");
    boolean hasOwn = false;
    for (var b : booths) {
      boolean own = user.id().equals(b.get("owner_id")) && b.get("status").equals("RESERVED");
      hasOwn |= own;
      if (!b.get("status").equals("AVAILABLE") && !own)
        throw error(HttpStatus.CONFLICT, "Um estande já está ocupado.");
      if (!merging && own)
        throw error(HttpStatus.CONFLICT, "Este estande já está reservado por você.");
    }
    if (merging && (!hasOwn || booths.size() < 2))
      throw error(
          HttpStatus.BAD_REQUEST, "Selecione sua reserva e ao menos um estande vizinho livre.");
    var fair =
        db.one(
            "SELECT reserve_hours FROM fair WHERE id=?",
            db.fairOf((String) booths.get(0).get("id")));
    Instant deadline =
        Instant.now().plusSeconds(((Number) fair.get("reserve_hours")).longValue() * 3600);
    // Extending a union must never reset an existing reservation's deadline.
    for (var b : booths)
      if (b.get("expires_at") != null && instant(b.get("expires_at")).isBefore(deadline))
        deadline = instant(b.get("expires_at"));
    String group = booths.size() > 1 ? id() : null;
    for (var b : booths)
      db.sql.update(
          "UPDATE booth SET"
              + " status='RESERVED',owner_id=?,label=?,group_id=?,expires_at=?,version=version+1"
              + " WHERE id=?",
          user.id(),
          user.company(),
          group,
          Timestamp.from(deadline),
          b.get("id"));
    db.audit(
        user.email(),
        merging ? "UNIR_RESERVA" : "RESERVAR",
        group == null ? request.ids().get(0) : group,
        Map.of("boothIds", request.ids(), "deadline", deadline.toString()));
  }

  @Transactional
  public Map<String, Object> contract(Selection request, Auth.User user, String ip) {
    var booths = select(request);
    sellable(booths);
    if (booths.size() > 1 && !connected(booths))
      throw error(HttpStatus.BAD_REQUEST, "A seleção deve ser de estandes vizinhos.");
    for (var b : booths)
      if (!b.get("status").equals("AVAILABLE")
          && !(b.get("status").equals("RESERVED") && user.id().equals(b.get("owner_id"))))
        throw error(HttpStatus.CONFLICT, "Um estande não está disponível para você.");
    var quote = quoteData(booths, request.assembly(), user);
    if (!Boolean.TRUE.equals(quote.get("ready")))
      throw error(HttpStatus.CONFLICT, "Preços e planta precisam estar publicados e conferidos.");
    if (!request.acceptTerms() || !Objects.equals(quote.get("hash"), request.quoteHash()))
      throw error(
          HttpStatus.CONFLICT,
          "Confira o orçamento atualizado e aceite as condições antes de contratar.");
    String contract = id(), group = booths.size() > 1 ? id() : null;
    db.sql.update(
        "INSERT INTO"
            + " contract_order(id,fair_id,user_id,booth_ids,snapshot,total,accepted_at,acceptance_ip)"
            + " VALUES(?,?,?,?,?,?,?,?)",
        contract,
        quote.get("fairId"),
        user.id(),
        db.encode(request.ids()),
        db.encode(quote),
        quote.get("total"),
        now(),
        ip);
    for (var b : booths)
      db.sql.update(
          "UPDATE booth SET"
              + " status='CONTRACTED',owner_id=?,label=?,expires_at=NULL,group_id=?,version=version+1"
              + " WHERE id=?",
          user.id(),
          user.company(),
          group,
          b.get("id"));
    db.notifyUser(
        user.id(),
        "Contratação registrada: " + contract + ". Consulte o comprovante em Minhas contratações.");
    db.audit(user.email(), "CONTRATAR", contract, quote);
    return Map.of("id", contract, "quote", quote);
  }

  @Transactional
  public void release(Selection request, Auth.User user) {
    var booths = select(request);
    for (var b : booths) {
      if (!b.get("status").equals("RESERVED")
          || (!user.role().equals("ADMIN") && !user.id().equals(b.get("owner_id"))))
        throw error(
            HttpStatus.FORBIDDEN,
            "Somente o titular ou um administrador pode liberar uma reserva. Contratações exigem"
                + " cancelamento administrativo.");
    }
    for (var b : booths) {
      offerOrFree((String) b.get("id"));
    }
    db.audit(user.email(), "LIBERAR", request.ids().get(0), Map.of("boothIds", request.ids()));
  }

  @Transactional
  public void queue(String boothId, long version, Auth.User user) {
    db.lockFair(db.fairOf(boothId));
    expireFair(db.fairOf(boothId));
    var b = db.one("SELECT * FROM booth WHERE id=? AND active=TRUE", boothId);
    sellable(List.of(b));
    if (((Number) b.get("version")).longValue() != version)
      throw error(HttpStatus.CONFLICT, "O estande mudou. Atualize a planta.");
    if (b.get("status").equals("AVAILABLE") || user.id().equals(b.get("owner_id")))
      throw error(HttpStatus.CONFLICT, "Reserve o estande livre ou consulte a sua reserva.");
    if (!List.of("BLOCKED", "RESERVED", "CONTRACTED").contains(b.get("status")))
      throw error(HttpStatus.CONFLICT, "Este estande não aceita fila.");
    if (!db.sql
        .queryForList(
            "SELECT id FROM queue_entry WHERE booth_id=? AND user_id=?", boothId, user.id())
        .isEmpty()) throw error(HttpStatus.CONFLICT, "Você já está na fila.");
    db.sql.update(
        "INSERT INTO queue_entry(id,booth_id,user_id,joined_at) VALUES(?,?,?,?)",
        id(),
        boothId,
        user.id(),
        now());
    db.audit(user.email(), "ENTRAR_FILA", boothId, Map.of());
  }

  @Transactional
  public void leaveQueue(String boothId, Auth.User user) {
    db.lockFair(db.fairOf(boothId));
    db.sql.update("DELETE FROM queue_entry WHERE booth_id=? AND user_id=?", boothId, user.id());
    db.audit(user.email(), "SAIR_FILA", boothId, Map.of());
  }

  public void offerOrFree(String boothId) {
    var booth =
        db.one(
            "SELECT b.code,b.verified,b.area,p.reviewed,p.fair_id,f.published FROM booth b JOIN pavilion p ON p.id=b.pavilion_id JOIN fair f ON f.id=p.fair_id WHERE"
                + " b.id=?",
            boothId);
    db.sql.update(
        "DELETE FROM queue_entry WHERE booth_id=? AND user_id IN (SELECT id FROM app_user WHERE"
            + " enabled=FALSE)",
        boothId);
    boolean canOffer = Boolean.TRUE.equals(booth.get("published"))
        && Boolean.TRUE.equals(booth.get("reviewed"))
        && Boolean.TRUE.equals(booth.get("verified")) && booth.get("area") != null;
    var q = canOffer ? db.sql.queryForList(
            "SELECT q.*,u.company FROM queue_entry q JOIN app_user u ON u.id=q.user_id WHERE"
                + " booth_id=? ORDER BY joined_at,id LIMIT 1",
            boothId) : Collections.<Map<String, Object>>emptyList();
    if (q.isEmpty())
      db.sql.update(
          "UPDATE booth SET"
              + " status='AVAILABLE',owner_id=NULL,label='',expires_at=NULL,group_id=NULL,version=version+1"
              + " WHERE id=?",
          boothId);
    else {
      var first = q.get(0);
      int hours =
          ((Number)
                  db.one("SELECT offer_hours FROM fair WHERE id=?", booth.get("fair_id"))
                      .get("offer_hours"))
              .intValue();
      var until = Timestamp.from(Instant.now().plusSeconds(hours * 3600L));
      db.sql.update(
          "UPDATE booth SET"
              + " status='RESERVED',owner_id=?,label=?,expires_at=?,group_id=NULL,version=version+1"
              + " WHERE id=?",
          first.get("user_id"),
          first.get("company"),
          until,
          boothId);
      db.sql.update("DELETE FROM queue_entry WHERE id=?", first.get("id"));
      db.notifyUser(
          (String) first.get("user_id"),
          "O estande "
              + booth.get("code")
              + " foi reservado exclusivamente para você até "
              + until.toInstant()
              + ". Confirme a contratação ou libere-o.");
    }
  }

  public void expireFair(String fairId) {
    var expired =
        db.sql.queryForList(
            "SELECT b.id,b.group_id,b.owner_id FROM booth b JOIN pavilion p ON p.id=b.pavilion_id"
                + " WHERE p.fair_id=? AND b.active=TRUE AND b.status='RESERVED' AND"
                + " b.expires_at<=?",
            fairId,
            now());
    Set<String> handled = new HashSet<>();
    for (var b : expired) {
      if (!handled.add((String) b.get("id"))) continue;
      List<Map<String, Object>> group =
          b.get("group_id") == null
              ? List.of(b)
              : db.sql.queryForList(
                  "SELECT id,owner_id FROM booth WHERE group_id=? AND active=TRUE",
                  b.get("group_id"));
      for (var member : group) {
        handled.add((String) member.get("id"));
        offerOrFree((String) member.get("id"));
      }
      if (b.get("owner_id") != null)
        db.notifyUser(
            (String) b.get("owner_id"),
            "O prazo da sua reserva terminou e o espaço foi liberado para a fila.");
      db.audit("SYSTEM", "EXPIRAR_RESERVA", (String) b.get("id"), Map.of());
    }
    // A closed fair keeps queue entries without starting a preference deadline.
    // On reopening, process these entries before a public reservation can claim the booth.
    for (var b : db.sql.queryForList(
        "SELECT b.id FROM booth b JOIN pavilion p ON p.id=b.pavilion_id JOIN fair f ON f.id=p.fair_id"
            + " WHERE p.fair_id=? AND f.published=TRUE AND p.reviewed=TRUE AND b.active=TRUE"
            + " AND b.verified=TRUE AND b.area IS NOT NULL AND b.status='AVAILABLE'"
            + " AND EXISTS (SELECT 1 FROM queue_entry q WHERE q.booth_id=b.id)", fairId))
      offerOrFree((String) b.get("id"));
  }

  @Transactional
  public void sweep() {
    for (var row : db.sql.queryForList("SELECT id FROM fair ORDER BY id")) {
      db.lockFair((String) row.get("id"));
      expireFair((String) row.get("id"));
    }
    db.sql.update("DELETE FROM auth_session WHERE expires_at<?", now());
    db.sql.update(
        "DELETE FROM auth_limit WHERE started_at<?",
        Timestamp.from(Instant.now().minusSeconds(86400)));
  }

  @Transactional
  public void cancelContract(String contract, String reason, Auth.User admin) {
    if (reason == null || reason.trim().length() < 8 || reason.length() > 500)
      throw error(HttpStatus.BAD_REQUEST, "Informe o motivo do cancelamento (8 a 500 caracteres).");
    var c = db.one("SELECT * FROM contract_order WHERE id=?", contract);
    db.lockFair((String) c.get("fair_id"));
    c = db.one("SELECT * FROM contract_order WHERE id=? FOR UPDATE", contract);
    if (!c.get("status").equals("ACTIVE"))
      throw error(HttpStatus.CONFLICT, "Contratação já cancelada.");
    try {
      var ids = db.json.readValue((String) c.get("booth_ids"), String[].class);
      for (String id : ids) offerOrFree(id);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
    db.sql.update(
        "UPDATE contract_order SET status='CANCELLED',cancelled_at=?,cancellation_reason=? WHERE"
            + " id=?",
        now(),
        reason.trim(),
        contract);
    db.notifyUser(
        (String) c.get("user_id"), "Contratação " + contract + " cancelada: " + reason.trim());
    db.audit(admin.email(), "CANCELAR_CONTRATO", contract, Map.of("reason", reason.trim()));
  }

  boolean connected(List<Map<String, Object>> booths) {
    Set<Integer> found = new HashSet<>();
    found.add(0);
    boolean changed;
    do {
      changed = false;
      for (int i = 0; i < booths.size(); i++)
        if (!found.contains(i))
          for (int j : new ArrayList<>(found))
            if (adjacent(
                db.points((String) booths.get(i).get("geometry")),
                db.points((String) booths.get(j).get("geometry")))) {
              found.add(i);
              changed = true;
              break;
            }
    } while (changed);
    return found.size() == booths.size();
  }

  static boolean adjacent(List<List<Double>> a, List<List<Double>> b) {
    for (int i = 0; i < a.size(); i++)
      for (int j = 0; j < b.size(); j++) {
        var p = a.get(i);
        var q = a.get((i + 1) % a.size());
        var r = b.get(j);
        var s = b.get((j + 1) % b.size());
        double dx = q.get(0) - p.get(0), dy = q.get(1) - p.get(1), len = Math.hypot(dx, dy);
        if (len < 2) continue;
        double ux = dx / len, uy = dy / len;
        double dist1 = Math.abs((r.get(0) - p.get(0)) * uy - (r.get(1) - p.get(1)) * ux),
            dist2 = Math.abs((s.get(0) - p.get(0)) * uy - (s.get(1) - p.get(1)) * ux);
        if (dist1 > 1.8 || dist2 > 1.8) continue;
        double t1 = (r.get(0) - p.get(0)) * ux + (r.get(1) - p.get(1)) * uy,
            t2 = (s.get(0) - p.get(0)) * ux + (s.get(1) - p.get(1)) * uy;
        if (Math.min(len, Math.max(t1, t2)) - Math.max(0, Math.min(t1, t2)) > 2) return true;
      }
    return false;
  }
}

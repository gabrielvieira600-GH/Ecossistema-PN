package br.com.feiras;

import static br.com.feiras.Db.*;

import java.awt.geom.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Maps {
  final Db db;
  final Commerce commerce;
  final Auth auth;

  public Maps(Db db, Commerce commerce, Auth auth) {
    this.db = db;
    this.commerce = commerce;
    this.auth = auth;
  }

  public record Edit(
      long version,
      String code,
      BigDecimal area,
      String dimensions,
      List<List<Double>> geometry,
      BigDecimal fontSize,
      String label,
      String status,
      boolean verified) {}

  public record Pricing(
      long version,
      boolean published,
      BigDecimal rate,
      BigDecimal assemblyRate,
      BigDecimal fixedFee,
      BigDecimal taxPercent,
      int reserveHours,
      int offerHours,
      String terms) {}

  public record UserEdit(boolean enabled, String role, String password) {}

  public record Annotation(
      Long version, String text, double x, double y, double fontSize, double rotation) {}

  public List<Map<String, Object>> fairs() {
    var fairs = db.sql.queryForList("SELECT * FROM fair ORDER BY id");
    for (var f : fairs)
      f.put(
          "pavilions",
          db.sql.queryForList("SELECT * FROM pavilion WHERE fair_id=? ORDER BY name", f.get("id")));
    return fairs;
  }

  @Transactional
  public Map<String, Object> pavilion(String id, Auth.User user) {
    var p = db.one("SELECT * FROM pavilion WHERE id=?", id);
    db.lockFair((String) p.get("fair_id"));
    commerce.expireFair((String) p.get("fair_id"));
    List<Map<String, Object>> booths = new ArrayList<>();
    for (var b :
        db.sql.queryForList(
            "SELECT * FROM booth WHERE pavilion_id=? AND active=TRUE ORDER BY code", id))
      booths.add(publicBooth(b, user));
    var out = new LinkedHashMap<String, Object>();
    out.put("pavilion", p);
    out.put("booths", booths);
    out.put(
        "annotations", db.sql.queryForList("SELECT * FROM map_annotation WHERE pavilion_id=?", id));
    return out;
  }

  Map<String, Object> publicBooth(Map<String, Object> b, Auth.User user) {
    var out = new LinkedHashMap<String, Object>();
    for (String k :
        List.of(
            "id",
            "pavilion_id",
            "code",
            "area",
            "dimensions",
            "font_size",
            "label",
            "status",
            "expires_at",
            "group_id",
            "verified",
            "version")) out.put(k, b.get(k));
    out.put("geometry", db.points((String) b.get("geometry")));
    out.put("mine", user.id().equals(b.get("owner_id")));
    int count =
        db.sql.queryForObject(
            "SELECT COUNT(*) FROM queue_entry WHERE booth_id=?", Integer.class, b.get("id"));
    out.put("queueCount", count);
    var entries =
        db.sql.queryForList(
            "SELECT user_id FROM queue_entry WHERE booth_id=? ORDER BY joined_at,id", b.get("id"));
    int position = 0;
    for (int i = 0; i < entries.size(); i++)
      if (entries.get(i).get("user_id").equals(user.id())) position = i + 1;
    out.put("queuePosition", position);
    if (user.role().equals("ADMIN")) {
      out.put("owner_id", b.get("owner_id"));
      out.put("source_label", b.get("source_label"));
    }
    return out;
  }

  @Transactional
  public List<Map<String, Object>> mine(Auth.User user) {
    for (var row : db.sql.queryForList("SELECT id FROM fair ORDER BY id")) {
      db.lockFair((String) row.get("id"));
      commerce.expireFair((String) row.get("id"));
    }
    var result =
        db.sql.queryForList(
            "SELECT b.*,p.name AS pavilion_name,p.fair_id FROM booth b JOIN pavilion p ON"
                + " p.id=b.pavilion_id WHERE b.owner_id=? AND b.active=TRUE ORDER BY p.name,b.code",
            user.id());
    List<Map<String, Object>> out = new ArrayList<>();
    for (var b : result) {
      var x = publicBooth(b, user);
      x.put("fair_id", b.get("fair_id"));
      x.put("pavilion_name", b.get("pavilion_name"));
      out.add(x);
    }
    return out;
  }

  public List<Map<String, Object>> queueList(Auth.User user) {
    return db.sql.queryForList(
        "SELECT q.booth_id,b.code,p.name,p.fair_id,q.joined_at,(SELECT COUNT(*) FROM queue_entry q2"
            + " WHERE q2.booth_id=q.booth_id AND (q2.joined_at<q.joined_at OR"
            + " (q2.joined_at=q.joined_at AND q2.id<=q.id))) AS position FROM queue_entry q JOIN"
            + " booth b ON b.id=q.booth_id JOIN pavilion p ON p.id=b.pavilion_id WHERE q.user_id=?"
            + " ORDER BY joined_at",
        user.id());
  }

  private void version(Map<String, Object> b, long version) {
    if (((Number) b.get("version")).longValue() != version)
      throw error(
          HttpStatus.CONFLICT,
          "Este registro foi alterado por outra pessoa. Recarregue antes de salvar.");
  }

  private void editValid(String pavilion, Edit e, String except) {
    if (e.code() == null || !e.code().trim().matches("[A-Za-z0-9][A-Za-z0-9/_. -]{0,39}"))
      throw error(HttpStatus.BAD_REQUEST, "Informe um código válido (até 40 caracteres).");
    if (e.label() == null
        || e.label().length() > 160
        || e.dimensions() == null
        || e.dimensions().length() > 160)
      throw error(HttpStatus.BAD_REQUEST, "Nome e dimensões devem ter até 160 caracteres.");
    if (e.area() != null
        && (e.area().signum() <= 0 || e.area().compareTo(new BigDecimal("10000")) > 0))
      throw error(HttpStatus.BAD_REQUEST, "Metragem inválida.");
    if (e.verified() && e.area() == null)
      throw error(HttpStatus.BAD_REQUEST, "Informe a metragem antes de conferir o estande.");
    if (e.fontSize() == null || e.fontSize().doubleValue() < 2 || e.fontSize().doubleValue() > 30)
      throw error(HttpStatus.BAD_REQUEST, "Fonte permitida: 2 a 30 unidades da planta.");
    if (e.geometry() == null || e.geometry().size() < 3 || e.geometry().size() > 16)
      throw error(HttpStatus.BAD_REQUEST, "A geometria deve ter de 3 a 16 vértices.");
    var p = db.one("SELECT * FROM pavilion WHERE id=?", pavilion);
    double width = ((Number) p.get("map_width")).doubleValue(),
        height = ((Number) p.get("map_height")).doubleValue();
    for (var point : e.geometry())
      if (point == null
          || point.size() != 2
          || point.get(0) == null
          || point.get(1) == null
          || !Double.isFinite(point.get(0))
          || !Double.isFinite(point.get(1))
          || point.get(0) < 0
          || point.get(0) > width
          || point.get(1) < 0
          || point.get(1) > height)
        throw error(HttpStatus.BAD_REQUEST, "Os vértices devem permanecer dentro da planta.");
    for (int i = 0; i < e.geometry().size(); i++)
      for (int j = i + 1; j < e.geometry().size(); j++) {
        int n = e.geometry().size();
        if (j == i + 1 || (i == 0 && j == n - 1)) continue;
        var a = e.geometry().get(i);
        var b = e.geometry().get((i + 1) % n);
        var c = e.geometry().get(j);
        var d = e.geometry().get((j + 1) % n);
        if (Line2D.linesIntersect(
            a.get(0), a.get(1), b.get(0), b.get(1), c.get(0), c.get(1), d.get(0), d.get(1)))
          throw error(HttpStatus.BAD_REQUEST, "O polígono não pode cruzar a si mesmo.");
      }
    Area area = shape(e.geometry());
    if (area.isEmpty() || area.getBounds2D().getWidth() < 2 || area.getBounds2D().getHeight() < 2)
      throw error(HttpStatus.BAD_REQUEST, "Geometria sem área útil.");
    for (var other :
        db.sql.queryForList(
            "SELECT id,code,geometry FROM booth WHERE pavilion_id=? AND active=TRUE", pavilion)) {
      if (other.get("id").equals(except)) continue;
      Area overlap = new Area(area);
      overlap.intersect(shape(db.points((String) other.get("geometry"))));
      if (!overlap.isEmpty()
          && overlap.getBounds2D().getWidth() > .3
          && overlap.getBounds2D().getHeight() > .3)
        throw error(
            HttpStatus.CONFLICT,
            "Sobreposição com o estande " + other.get("code") + ". Corrija os vértices.");
    }
  }

  static Area shape(List<List<Double>> points) {
    Path2D p = new Path2D.Double();
    p.moveTo(points.get(0).get(0), points.get(0).get(1));
    for (int i = 1; i < points.size(); i++) p.lineTo(points.get(i).get(0), points.get(i).get(1));
    p.closePath();
    return new Area(p);
  }

  @Transactional
  public String create(String pavilion, Edit edit, Auth.User admin) {
    var p = db.one("SELECT * FROM pavilion WHERE id=?", pavilion);
    db.lockFair((String) p.get("fair_id"));
    editValid(pavilion, edit, null);
    String id = id();
    db.sql.update(
        "INSERT INTO"
            + " booth(id,pavilion_id,code,area,dimensions,geometry,font_size,label,status,verified)"
            + " VALUES(?,?,?,?,?,?,?,?,?,?)",
        id,
        pavilion,
        edit.code().trim().toUpperCase(Locale.ROOT),
        edit.area(),
        edit.dimensions().trim(),
        db.encode(edit.geometry()),
        edit.fontSize(),
        edit.label().trim(),
        "AVAILABLE",
        edit.verified());
    db.audit(admin.email(), "CRIAR_ESTANDE", id, edit);
    return id;
  }

  @Transactional
  public void edit(String id, Edit edit, Auth.User admin) {
    db.lockFair(db.fairOf(id));
    var b = db.one("SELECT * FROM booth WHERE id=? AND active=TRUE", id);
    version(b, edit.version());
    editValid((String) b.get("pavilion_id"), edit, id);
    String status = (String) b.get("status");
    boolean structureChanged = !edit.code().trim().toUpperCase(Locale.ROOT).equals(b.get("code"))
        || (edit.area() == null ? b.get("area") != null : b.get("area") == null || edit.area().compareTo((BigDecimal) b.get("area")) != 0)
        || !db.points((String) b.get("geometry")).equals(edit.geometry())
        || !Objects.equals(edit.verified(), b.get("verified"));
    if (structureChanged && List.of("RESERVED", "CONTRACTED").contains(status))
      throw error(HttpStatus.CONFLICT, "Libere a reserva ou cancele a contratação antes de alterar código, área, contorno ou conferência. Nome e fonte podem ser editados.");
    if (!Objects.equals(edit.status(), status)) {
      if (!(List.of("AVAILABLE", "BLOCKED").contains(status)
          && List.of("AVAILABLE", "BLOCKED").contains(edit.status())))
        throw error(
            HttpStatus.CONFLICT,
            "Use os comandos de reserva, liberação ou cancelamento para alterar ocupação.");
      if (edit.status().equals("AVAILABLE")) {
        commerce.offerOrFree(id);
        b = db.one("SELECT * FROM booth WHERE id=?", id);
        status = (String) b.get("status");
      } else status = edit.status();
    }
    db.sql.update(
        "UPDATE booth SET"
            + " code=?,area=?,dimensions=?,geometry=?,font_size=?,label=?,status=?,verified=?,version=version+1"
            + " WHERE id=?",
        edit.code().trim().toUpperCase(Locale.ROOT),
        edit.area(),
        edit.dimensions().trim(),
        db.encode(edit.geometry()),
        edit.fontSize(),
        status.equals("RESERVED") && !Objects.equals(edit.status(), status)
            ? b.get("label")
            : edit.label().trim(),
        status,
        edit.verified(),
        id);
    db.audit(admin.email(), "EDITAR_ESTANDE", id, Map.of("before", b, "after", edit));
  }

  @Transactional
  public void delete(String id, long version, Auth.User admin) {
    db.lockFair(db.fairOf(id));
    var b = db.one("SELECT * FROM booth WHERE id=? AND active=TRUE", id);
    version(b, version);
    if (!b.get("status").equals("AVAILABLE")
        || db.sql.queryForObject(
                "SELECT COUNT(*) FROM queue_entry WHERE booth_id=?", Integer.class, id)
            > 0)
      throw error(HttpStatus.CONFLICT, "Libere o estande e resolva a fila antes de excluir.");
    db.sql.update(
        "UPDATE booth SET active=FALSE,status='ARCHIVED',version=version+1 WHERE id=?", id);
    db.audit(admin.email(), "EXCLUIR_ESTANDE", id, b);
  }

  @Transactional
  public void assign(String id, long version, String userId, Auth.User admin) {
    db.lockFair(db.fairOf(id));
    var b = db.one("SELECT * FROM booth WHERE id=? AND active=TRUE", id);
    version(b, version);
    if (!List.of("AVAILABLE", "BLOCKED").contains(b.get("status")))
      throw error(HttpStatus.CONFLICT, "Estande já tem titular.");
    if (db.sql.queryForObject("SELECT COUNT(*) FROM queue_entry WHERE booth_id=?", Integer.class, id) > 0)
      throw error(HttpStatus.CONFLICT, "Há uma fila de preferência. Libere a ocupação para atender a primeira empresa da fila.");
    var user = db.one("SELECT * FROM app_user WHERE id=? AND enabled=TRUE", userId);
    var fair = db.one("SELECT reserve_hours FROM fair WHERE id=?", db.fairOf(id));
    Timestamp until =
        Timestamp.from(
            Instant.now().plusSeconds(((Number) fair.get("reserve_hours")).longValue() * 3600));
    db.sql.update(
        "UPDATE booth SET status='RESERVED',owner_id=?,label=?,expires_at=?,version=version+1 WHERE"
            + " id=?",
        userId,
        user.get("company"),
        until,
        id);
    db.notifyUser(
        userId, "A organização atribuiu a reserva do estande " + b.get("code") + " à sua empresa.");
    db.audit(admin.email(), "ATRIBUIR_RESERVA", id, Map.of("userId", userId));
  }

  @Transactional
  public void pricing(String id, Pricing p, Auth.User admin) {
    db.lockFair(id);
    var fair = db.one("SELECT * FROM fair WHERE id=?", id);
    version(fair, p.version());
    for (var v : Arrays.asList(p.rate(), p.assemblyRate(), p.fixedFee(), p.taxPercent()))
      if (v == null || v.signum() < 0 || v.compareTo(new BigDecimal("1000000")) > 0)
        throw error(HttpStatus.BAD_REQUEST, "Informe valores não negativos e até R$ 1.000.000.");
    if (p.taxPercent().compareTo(new BigDecimal("100")) > 0
        || p.reserveHours() < 1
        || p.reserveHours() > 720
        || p.offerHours() < 1
        || p.offerHours() > 168
        || p.terms() == null
        || p.terms().length() > 20000)
      throw error(HttpStatus.BAD_REQUEST, "Confira impostos, condições e prazos.");
    if (p.published() && (p.rate().signum() <= 0 || p.terms().trim().length() < 30))
      throw error(
          HttpStatus.BAD_REQUEST,
          "Para publicar, informe preço por m² e condições comerciais completas (mínimo 30"
              + " caracteres).");
    db.sql.update(
        "UPDATE fair SET"
            + " published=?,rate=?,assembly_rate=?,fixed_fee=?,tax_percent=?,reserve_hours=?,offer_hours=?,terms=?,version=version+1"
            + " WHERE id=?",
        p.published(),
        p.rate(),
        p.assemblyRate(),
        p.fixedFee(),
        p.taxPercent(),
        p.reserveHours(),
        p.offerHours(),
        p.terms().trim(),
        id);
    db.audit(admin.email(), "PREÇOS", id, p);
  }

  @Transactional
  public void reviewed(String id, boolean reviewed, Auth.User admin) {
    var p = db.one("SELECT * FROM pavilion WHERE id=?", id);
    db.lockFair((String) p.get("fair_id"));
    if (reviewed
        && db.sql.queryForObject(
                "SELECT COUNT(*) FROM booth WHERE pavilion_id=? AND active=TRUE AND (verified=FALSE"
                    + " OR area IS NULL)",
                Integer.class,
                id)
            > 0)
      throw error(HttpStatus.CONFLICT, "Confira os estandes pendentes antes de publicar a planta.");
    db.sql.update("UPDATE pavilion SET reviewed=? WHERE id=?", reviewed, id);
    db.audit(admin.email(), "CONFERIR_PLANTA", id, Map.of("reviewed", reviewed));
  }

  @Transactional
  public void userEdit(String id, UserEdit edit, Auth.User admin) {
    db.lockFair("navalshore-2027");
    var before = db.one("SELECT * FROM app_user WHERE id=? FOR UPDATE", id);
    if (!List.of("ADMIN", "EXHIBITOR").contains(edit.role()))
      throw error(HttpStatus.BAD_REQUEST, "Perfil inválido.");
    if (before.get("role").equals("ADMIN")
        && (Boolean) before.get("enabled")
        && (!edit.enabled() || !edit.role().equals("ADMIN"))
        && db.sql.queryForObject(
                "SELECT COUNT(*) FROM app_user WHERE role='ADMIN' AND enabled=TRUE", Integer.class)
            <= 1) throw error(HttpStatus.CONFLICT, "Mantenha pelo menos um administrador ativo.");
    db.sql.update(
        "UPDATE app_user SET enabled=?,role=? WHERE id=?", edit.enabled(), edit.role(), id);
    if (edit.password() != null && !edit.password().isBlank())
      db.sql.update(
          "UPDATE app_user SET password_hash=? WHERE id=?", auth.passwordHash(edit.password()), id);
    db.sql.update("DELETE FROM auth_session WHERE user_id=?", id);
    db.audit(
        admin.email(),
        "EDITAR_USUÁRIO",
        id,
        Map.of(
            "enabled",
            edit.enabled(),
            "role",
            edit.role(),
            "passwordReset",
            edit.password() != null && !edit.password().isBlank()));
  }

  @Transactional
  public String annotation(String pavilion, String id, Annotation edit, Auth.User admin) {
    var p = db.one("SELECT * FROM pavilion WHERE id=?", pavilion);
    db.lockFair((String) p.get("fair_id"));
    if (edit.text() == null
        || edit.text().length() > 160
        || edit.text().isBlank()
        || !Double.isFinite(edit.x())
        || !Double.isFinite(edit.y())
        || edit.x() < 0
        || edit.y() < 0
        || edit.x() > ((Number) p.get("map_width")).doubleValue()
        || edit.y() > ((Number) p.get("map_height")).doubleValue()
        || !Double.isFinite(edit.fontSize())
        || edit.fontSize() < 2
        || edit.fontSize() > 40
        || !Double.isFinite(edit.rotation())
        || Math.abs(edit.rotation()) > 360)
      throw error(HttpStatus.BAD_REQUEST, "Texto ou posição inválidos.");
    if (id == null) {
      id = id();
      db.sql.update(
          "INSERT INTO map_annotation(id,pavilion_id,text,x,y,font_size,rotation)"
              + " VALUES(?,?,?,?,?,?,?)",
          id,
          pavilion,
          edit.text(),
          edit.x(),
          edit.y(),
          edit.fontSize(),
          edit.rotation());
    } else {
      var a = db.one("SELECT * FROM map_annotation WHERE id=? AND pavilion_id=?", id, pavilion);
      version(a, edit.version() == null ? -1 : edit.version());
      db.sql.update(
          "UPDATE map_annotation SET text=?,x=?,y=?,font_size=?,rotation=?,version=version+1 WHERE"
              + " id=?",
          edit.text(),
          edit.x(),
          edit.y(),
          edit.fontSize(),
          edit.rotation(),
          id);
    }
    db.audit(admin.email(), "TEXTO_PLANTA", id, edit);
    return id;
  }

  @Transactional
  public void deleteAnnotation(String id, long version, Auth.User admin) {
    var a = db.one("SELECT * FROM map_annotation WHERE id=?", id);
    var p = db.one("SELECT fair_id FROM pavilion WHERE id=?", a.get("pavilion_id"));
    db.lockFair((String) p.get("fair_id"));
    a = db.one("SELECT * FROM map_annotation WHERE id=?", id);
    version(a, version);
    db.sql.update("DELETE FROM map_annotation WHERE id=?", id);
    db.audit(admin.email(), "EXCLUIR_TEXTO", id, a);
  }
}

package br.com.feiras;

import static br.com.feiras.Db.*;

import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class Api {
  final Auth auth;
  final Maps maps;
  final Commerce commerce;
  final Db db;

  public Api(Auth auth, Maps maps, Commerce commerce, Db db) {
    this.auth = auth;
    this.maps = maps;
    this.commerce = commerce;
    this.db = db;
  }

  @GetMapping("/health")
  public Map<String, Object> health() {
    db.sql.queryForObject("SELECT 1", Integer.class);
    return Map.of("status", "UP");
  }

  @GetMapping("/auth/csrf")
  public Map<String, String> csrf(CsrfToken token) {
    return Map.of("token", token.getToken(), "header", token.getHeaderName());
  }

  @PostMapping("/auth/register")
  public Map<String, String> register(@Valid @RequestBody Auth.Register r, HttpServletRequest req) {
    auth.register(r, req.getRemoteAddr());
    return Map.of("message", "Cadastro recebido. Aguarde aprovação da organização.");
  }

  @PostMapping("/auth/login")
  public Auth.User login(
      @Valid @RequestBody Auth.Login r, HttpServletRequest req, HttpServletResponse res) {
    return auth.login(r, req.getRemoteAddr(), res);
  }

  @GetMapping("/auth/me")
  public Auth.User me() {
    return Auth.current();
  }

  @PostMapping("/auth/logout")
  public void logout(HttpServletRequest req, HttpServletResponse res) {
    auth.logout(req, res);
  }

  @GetMapping("/fairs")
  public Object fairs() {
    return maps.fairs();
  }

  @GetMapping("/pavilions/{id}")
  public Object pavilion(@PathVariable String id) {
    return maps.pavilion(id, Auth.current());
  }

  @GetMapping("/me/booths")
  public Object mine() {
    return maps.mine(Auth.current());
  }

  @GetMapping("/me/queue")
  public Object queues() {
    return maps.queueList(Auth.current());
  }

  @PostMapping("/quote")
  public Object quote(@RequestBody Commerce.Selection s) {
    return commerce.quote(s, Auth.current());
  }

  @PostMapping("/reserve")
  public void reserve(@RequestBody Commerce.Selection s) {
    commerce.reserve(s, Auth.current(), false);
  }

  @PostMapping("/merge")
  public void merge(@RequestBody Commerce.Selection s) {
    commerce.reserve(s, Auth.current(), true);
  }

  @PostMapping("/contract")
  public Object contract(@RequestBody Commerce.Selection s, HttpServletRequest req) {
    return commerce.contract(s, Auth.current(), req.getRemoteAddr());
  }

  public record Version(long version) {}

  @PostMapping("/booths/{id}/queue")
  public void queue(@PathVariable String id, @RequestBody Version v) {
    commerce.queue(id, v.version(), Auth.current());
  }

  @DeleteMapping("/booths/{id}/queue")
  public void leave(@PathVariable String id) {
    commerce.leaveQueue(id, Auth.current());
  }

  @PostMapping("/release")
  public void release(@RequestBody Commerce.Selection s) {
    commerce.release(s, Auth.current());
  }

  @GetMapping("/orders")
  public Object orders() {
    var u = Auth.current();
    return u.role().equals("ADMIN")
        ? db.sql.queryForList(
            "SELECT c.id,c.fair_id,c.user_id,c.total,c.status,c.accepted_at,u.company FROM"
                + " contract_order c JOIN app_user u ON u.id=c.user_id ORDER BY accepted_at DESC"
                + " LIMIT 500")
        : db.sql.queryForList(
            "SELECT id,fair_id,total,status,accepted_at FROM contract_order WHERE user_id=? ORDER"
                + " BY accepted_at DESC",
            u.id());
  }

  @GetMapping("/orders/{id}")
  public Object order(@PathVariable String id) {
    var c =
        db.one(
            "SELECT c.*,u.company,u.contact,u.email FROM contract_order c JOIN app_user u ON"
                + " u.id=c.user_id WHERE c.id=?",
            id);
    var u = Auth.current();
    if (!u.role().equals("ADMIN") && !u.id().equals(c.get("user_id")))
      throw error(HttpStatus.FORBIDDEN, "Comprovante de outra empresa.");
    try {
      c.put("snapshot", db.json.readTree((String) c.get("snapshot")));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    return c;
  }

  @GetMapping("/notifications")
  public Object notifications() {
    return db.sql.queryForList(
        "SELECT id,message,created_at,seen FROM notification WHERE user_id=? ORDER BY created_at"
            + " DESC LIMIT 100",
        Auth.current().id());
  }

  @PostMapping("/notifications/read")
  public void readNotifications() {
    db.sql.update("UPDATE notification SET seen=TRUE WHERE user_id=?", Auth.current().id());
  }

  @GetMapping("/admin/users")
  public Object users() {
    Auth.admin();
    return db.sql.queryForList(
        "SELECT id,email,company,contact,phone,role,enabled,created_at FROM app_user ORDER BY"
            + " created_at DESC");
  }

  @PutMapping("/admin/users/{id}")
  public void userEdit(@PathVariable String id, @RequestBody Maps.UserEdit e) {
    maps.userEdit(id, e, Auth.admin());
  }

  @PutMapping("/admin/fairs/{id}")
  public void pricing(@PathVariable String id, @RequestBody Maps.Pricing p) {
    maps.pricing(id, p, Auth.admin());
  }

  public record Reviewed(boolean reviewed) {}

  @PutMapping("/admin/pavilions/{id}/review")
  public void reviewed(@PathVariable String id, @RequestBody Reviewed r) {
    maps.reviewed(id, r.reviewed(), Auth.admin());
  }

  @PostMapping("/admin/pavilions/{id}/booths")
  public Object create(@PathVariable String id, @RequestBody Maps.Edit e) {
    return Map.of("id", maps.create(id, e, Auth.admin()));
  }

  @PutMapping("/admin/booths/{id}")
  public void edit(@PathVariable String id, @RequestBody Maps.Edit e) {
    maps.edit(id, e, Auth.admin());
  }

  @DeleteMapping("/admin/booths/{id}")
  public void delete(@PathVariable String id, @RequestParam long version) {
    maps.delete(id, version, Auth.admin());
  }

  public record Assign(long version, String userId) {}

  @PostMapping("/admin/booths/{id}/assign")
  public void assign(@PathVariable String id, @RequestBody Assign a) {
    maps.assign(id, a.version(), a.userId(), Auth.admin());
  }

  public record Reason(String reason) {}

  @PostMapping("/admin/orders/{id}/cancel")
  public void cancel(@PathVariable String id, @RequestBody Reason r) {
    commerce.cancelContract(id, r.reason(), Auth.admin());
  }

  @GetMapping("/admin/audit")
  public Object audit() {
    Auth.admin();
    return db.sql.queryForList("SELECT * FROM audit_log ORDER BY created_at DESC LIMIT 500");
  }

  @PostMapping("/admin/pavilions/{id}/annotations")
  public Object annotation(@PathVariable String id, @RequestBody Maps.Annotation a) {
    return Map.of("id", maps.annotation(id, null, a, Auth.admin()));
  }

  @PutMapping("/admin/pavilions/{id}/annotations/{annotation}")
  public Object annotationEdit(
      @PathVariable String id, @PathVariable String annotation, @RequestBody Maps.Annotation a) {
    return Map.of("id", maps.annotation(id, annotation, a, Auth.admin()));
  }

  @DeleteMapping("/admin/annotations/{id}")
  public void annotationDelete(@PathVariable String id, @RequestParam long version) {
    maps.deleteAnnotation(id, version, Auth.admin());
  }

  @GetMapping(value = "/admin/export", produces = "application/json")
  public ResponseEntity<Object> export() {
    Auth.admin();
    var data = new LinkedHashMap<String, Object>();
    for (String table :
        List.of(
            "fair",
            "pavilion",
            "booth",
            "queue_entry",
            "contract_order",
            "map_annotation",
            "audit_log")) data.put(table, db.sql.queryForList("SELECT * FROM " + table));
    db.audit(Auth.current().email(), "EXPORTAR", "dataset", Map.of());
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=feiras-export.json")
        .body(data);
  }
}

@RestControllerAdvice
class Errors {
  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<Object> status(ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode())
        .body(Map.of("message", e.getReason() == null ? "Operação recusada." : e.getReason()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<Object> validation(MethodArgumentNotValidException e) {
    return ResponseEntity.badRequest()
        .body(Map.of("message", "Confira os campos obrigatórios e seus limites."));
  }

  @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
  ResponseEntity<Object> badBody(Exception e) {
    return ResponseEntity.badRequest()
        .body(Map.of("message", "Dados inválidos. Confira os campos e tente novamente."));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<Object> duplicate(Exception e) {
    return ResponseEntity.status(409)
        .body(Map.of("message", "Código ou e-mail já cadastrado, ou vínculo de dados inválido."));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<Object> unexpected(Exception e) {
    org.slf4j.LoggerFactory.getLogger(Errors.class).error("Unexpected application error", e);
    return ResponseEntity.status(500)
        .body(
            Map.of(
                "message",
                "Não foi possível concluir. Consulte a organização com o horário desta operação."));
  }
}

package br.com.feiras;

import static br.com.feiras.Db.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

/** Confirma a formalização comercial de uma reserva individual. */
@RestController
@RequestMapping("/api/admin/booths")
public class BoothContractConfirmation {
  private final Db db;
  private final Auth auth;

  public BoothContractConfirmation(Db db, Auth auth) {
    this.db = db;
    this.auth = auth;
  }

  public record Confirmation(Boolean signed, Boolean paid) {}

  @Transactional
  @PostMapping("/{id}/confirm-contract")
  public Map<String, Object> confirm(@PathVariable String id,
      @RequestBody Confirmation confirmation, HttpServletRequest req) {
    auth.requireBooth(req, id);
    Auth.User admin = Auth.admin();
    if (confirmation == null || !Boolean.TRUE.equals(confirmation.signed())
        || !Boolean.TRUE.equals(confirmation.paid()))
      throw error(HttpStatus.BAD_REQUEST,
          "Confirme a assinatura do contrato e o pagamento antes de concluir.");

    db.lockFair(db.fairOf(id));
    var booth = db.one("SELECT id,code,status,owner_id,label FROM booth WHERE id=? AND active=TRUE", id);
    if (!Objects.equals(booth.get("status"), "RESERVED"))
      throw error(HttpStatus.CONFLICT, "Somente estandes reservados podem ter a contratação confirmada.");
    if (booth.get("owner_id") == null)
      throw error(HttpStatus.CONFLICT, "A reserva não possui empresa responsável.");

    int changed = db.sql.update(
        "UPDATE booth SET status='CONTRACTED', expires_at=NULL, version=version+1 "
            + "WHERE id=? AND status='RESERVED' AND owner_id=?",
        id, booth.get("owner_id"));
    if (changed != 1)
      throw error(HttpStatus.CONFLICT, "A reserva foi alterada. Atualize a planta.");

    db.audit(admin.email(), "CONFIRMAR_CONTRATACAO", id,
        Map.of("boothCode", booth.get("code"),
               "company", Objects.toString(booth.get("label"), ""),
               "signed", true, "paid", true));
    db.notifyUser((String) booth.get("owner_id"),
        "A contratação do estande " + booth.get("code") + " foi confirmada pela organização.");
    return Map.of("id", id, "status", "CONTRACTED");
  }
}

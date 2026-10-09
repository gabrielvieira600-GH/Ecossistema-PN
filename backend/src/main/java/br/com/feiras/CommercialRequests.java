package br.com.feiras;

import static br.com.feiras.Db.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommercialRequests {
  private final Db db;
  private final Commerce commerce;
  @Value("${COMMERCIAL_EMAIL_TO:gabriel@portosenavios.com.br}") String destination;
  @Value("${RESEND_API_KEY:}") String emailKey;
  @Value("${COMMERCIAL_EMAIL_FROM:}") String sender;

  public CommercialRequests(Db db, Commerce commerce) { this.db=db; this.commerce=commerce; }

  public record Payment(String type, int dueDay, Integer installments, boolean twentyPercentNow,
      Boolean installmentsFromJanuary, String customTerms) {}
  public record ContractData(String mode, String changes, Map<String,String> newExhibitor) {}
  public record Payload(Commerce.Selection selection, String requestType, Payment payment,
      ContractData contractData) {}

  @Transactional
  public Map<String,Object> submit(Payload payload, Auth.User user, String fairId) {
    if (payload == null || payload.selection() == null || payload.selection().ids() == null || payload.selection().ids().isEmpty())
      throw error(HttpStatus.BAD_REQUEST,"Selecione um estande.");
    var req=payload.selection();
    if (!List.of("CONTRACT","WAITLIST").contains(payload.requestType()))
      throw error(HttpStatus.BAD_REQUEST,"Tipo de solicitação inválido.");
    var q=commerce.quote(req,user);
    if (!fairId.equals(q.get("fairId"))) throw error(HttpStatus.FORBIDDEN,"Empresa incorreta.");
    if (!Objects.equals(req.quoteHash(),q.get("hash")) || !req.acceptTerms())
      throw error(HttpStatus.CONFLICT,"O orçamento mudou. Revise e aceite novamente.");
    var booths=db.sql.queryForList("SELECT id,status,owner_id FROM booth WHERE id IN ("+
        String.join(",",Collections.nCopies(req.ids().size(),"?"))+")", req.ids().toArray());
    if (booths.size()!=req.ids().size()) throw error(HttpStatus.CONFLICT,"Estandes não encontrados.");
    boolean available=booths.stream().allMatch(b -> "AVAILABLE".equals(b.get("status")) ||
        ("RESERVED".equals(b.get("status")) && user.id().equals(b.get("owner_id"))));
    if (payload.requestType().equals("CONTRACT") && !available)
      throw error(HttpStatus.CONFLICT,"O estande está ocupado. Solicite entrada na fila.");
    if (payload.requestType().equals("WAITLIST") && (available || booths.size()!=1))
      throw error(HttpStatus.BAD_REQUEST,"Fila disponível apenas para um estande ocupado.");
    if (payload.requestType().equals("CONTRACT")) validatePayment(payload.payment(),fairId);
    if (payload.contractData()==null) throw error(HttpStatus.BAD_REQUEST,"Confirme os dados contratuais.");
    var cd=payload.contractData();
    if (!List.of("SAME","CHANGE","NEW").contains(cd.mode())) throw error(HttpStatus.BAD_REQUEST,"Opção de cadastro inválida.");
    if (cd.mode().equals("CHANGE") && (cd.changes()==null || cd.changes().isBlank()))
      throw error(HttpStatus.BAD_REQUEST,"Descreva as alterações contratuais.");
    if (cd.mode().equals("NEW") && (cd.newExhibitor()==null ||
        StreamRequired.missing(cd.newExhibitor())))
      throw error(HttpStatus.BAD_REQUEST,"Preencha os dados obrigatórios do novo expositor.");
    if (payload.requestType().equals("WAITLIST")) {
      String boothId=req.ids().get(0);
      db.lockFair(fairId);
      if (db.sql.queryForObject("SELECT COUNT(*) FROM queue_entry WHERE booth_id=? AND user_id=?",
          Integer.class,boothId,user.id()) == 0) {
        db.sql.update("INSERT INTO queue_entry(id,booth_id,user_id,joined_at) VALUES(?,?,?,?)",
          id(),boothId,user.id(),now());
      }
    }

    var detail=new LinkedHashMap<String,Object>();
    detail.put("requestType",payload.requestType()); detail.put("fair",q.get("fair"));
    detail.put("user",Map.of("company",user.company(),"email",user.email(),"contact",user.contact()));
    detail.put("quote",q); detail.put("payment",payload.payment()); detail.put("contractData",cd);
    String id=id();
    db.sql.update("INSERT INTO commercial_request(id,fair_id,user_id,booth_ids,request_type,details,status,created_at) VALUES(?,?,?,?,?,?,?,?)",
      id,fairId,user.id(),db.encode(req.ids()),payload.requestType(),db.encode(detail),"PENDING_EMAIL",now());
    // Do not mark a booth CONTRACTED. Commercial team must review and complete contracts manually.
    db.notifyUser(user.id(),"Pedido recebido pelo portal: "+id+". Aguarda processamento comercial.");
    db.audit(user.email(),"PEDIDO_COMERCIAL",id,Map.of("fair",fairId,"type",payload.requestType()));
    boolean mailed=sendEmail(id,detail);
    if(mailed) db.sql.update("UPDATE commercial_request SET status='EMAILED', emailed_at=? WHERE id=?",now(),id);
    return Map.of("id",id,"emailSent",mailed,"message",mailed?
      "Seu pedido foi enviado para o comercial da "+q.get("fair")+".":
      "Seu pedido foi registrado, mas o e-mail ainda não foi enviado. A equipe deve verificar a configuração de e-mail.");
  }
  static class StreamRequired {
    static boolean missing(Map<String,String> a) {
      for(String k:List.of("company","taxId","contact","email","phone","address","city","state","postalCode","signatory","financialContact","boothContact"))
        if(a.get(k)==null || a.get(k).isBlank()) return true;
      return false;
    }
  }
  void validatePayment(Payment p,String fairId) {
    if (p==null || !List.of("CASH","INSTALLMENTS","NEGOTIATE").contains(p.type()))
      throw error(HttpStatus.BAD_REQUEST,"Selecione uma condição de pagamento.");
    if(p.type().equals("NEGOTIATE")) {
      if(p.customTerms()==null || p.customTerms().isBlank()) throw error(HttpStatus.BAD_REQUEST,"Descreva as condições propostas.");
      return;
    }
    if(!List.of(5,10,15,20,25).contains(p.dueDay())) throw error(HttpStatus.BAD_REQUEST,"Data de vencimento inválida.");
    if(p.type().equals("INSTALLMENTS") && (p.installments()==null || p.installments()<2 || p.installments()>36))
      throw error(HttpStatus.BAD_REQUEST,"Quantidade de parcelas inválida.");
    // The commercial team confirms calendar feasibility: due dates cannot exceed the relevant deadline.
    YearMonth deadline=fairId.startsWith("naval")? YearMonth.of(2027,7):YearMonth.of(2027,3);
    YearMonth first=p.twentyPercentNow() ?
      YearMonth.of(2027,1):YearMonth.from(LocalDate.now().plusMonths(1));
    int parts=p.type().equals("CASH")?1:p.installments();
    if(first.plusMonths(parts-1).isAfter(deadline))
      throw error(HttpStatus.BAD_REQUEST,"O último vencimento excede o limite de "+deadline+".");
  }
  boolean sendEmail(String id,Map<String,Object> detail) {
    if(emailKey.isBlank() || sender.isBlank()) return false;
    try {
      @SuppressWarnings("unchecked")
      Map<String,Object> q=(Map<String,Object>)detail.get("quote");
      StringBuilder html=new StringBuilder("<div style='font-family:Arial,sans-serif;color:#173348;max-width:760px'>")
        .append("<h1 style='color:#078e83'>Novo pedido comercial</h1>")
        .append("<p><b>Feira:</b> ").append(escape(String.valueOf(detail.get("fair")))).append("</p>")
        .append("<p><b>Protocolo:</b> ").append(escape(id)).append("</p>")
        .append("<p><b>Tipo:</b> ").append(escape(String.valueOf(detail.get("requestType")))).append("</p>");
      @SuppressWarnings("unchecked")
      Map<String,Object> u=(Map<String,Object>)detail.get("user");
      html.append("<h2>Empresa solicitante</h2><p>").append(escape(String.valueOf(u.get("company"))))
        .append(" — ").append(escape(String.valueOf(u.get("contact"))))
        .append(" — ").append(escape(String.valueOf(u.get("email")))).append("</p>");
      html.append("<h2>Orçamento</h2><table style='border-collapse:collapse;width:100%'>");
      @SuppressWarnings("unchecked")
      List<Map<String,Object>> lines=(List<Map<String,Object>>)q.get("lines");
      if(lines!=null) for(var line:lines) html.append("<tr><td style='border-bottom:1px solid #ddd;padding:8px'>")
        .append(escape(String.valueOf(line.get("label")))).append("</td><td style='border-bottom:1px solid #ddd;padding:8px;text-align:right'>R$ ")
        .append(escape(String.valueOf(line.get("total")))).append("</td></tr>");
      html.append("</table><p><b>Total: R$ ").append(escape(String.valueOf(q.get("total")))).append("</b></p>");
      html.append("<h2>Condições de pagamento</h2><pre style='white-space:pre-wrap'>")
        .append(escape(db.json.writerWithDefaultPrettyPrinter().writeValueAsString(detail.get("payment"))))
        .append("</pre><h2>Dados contratuais</h2><pre style='white-space:pre-wrap'>")
        .append(escape(db.json.writerWithDefaultPrettyPrinter().writeValueAsString(detail.get("contractData"))))
        .append("</pre><h2>Orçamento integral / estandes</h2><pre style='white-space:pre-wrap'>")
        .append(escape(db.json.writerWithDefaultPrettyPrinter().writeValueAsString(q)))
        .append("</pre></div>");
      var body=Map.of("from",sender,"to",List.of(destination),"subject","Pedido comercial "+id+" — "+detail.get("fair"),"html",html.toString());
      var http=HttpClient.newHttpClient();
      var request=HttpRequest.newBuilder(URI.create("https://api.resend.com/emails"))
        .timeout(java.time.Duration.ofSeconds(15))
        .header("Authorization","Bearer "+emailKey).header("Content-Type","application/json")
        .POST(HttpRequest.BodyPublishers.ofString(db.encode(body))).build();
      var response=http.send(request,HttpResponse.BodyHandlers.ofString());
      return response.statusCode()>=200 && response.statusCode()<300;
    } catch(Exception ex) { return false; }
  }
  static String escape(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
  public Object list(String fairId) {
    return db.sql.queryForList("SELECT r.id,r.request_type,r.status,r.created_at,r.emailed_at,r.details,u.company,u.email FROM commercial_request r JOIN app_user u ON u.id=r.user_id WHERE r.fair_id=? ORDER BY r.created_at DESC LIMIT 500",fairId);
  }
}

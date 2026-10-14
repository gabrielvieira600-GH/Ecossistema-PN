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
      ContractData contractData, Requester requester) {}
  public record Requester(String company,String name,String email,String phone) {}

  @Transactional
  public Map<String,Object> submit(Payload payload, Auth.User user, String fairId) {
    if (payload == null || payload.selection() == null || payload.selection().ids() == null || payload.selection().ids().isEmpty())
      throw error(HttpStatus.BAD_REQUEST,"Selecione um estande.");
    var requester = payload.requester();
    if(requester==null || requester.company()==null || requester.name()==null || requester.email()==null || requester.phone()==null
      || requester.company().isBlank() || requester.name().isBlank() || !requester.email().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || requester.phone().isBlank()
      || requester.company().length()>160 || requester.name().length()>160 || requester.email().length()>240 || requester.phone().length()>60)
      throw error(HttpStatus.BAD_REQUEST,"Preencha os dados do solicitante corretamente.");
    var req=payload.selection();
    if (!List.of("CONTRACT","WAITLIST").contains(payload.requestType()))
      throw error(HttpStatus.BAD_REQUEST,"Tipo de solicitação inválido.");
    var q=commerce.quote(req,user);
    if (!fairId.equals(q.get("fairId"))) throw error(HttpStatus.FORBIDDEN,"Empresa incorreta.");
    if (!Boolean.TRUE.equals(q.get("ready")))
      throw error(HttpStatus.CONFLICT,"A comercialização depende de preços e planta publicados.");
    if (!Objects.equals(req.quoteHash(),q.get("hash")) || !req.acceptTerms())
      throw error(HttpStatus.CONFLICT,"O orçamento mudou. Revise e aceite novamente.");
    // Lock the fair before reading booth state; serialize competing submissions.
    db.lockFair(fairId);
    var booths=db.sql.queryForList("SELECT id,status,owner_id FROM booth WHERE id IN ("+
        String.join(",",Collections.nCopies(req.ids().size(),"?"))+")", req.ids().toArray());
    if (booths.size()!=req.ids().size()) throw error(HttpStatus.CONFLICT,"Estandes não encontrados.");
    boolean available=booths.stream().allMatch(b -> "AVAILABLE".equals(b.get("status")) ||
        ("RESERVED".equals(b.get("status")) && user.id().equals(b.get("owner_id"))));
    if (payload.requestType().equals("CONTRACT") && !available)
      throw error(HttpStatus.CONFLICT,"O estande está ocupado. Solicite entrada na fila.");
    if (payload.requestType().equals("WAITLIST") && (available || booths.size()!=1 || !List.of("CONTRACTED","RESERVED").contains(booths.get(0).get("status"))))
      throw error(HttpStatus.BAD_REQUEST,"Fila disponível apenas para um estande ocupado.");
    validatePayment(payload.payment(),fairId);
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
      if (db.sql.queryForObject("SELECT COUNT(*) FROM queue_entry WHERE booth_id=? AND user_id=?",
          Integer.class,boothId,user.id()) > 0)
        throw error(HttpStatus.CONFLICT,"Você já está na fila deste estande.");
      if (db.sql.queryForObject("SELECT COUNT(*) FROM queue_entry WHERE booth_id=? AND user_id=?",
          Integer.class,boothId,user.id()) == 0) {
        db.sql.update("INSERT INTO queue_entry(id,booth_id,user_id,joined_at) VALUES(?,?,?,?)",
          id(),boothId,user.id(),now());
      }
    }

    var detail=new LinkedHashMap<String,Object>();
    detail.put("requestType",payload.requestType()); detail.put("fair",q.get("fair"));
    detail.put("user",Map.of("company",user.company(),"email",user.email(),"contact",user.contact()));
    detail.put("requester",requester); detail.put("quote",q); detail.put("payment",payload.payment()); detail.put("contractData",cd);
    String id=id();
    db.sql.update("INSERT INTO commercial_request(id,fair_id,user_id,booth_ids,request_type,details,status,created_at) VALUES(?,?,?,?,?,?,?,?)",
      id,fairId,user.id(),db.encode(req.ids()),payload.requestType(),db.encode(detail),"PENDING_EMAIL",now());
    // The booth is occupied as soon as the commercial request is submitted.
    // The commercial workflow (signature/payment) remains pending independently.
    if (payload.requestType().equals("CONTRACT")) {
      String groupId=req.ids().size()>1 ? id() : null;
      for (var booth:booths) {
        int updated=db.sql.update("UPDATE booth SET status='CONTRACTED', owner_id=?, label=?, "
            + "expires_at=NULL, group_id=COALESCE(?,group_id), version=version+1 "
            + "WHERE id=? AND (status='AVAILABLE' OR (status='RESERVED' AND owner_id=?))",
            user.id(),requester.company().trim(),groupId,booth.get("id"),user.id());
        if(updated!=1) throw error(HttpStatus.CONFLICT,"Este estande acabou de ser ocupado. Atualize a planta.");
      }
    }
    db.notifyUser(user.id(),"Pedido recebido pelo portal: "+id+". Aguarda processamento comercial.");
    db.audit(user.email(),"PEDIDO_COMERCIAL",id,Map.of("fair",fairId,"type",payload.requestType()));
    boolean mailed=sendEmail(id,detail);
    if(mailed) db.sql.update("UPDATE commercial_request SET status='EMAILED', emailed_at=? WHERE id=?",now(),id);
    return Map.of("id",id,"emailSent",mailed,"message",mailed?
      "Seu pedido foi enviado para o comercial da "+q.get("fair")+". O estande já está ocupado no mapa, sujeito à formalização comercial.":
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
  // E-mail comercial com HTML compatível com Gmail, Outlook e clientes móveis.
  // Nunca anexe o snapshot técnico do banco: o conteúdo é apresentado em campos legíveis.
  String commercialEmailHtml(String id, Map<String,Object> detail) {
    @SuppressWarnings("unchecked")
    Map<String,Object> quote=(Map<String,Object>)detail.get("quote");
    @SuppressWarnings("unchecked")
    Map<String,Object> account=(Map<String,Object>)detail.get("user");
    Requester requester=(Requester)detail.get("requester");
    Payment payment=(Payment)detail.get("payment");
    ContractData contract=(ContractData)detail.get("contractData");
    boolean waitlist="WAITLIST".equals(detail.get("requestType"));
    String fair=Objects.toString(detail.get("fair"),"Feira");
    StringBuilder h=new StringBuilder(7000);
    h.append("<!doctype html><html lang='pt-BR'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1'></head>")
      .append("<body style='margin:0;padding:24px 10px;background:#edf3f5;font-family:Arial,Helvetica,sans-serif;color:#153247'>")
      .append("<table role='presentation' cellpadding='0' cellspacing='0' style='width:100%;max-width:720px;margin:auto;border-collapse:collapse;background:#ffffff;border-radius:14px;overflow:hidden'>")
      .append("<tr><td style='padding:30px;background:#08263b;color:#ffffff'>")
      .append("<div style='font-size:11px;letter-spacing:2px;color:#74e0cf;font-weight:bold'>ECOSSISTEMA PN · PORTAL DO EXPOSITOR</div>")
      .append("<h1 style='margin:12px 0 8px;font-size:27px;color:#ffffff'>")
      .append(waitlist?"Solicitação de fila de espera":"Pedido de contratação")
      .append("</h1><div style='font-size:15px;color:#bed5df'>").append(escape(fair)).append("</div>")
      .append("<div style='margin-top:15px;font-size:12px;color:#bed5df'>Protocolo: <strong style='color:#ffffff'>")
      .append(escape(id)).append("</strong></div></td></tr><tr><td style='padding:28px'>");

    section(h,"01 · Resumo da solicitação");
    row(h,"Tipo de pedido",waitlist?"Entrada na fila de espera":"Solicitação de contratação");
    row(h,"Feira",fair);
    row(h,"Pavilhão",value(quote.get("pavilion")));
    @SuppressWarnings("unchecked")
    List<Map<String,Object>> booths=(List<Map<String,Object>>)quote.get("items");
    if(booths!=null && !booths.isEmpty()) {
      StringJoiner codes=new StringJoiner(" + ");
      for(var booth:booths) codes.add(value(booth.get("code")));
      row(h,"Estande(s)",codes.toString());
    }
    row(h,"Área total",value(quote.get("area"))+" m²");
    if(waitlist) note(h,"O expositor solicitou posição na fila de espera. O ocupante atual não é substituído por este pedido.");

    section(h,"02 · Orçamento detalhado");
    h.append("<table role='presentation' cellpadding='0' cellspacing='0' style='border-collapse:collapse;width:100%;font-size:13px'>")
      .append("<tr style='background:#eaf5f2'><th align='left' style='padding:11px;border-bottom:1px solid #d8e4e9'>Descrição</th>")
      .append("<th align='right' style='padding:11px;border-bottom:1px solid #d8e4e9'>Valor</th></tr>");
    @SuppressWarnings("unchecked")
    List<Map<String,Object>> lines=(List<Map<String,Object>>)quote.get("lines");
    if(lines!=null) for(var line:lines) {
      h.append("<tr><td style='padding:11px 8px;border-bottom:1px solid #e5ecef'>")
       .append(escape(value(line.get("label")))).append("</td><td align='right' style='padding:11px 8px;border-bottom:1px solid #e5ecef;white-space:nowrap'>")
       .append(escape(brl(line.get("total")))).append("</td></tr>");
    }
    h.append("<tr style='background:#e7f6f2'><td style='padding:16px 10px;font-weight:bold;font-size:16px'>TOTAL DO ORÇAMENTO</td>")
     .append("<td align='right' style='padding:16px 10px;color:#008a7d;font-weight:bold;font-size:20px;white-space:nowrap'>")
     .append(escape(brl(quote.get("total")))).append("</td></tr></table>");
    row(h,"Lote", "Lote "+value(quote.get("lot"))+("0".equals(value(quote.get("lot")))?" · Renovação":""));
    note(h,"Valores apresentados conforme a simulação confirmada pelo solicitante. Sujeitos à validação comercial e formalização contratual.");

    section(h,"03 · Condições de pagamento");
    if(payment!=null) {
      switch (value(payment.type())) {
        case "CASH" -> row(h,"Condição","À vista");
        case "INSTALLMENTS" -> row(h,"Condição","Parcelado");
        case "NEGOTIATE" -> row(h,"Condição","Outras condições a negociar");
        default -> row(h,"Condição",value(payment.type()));
      }
      if(!"NEGOTIATE".equals(payment.type())) {
        row(h,"Melhor dia de vencimento","Dia "+payment.dueDay());
        if("INSTALLMENTS".equals(payment.type())) row(h,"Quantidade de parcelas",value(payment.installments()));
        row(h,"Entrada de 20% agora",payment.twentyPercentNow()?"Sim":"Não");
        if(payment.twentyPercentNow()) row(h,"Parcelar saldo a partir de janeiro",Boolean.TRUE.equals(payment.installmentsFromJanuary())?"Sim":"Não");
      }
      if(payment.customTerms()!=null && !payment.customTerms().isBlank()) row(h,"Condições propostas",payment.customTerms());
    }

    section(h,"04 · Dados para elaboração do contrato");
    if(contract!=null) {
      switch(value(contract.mode())) {
        case "SAME" -> note(h,"O solicitante confirmou que todos os dados para elaboração do contrato são os mesmos da edição anterior, incluindo responsável pela assinatura, responsável financeiro e responsável pelo estande.");
        case "CHANGE" -> { row(h,"Opção","Alterar dados do contrato");row(h,"Alterações solicitadas",contract.changes()); }
        case "NEW" -> {
          row(h,"Opção","Sou um novo expositor");
          var form=contract.newExhibitor();
          if(form!=null) {
            Map<String,String> names=new LinkedHashMap<>();
            names.put("company","Razão social / empresa"); names.put("taxId","CNPJ / identificação fiscal");
            names.put("contact","Pessoa de contato"); names.put("email","E-mail"); names.put("phone","Telefone");
            names.put("address","Endereço"); names.put("city","Cidade"); names.put("state","Estado");
            names.put("postalCode","CEP"); names.put("signatory","Responsável pela assinatura");
            names.put("financialContact","Responsável financeiro"); names.put("boothContact","Responsável pelo estande");
            for(var entry:names.entrySet()) if(form.containsKey(entry.getKey())) row(h,entry.getValue(),form.get(entry.getKey()));
            for(var entry:form.entrySet()) if(!names.containsKey(entry.getKey())) row(h,entry.getKey(),entry.getValue());
          }
        }
        default -> row(h,"Opção",contract.mode());
      }
    }

    section(h,"05 · Dados do solicitante");
    if(requester!=null) {
      row(h,"Empresa contratante",requester.company());
      row(h,"Nome do responsável",requester.name());
      row(h,"E-mail",requester.email());
      row(h,"Telefone",requester.phone());
    }
    if(account!=null) row(h,"Conta utilizada no portal",value(account.get("email")));
    h.append("<p style='margin:28px 0 0;padding:16px;background:#f0f5f7;border-radius:9px;font-size:12px;line-height:1.6;color:#526d7c'>")
      .append("Este relatório reúne as informações declaradas na triagem. O envio do pedido não comprova assinatura contratual nem pagamento.")
      .append("</p></td></tr><tr><td style='padding:18px 28px;background:#f3f7f8;color:#68808b;font-size:11px'>")
      .append("Ecossistema PN · Relatório gerado automaticamente pelo Portal do Expositor")
      .append("</td></tr></table></body></html>");
    return h.toString();
  }

  static void section(StringBuilder h,String label) {
    h.append("<h2 style='font-size:17px;color:#0a3046;margin:30px 0 12px;border-bottom:2px solid #bdeae2;padding-bottom:9px'>")
      .append(escape(label)).append("</h2>");
  }
  static void row(StringBuilder h,String label,String val) {
    h.append("<table role='presentation' cellpadding='0' cellspacing='0' style='border-collapse:collapse;width:100%;border-bottom:1px solid #e7eef1;font-size:13px'>")
      .append("<tr><td valign='top' style='width:42%;padding:10px 8px 10px 0;color:#627c8a'>").append(escape(label))
      .append("</td><td valign='top' style='padding:10px 0;font-weight:bold;color:#18394b;white-space:pre-wrap;overflow-wrap:anywhere'>")
      .append(escape(value(val))).append("</td></tr></table>");
  }
  static void note(StringBuilder h,String val) {
    h.append("<p style='padding:13px 15px;background:#f1f8f7;border-left:3px solid #0b998b;font-size:13px;line-height:1.6'>")
      .append(escape(val)).append("</p>");
  }
  static String value(Object o) {return o==null?"Não informado":String.valueOf(o);}
  static String brl(Object o) {
    try {
      var number=new java.math.BigDecimal(String.valueOf(o));
      return java.text.NumberFormat.getCurrencyInstance(new Locale("pt","BR")).format(number);
    } catch(Exception e) {return "R$ "+value(o);}
  }
  boolean sendEmail(String id,Map<String,Object> detail) {
    if(emailKey.isBlank() || sender.isBlank()) return false;
    try {
      var body=Map.of("from",sender,"to",List.of(destination),
        "subject",("WAITLIST".equals(detail.get("requestType"))?"Fila de espera":"Pedido de contratação")+" "+id+" — "+detail.get("fair"),
        "html",commercialEmailHtml(id,detail));
      var http=HttpClient.newHttpClient();
      var request=HttpRequest.newBuilder(URI.create("https://api.resend.com/emails"))
        .timeout(java.time.Duration.ofSeconds(15))
        .header("Authorization","Bearer "+emailKey).header("Content-Type","application/json")
        .POST(HttpRequest.BodyPublishers.ofString(db.encode(body))).build();
      var response=http.send(request,HttpResponse.BodyHandlers.ofString());
      if(response.statusCode()<200 || response.statusCode()>=300)
        org.slf4j.LoggerFactory.getLogger(CommercialRequests.class).warn(
          "Resend recusou o pedido comercial {} (HTTP {}). Confira Resend > Logs.",id,response.statusCode());
      return response.statusCode()>=200 && response.statusCode()<300;
    } catch(Exception ex) {
      org.slf4j.LoggerFactory.getLogger(CommercialRequests.class).warn("Erro no envio de e-mail do pedido {}: {}",id,ex.getClass().getSimpleName());
      return false;
    }
  }
  static String escape(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
  public Object list(String fairId) {
    return db.sql.queryForList("SELECT r.id,r.request_type,r.status,r.created_at,r.emailed_at,r.details,u.company,u.email FROM commercial_request r JOIN app_user u ON u.id=r.user_id WHERE r.fair_id=? ORDER BY r.created_at DESC LIMIT 500",fairId);
  }
}

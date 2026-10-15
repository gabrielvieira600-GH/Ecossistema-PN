package br.com.feiras;

import static br.com.feiras.Db.*;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/media")
public class MediaRequests {
  final Db db;
  final Auth auth;
  @Value("${RESEND_API_KEY:}") String apiKey;
  @Value("${COMMERCIAL_EMAIL_FROM:}") String emailFrom;
  @Value("${MEDIA_EMAIL_TO:gabriel@portosenavios.com.br}") String emailTo;

  public MediaRequests(Db db, Auth auth){ this.db=db; this.auth=auth; }

  // Server-owned price list from Kit de Mídia Portos e Navios 2026.
  static final Map<String,Item> CATALOG = Map.ofEntries(
    entry("site-square","Site","Quadrado",980,"mês"),
    entry("site-leader","Site","Super leaderboard",3420,"mês"),
    entry("site-rectangle","Site","Retângulo",2707,"mês"),
    entry("site-content","Site","Banner no conteúdo",2707,"mês"),
    entry("news-square-top","Newsletter","Quadrado · Topo",1990,"mês"),
    entry("news-square-mid","Newsletter","Quadrado · Meio",1390,"mês"),
    entry("news-highlight-top","Newsletter","Destaque · Topo",3690,"mês"),
    entry("news-highlight-mid","Newsletter","Destaque · Meio",3477,"mês"),
    entry("news-highlight-bottom","Newsletter","Destaque · Rodapé",3112,"mês"),
    entry("news-rectangle","Newsletter","Retângulo · Meio",2707,"mês"),
    entry("mag-overcover","Revista","Sobrecapa (capa 1)",33410,"edição"),
    entry("mag-fold","Revista","Capa dupla desdobrável",34080,"edição"),
    entry("mag-double","Revista","Página dupla",20907,"edição"),
    entry("mag-double-premium","Revista","Página dupla determinada",23117,"edição"),
    entry("mag-page","Revista","Página inteira",12900,"edição"),
    entry("mag-page-premium","Revista","Página inteira determinada",14138,"edição"),
    entry("mag-cover23","Revista","Capas 2 e 3",14180,"edição"),
    entry("mag-cover4","Revista","Capa 4",14822,"edição"),
    entry("mag-two-thirds","Revista","2/3 de página",9855,"edição"),
    entry("mag-half","Revista","1/2 página",7580,"edição"),
    entry("mag-third","Revista","1/3 de página",5556,"edição"),
    entry("mag-quarter","Revista","1/4 de página",4170,"edição"),
    entry("mag-sixth","Revista","1/6 de página",3044,"edição"),
    entry("editor-social","Ações editoriais","Trio de posts sociais",1533,"ação"),
    entry("editor-release","Ações editoriais","Press release no portal",1020,"ação"),
    entry("editor-email","Ações editoriais","Disparo de e-mail marketing",1395,"disparo")
  );
  record Item(String group,String name,BigDecimal price,String unit) {}
  private static Map.Entry<String,Item> entry(String id,String group,String name,int price,String unit){return Map.entry(id,new Item(group,name,BigDecimal.valueOf(price),unit));}
  public record Line(String id,int quantity) {}
  public record Proposal(List<Line> items,String company,String contact,String email,String phone,String taxId,String campaign,String start,String edition,String payment,String notes){}

  void requirePortos(HttpServletRequest request) {
    if(!"portos".equals(auth.portal(request))) throw error(HttpStatus.FORBIDDEN,"Este recurso pertence ao portal Portos e Navios.");
  }
  static String checked(String value,String label,int max,boolean required){
    String v=Objects.toString(value,"").trim();
    if(v.length()>max || (required&&v.isBlank()))throw error(HttpStatus.BAD_REQUEST,"Confira o campo "+label+".");
    return v;
  }
  @PostMapping("/requests")
  @Transactional
  public Map<String,Object> submit(@RequestBody Proposal p,HttpServletRequest request){
    requirePortos(request);
    if(p==null||p.items()==null||p.items().isEmpty()||p.items().size()>26)throw error(HttpStatus.BAD_REQUEST,"Escolha entre 1 e 26 formatos.");
    var user=Auth.current();
    var company=checked(p.company(),"Empresa contratante",160,true);
    var contact=checked(p.contact(),"Responsável",160,true);
    var email=checked(p.email(),"E-mail",240,true);
    if(!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))throw error(HttpStatus.BAD_REQUEST,"E-mail inválido.");
    var phone=checked(p.phone(),"Telefone",60,true);
    var taxId=checked(p.taxId(),"CNPJ/CPF",40,false);
    var campaign=checked(p.campaign(),"Campanha",160,false);
    var start=checked(p.start(),"Período",7,false);
    if(!start.isBlank()&&!start.matches("\\d{4}-(0[1-9]|1[0-2])"))throw error(HttpStatus.BAD_REQUEST,"Período inválido.");
    var edition=checked(p.edition(),"Edição",120,false);
    var payment=checked(p.payment(),"Condição de pagamento",90,true);
    if(!List.of("A negociar","À vista","Parcelado — sujeito à aprovação").contains(payment))throw error(HttpStatus.BAD_REQUEST,"Condição de pagamento inválida.");
    var notes=checked(p.notes(),"Observações",3000,false);
    var sum=BigDecimal.ZERO;
    var lines=new ArrayList<Map<String,Object>>();
    var ids=new HashSet<String>();
    for(var line:p.items()){
      if(line==null||!ids.add(line.id()))throw error(HttpStatus.BAD_REQUEST,"Formato duplicado ou inválido.");
      var item=CATALOG.get(line.id());
      if(item==null||line.quantity()<1||line.quantity()>24)throw error(HttpStatus.BAD_REQUEST,"Formato ou quantidade inválidos.");
      var total=item.price().multiply(BigDecimal.valueOf(line.quantity()));
      sum=sum.add(total);
      lines.add(Map.of("id",line.id(),"group",item.group(),"name",item.name(),"unit",item.unit(),"price",item.price(),"quantity",line.quantity(),"total",total));
    }
    var details=new LinkedHashMap<String,Object>();
    details.put("company",company);details.put("contact",contact);details.put("email",email);details.put("phone",phone);
    details.put("taxId",taxId);details.put("campaign",campaign);details.put("start",start);details.put("edition",edition);
    details.put("payment",payment);details.put("notes",notes);details.put("lines",lines);details.put("total",sum);
    details.put("catalogYear",2026);details.put("status","SUJEITO_A_APROVACAO_COMERCIAL");
    var id=id();
    db.sql.update("INSERT INTO media_request(id,user_id,details,total,status,created_at) VALUES(?,?,?,?,?,?)",id,user.id(),db.encode(details),sum,"PENDING_EMAIL",now());
    db.audit(user.email(),"PEDIDO_MIDIA",id,Map.of("company",company,"total",sum));
    boolean sent=sendEmail(id,details,lines,sum);
    if(sent) db.sql.update("UPDATE media_request SET status='EMAILED',emailed_at=? WHERE id=?",now(),id);
    return Map.of("id",id,"emailSent",sent,"total",sum);
  }
  @GetMapping("/admin/requests")
  public Object list(HttpServletRequest req){requirePortos(req);Auth.admin();return db.sql.queryForList("SELECT id,user_id,details,total,status,created_at,emailed_at FROM media_request ORDER BY created_at DESC LIMIT 500");}

  static String html(String s){return Objects.toString(s,"").replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
  static String money(BigDecimal n){return java.text.NumberFormat.getCurrencyInstance(new Locale("pt","BR")).format(n);}
  boolean sendEmail(String id,Map<String,Object> d,List<Map<String,Object>> lines,BigDecimal total){
    if(apiKey.isBlank()||emailFrom.isBlank()||emailTo.isBlank()) return false;
    var b=new StringBuilder();
    b.append("<html><body style='background:#f3f5f7;padding:28px;font-family:Arial,sans-serif;color:#203443'>")
     .append("<div style='max-width:740px;margin:auto;background:white;border-radius:14px;overflow:hidden'>")
     .append("<div style='background:#122634;color:white;padding:28px'><small style='color:#ff898a'>PORTOS E NAVIOS · PUBLICIDADE</small><h1>Solicitação de mídia</h1><p>Protocolo ").append(html(id)).append("</p></div><div style='padding:28px'>")
     .append("<h2>Dados do solicitante</h2><p><b>Empresa:</b> ").append(html((String)d.get("company")))
     .append("<br><b>Responsável:</b> ").append(html((String)d.get("contact")))
     .append("<br><b>E-mail:</b> ").append(html((String)d.get("email")))
     .append("<br><b>Telefone:</b> ").append(html((String)d.get("phone")))
     .append("<br><b>CNPJ/CPF:</b> ").append(html((String)d.get("taxId"))).append("</p>")
     .append("<h2>Campanha e condições</h2><p><b>Campanha:</b> ").append(html((String)d.get("campaign")))
     .append("<br><b>Período desejado:</b> ").append(html((String)d.get("start")))
     .append("<br><b>Edição:</b> ").append(html((String)d.get("edition")))
     .append("<br><b>Pagamento preferido:</b> ").append(html((String)d.get("payment")))
     .append("<br><b>Observações:</b> ").append(html((String)d.get("notes"))).append("</p>")
     .append("<h2>Orçamento estimado</h2><table style='width:100%;border-collapse:collapse'><tr style='background:#e8eef1'><th align='left' style='padding:10px'>Formato</th><th>Qtd.</th><th align='right'>Total</th></tr>");
    for(var l:lines)b.append("<tr><td style='padding:12px;border-bottom:1px solid #e4e9ec'>").append(html((String)l.get("name"))).append("<br><small>").append(html((String)l.get("group"))).append(" · ").append(html((String)l.get("unit"))).append("</small></td><td align='center'>").append(l.get("quantity")).append("</td><td align='right'>").append(money((BigDecimal)l.get("total"))).append("</td></tr>");
    b.append("</table><h2 style='text-align:right;color:#d32531'>").append(money(total)).append("</h2><p style='font-size:12px;color:#73848c'>Valores de referência: Kit de Mídia 2026. Disponibilidade, datas, valores finais e condições sujeitos à aprovação comercial. Solicitação não equivale a contrato ou pagamento.</p></div></div></body></html>");
    try{
      var body=Map.of("from",emailFrom,"to",List.of(emailTo),"subject","Pedido de mídia "+id+" — "+d.get("company"),"html",b.toString());
      var request=HttpRequest.newBuilder(URI.create("https://api.resend.com/emails")).timeout(Duration.ofSeconds(15)).header("Authorization","Bearer "+apiKey).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(db.encode(body))).build();
      var response=HttpClient.newHttpClient().send(request,HttpResponse.BodyHandlers.ofString());
      return response.statusCode()>=200&&response.statusCode()<300;
    }catch(Exception e){return false;}
  }
}

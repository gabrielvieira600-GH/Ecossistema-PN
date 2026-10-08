package br.com.feiras;

import static br.com.feiras.Db.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(
    properties = {
      "spring.datasource.url=${TEST_DATABASE_URL:jdbc:h2:mem:workflow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE}",
      "spring.datasource.username=${TEST_DATABASE_USERNAME:sa}",
      "spring.datasource.password=${TEST_DATABASE_PASSWORD:}",
      "app.admin-email=test-admin@example.test",
      "app.admin-password=test-password-very-strong",
      "app.secure-cookie=false"
    })
@AutoConfigureMockMvc
class WorkflowTest {
  @Autowired Db db;
  @Autowired Commerce service;
  @Autowired Maps maps;
  @Autowired Auth auth;
  @Autowired MockMvc mvc;
  Auth.User u, v, admin;
  String fair, pavilion, a, b;

  @BeforeEach
  void setup() {
    u = createUser("EXHIBITOR");
    v = createUser("EXHIBITOR");
    admin = createUser("ADMIN");
    fair = id();
    pavilion = id();
    a = id();
    b = id();
    db.sql.update(
        "INSERT INTO fair(id,name,published,rate,assembly_rate,fixed_fee,tax_percent,terms)"
            + " VALUES(?,?,TRUE,100,20,50,10,?)",
        fair,
        "Test fair",
        "Condições comerciais de teste completas com pagamento e cancelamento.");
    db.sql.update(
        "INSERT INTO"
            + " pavilion(id,fair_id,name,image_path,clean_path,original_pdf,map_width,map_height,reviewed)"
            + " VALUES(?,?,?,'/maps/test.png','/maps/test.png','/maps/test.pdf',1000,700,TRUE)",
        pavilion,
        fair,
        "Test pavilion");
    createBooth(a, "A", 0);
    createBooth(b, "B", 100);
  }

  Auth.User createUser(String role) {
    String uid = id(), email = uid + "@example.test";
    db.sql.update(
        "INSERT INTO app_user(id,email,password_hash,company,contact,role,enabled,created_at)"
            + " VALUES(?,?,?,?,?,?,TRUE,?)",
        uid,
        email,
        auth.passwordHash("test-password-very-strong"),
        "Empresa " + uid,
        "Test",
        role,
        now());
    return new Auth.User(uid, email, "Empresa " + uid, "Test", "", role, true);
  }

  void createBooth(String uid, String code, double x) {
    db.sql.update(
        "INSERT INTO booth(id,pavilion_id,code,area,geometry,verified) VALUES(?,?,?,12,?,TRUE)",
        uid,
        pavilion,
        code,
        db.encode(
            List.of(List.of(x, 0), List.of(x + 100, 0), List.of(x + 100, 100), List.of(x, 100))));
  }

  long version(String uid) {
    return ((Number) db.one("SELECT version FROM booth WHERE id=?", uid).get("version"))
        .longValue();
  }

  Commerce.Selection selection(String... ids) {
    var versions = new HashMap<String, Long>();
    for (String uid : ids) versions.put(uid, version(uid));
    return new Commerce.Selection(List.of(ids), versions, true, null, false);
  }

  @Test
  void twoUsersCannotClaimSameBooth() throws Exception {
    var s = selection(a);
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);
    var pool = Executors.newFixedThreadPool(2);
    try {
      List<Future<Boolean>> results = new ArrayList<>();
      for (var user : List.of(u, v))
        results.add(
            pool.submit(
                () -> {
                  ready.countDown();
                  start.await();
                  try {
                    service.reserve(s, user, false);
                    return true;
                  } catch (ResponseStatusException e) {
                    assertEquals(409, e.getStatusCode().value());
                    return false;
                  }
                }));
      assertTrue(ready.await(5, TimeUnit.SECONDS));
      start.countDown();
      int successes = 0;
      for (var f : results) if (f.get(10, TimeUnit.SECONDS)) successes++;
      assertEquals(1, successes);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void releaseIsPrivateAndPromotesFirstInQueue() {
    service.reserve(selection(a), u, false);
    service.queue(a, version(a), v);
    assertThrows(ResponseStatusException.class, () -> service.release(selection(a), v));
    service.release(selection(a), u);
    var row = db.one("SELECT * FROM booth WHERE id=?", a);
    assertEquals(v.id(), row.get("owner_id"));
    assertEquals("RESERVED", row.get("status"));
    assertEquals(
        0,
        db.sql.queryForObject(
            "SELECT COUNT(*) FROM queue_entry WHERE booth_id=?", Integer.class, a));
    assertTrue(instant(row.get("expires_at")).isAfter(Instant.now()));
  }

  @Test
  void expiryPromotesQueueAndCannotBeExtendedByMerge() {
    service.reserve(selection(a), u, false);
    db.sql.update(
        "UPDATE booth SET expires_at=? WHERE id=?",
        Timestamp.from(Instant.now().plusSeconds(3600)),
        a);
    Instant before =
        instant(db.one("SELECT expires_at FROM booth WHERE id=?", a).get("expires_at"));
    service.reserve(selection(a, b), u, true);
    assertEquals(
        before, instant(db.one("SELECT expires_at FROM booth WHERE id=?", b).get("expires_at")));
    service.queue(a, version(a), v);
    db.sql.update(
        "UPDATE booth SET expires_at=? WHERE id IN (?,?)",
        Timestamp.from(Instant.now().minusSeconds(1)),
        a,
        b);
    service.sweep();
    assertEquals(v.id(), db.one("SELECT owner_id FROM booth WHERE id=?", a).get("owner_id"));
    assertEquals("AVAILABLE", db.one("SELECT status FROM booth WHERE id=?", b).get("status"));
  }

  @Test
  void contractStoresQuoteAndRejectsChangedPrice() {
    var s = selection(a);
    var quote = service.quote(s, u);
    assertEquals(new BigDecimal("1639.00"), quote.get("total"));
    db.sql.update("UPDATE fair SET rate=101,version=version+1 WHERE id=?", fair);
    var stale =
        new Commerce.Selection(s.ids(), s.versions(), true, (String) quote.get("hash"), true);
    assertThrows(ResponseStatusException.class, () -> service.contract(stale, u, "127.0.0.1"));
    quote = service.quote(selection(a), u);
    var r =
        service.contract(
            new Commerce.Selection(
                List.of(a), Map.of(a, version(a)), true, (String) quote.get("hash"), true),
            u,
            "127.0.0.1");
    assertEquals("CONTRACTED", db.one("SELECT status FROM booth WHERE id=?", a).get("status"));
    String snapshot =
        (String)
            db.one("SELECT snapshot FROM contract_order WHERE id=?", r.get("id")).get("snapshot");
    db.sql.update("UPDATE fair SET rate=200 WHERE id=?", fair);
    assertTrue(snapshot.contains("101.00"));
    service.cancelContract(
        (String) r.get("id"), "Cancelamento confirmado pela organização.", admin);
    assertEquals("AVAILABLE", db.one("SELECT status FROM booth WHERE id=?", a).get("status"));
  }

  @Test
  void connectedUnionRequiresAllMembersAndBlocksRoads() {
    db.sql.update(
        "UPDATE booth SET geometry=? WHERE id=?",
        db.encode(List.of(List.of(120, 0), List.of(220, 0), List.of(220, 100), List.of(120, 100))),
        b);
    assertThrows(ResponseStatusException.class, () -> service.reserve(selection(a, b), u, false));
    db.sql.update(
        "UPDATE booth SET geometry=? WHERE id=?",
        db.encode(List.of(List.of(100, 0), List.of(200, 0), List.of(200, 100), List.of(100, 100))),
        b);
    service.reserve(selection(a, b), u, false);
    assertThrows(ResponseStatusException.class, () -> service.release(selection(a), u));
    service.release(selection(a, b), u);
  }

  @Test
  void editActuallyPersistsFontAndChecksVersion() {
    var row = db.one("SELECT * FROM booth WHERE id=?", a);
    var edit =
        new Maps.Edit(
            version(a),
            "A",
            new BigDecimal("12"),
            "3 x 4 m",
            db.points((String) row.get("geometry")),
            new BigDecimal("12"),
            "Empresa teste",
            "AVAILABLE",
            true);
    maps.edit(a, edit, admin);
    assertEquals(
        new BigDecimal("12.00"),
        db.one("SELECT font_size FROM booth WHERE id=?", a).get("font_size"));
    assertThrows(ResponseStatusException.class, () -> maps.edit(a, edit, admin));
  }

  @Test
  void apiEnforcesRoleCsrfAndOwnContractAccess() throws Exception {
    // The portal-aware endpoints require a REAL auth_session cookie. A mocked
    // Spring Security principal alone no longer represents a logged-in user.
    db.sql.update("UPDATE pavilion SET fair_id='navalshore-2027' WHERE id=?", pavilion);
    db.sql.update("UPDATE fair SET published=TRUE WHERE id='navalshore-2027'");
    var response = new MockHttpServletResponse();
    auth.login(new Auth.Login(u.email(), "test-password-very-strong", "navalshore"),
        "127.0.0.1", response);
    String token = response.getHeader("Set-Cookie").split(";", 2)[0].split("=", 2)[1];
    var sessionCookie = new Cookie("FEIRA_SESSION", token);

    mvc.perform(get("/api/admin/users").cookie(sessionCookie))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/reserve")
                .cookie(sessionCookie)
                .contentType("application/json")
                .content(db.encode(selection(a))))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/reserve")
                .cookie(sessionCookie)
                .with(csrf())
                .contentType("application/json")
                .content(db.encode(selection(a))))
        .andExpect(status().isOk());
  }

  @Test
  void largeUnionStoresAllComponentsWithoutOverflowingAuditTarget() {
    String c = id(), d = id(), e = id();
    createBooth(c, "C", 200);
    createBooth(d, "D", 300);
    createBooth(e, "E", 400);
    service.reserve(selection(a, b, c, d, e), u, false);
    var group = db.one("SELECT group_id FROM booth WHERE id=?", a).get("group_id");
    assertEquals(
        5,
        db.sql.queryForObject("SELECT COUNT(*) FROM booth WHERE group_id=?", Integer.class, group));
    var audit = db.one("SELECT * FROM audit_log WHERE action='RESERVAR' AND target=?", group);
    assertTrue(((String) audit.get("details")).contains(e));
    service.release(selection(a, b, c, d, e), u);
    assertEquals(
        5,
        db.sql.queryForObject(
            "SELECT COUNT(*) FROM booth WHERE pavilion_id=? AND status='AVAILABLE'",
            Integer.class,
            pavilion));
  }

  @Test
  void loginCookieAndDisabledUserCannotAuthenticate() {
    var response = new org.springframework.mock.web.MockHttpServletResponse();
    auth.login(new Auth.Login(u.email(), "test-password-very-strong"), "unit-test", response);
    String cookie = response.getHeader("Set-Cookie");
    assertTrue(cookie.contains("HttpOnly"));
    assertTrue(cookie.contains("SameSite=Strict"));
    String t = cookie.substring("FEIRA_SESSION=".length()).split(";")[0];
    assertEquals(u.id(), auth.authenticate(t).id());
    db.sql.update("UPDATE app_user SET enabled=FALSE WHERE id=?", u.id());
    assertNull(auth.authenticate(t));
  }

  @Test
  void closedFairRejectsReservationsAndQueueButAllowsRelease() {
    service.reserve(selection(a), u, false);
    db.sql.update("UPDATE fair SET published=FALSE WHERE id=?", fair);
    assertThrows(ResponseStatusException.class, () -> service.reserve(selection(b), v, false));
    assertThrows(ResponseStatusException.class, () -> service.queue(a, version(a), v));
    service.release(selection(a), u);
    assertEquals("AVAILABLE", db.one("SELECT status FROM booth WHERE id=?", a).get("status"));
  }

  @Test
  void occupiedBoothPreservesStructureButAllowsCompanyFontChanges() {
    service.reserve(selection(a), u, false);
    var row = db.one("SELECT * FROM booth WHERE id=?", a);
    var geometry = db.points((String) row.get("geometry"));
    assertThrows(ResponseStatusException.class, () -> maps.edit(a,
        new Maps.Edit(version(a), "A", new BigDecimal("18"), "", geometry,
            new BigDecimal("9"), "Novo nome", "RESERVED", true), admin));
    maps.edit(a, new Maps.Edit(version(a), "A", new BigDecimal("12"), "", geometry,
        new BigDecimal("9"), "Novo nome", "RESERVED", true), admin);
    assertEquals(new BigDecimal("12.00"), db.one("SELECT area FROM booth WHERE id=?", a).get("area"));
    assertEquals("Novo nome", db.one("SELECT label FROM booth WHERE id=?", a).get("label"));
    assertEquals(new BigDecimal("9.00"), db.one("SELECT font_size FROM booth WHERE id=?", a).get("font_size"));
  }

  @Test
  void administrativeAssignmentCannotSkipWaitingCompany() {
    db.sql.update("UPDATE booth SET status='BLOCKED' WHERE id=?", a);
    service.queue(a, version(a), v);
    assertThrows(ResponseStatusException.class, () -> maps.assign(a, version(a), u.id(), admin));
    var row = db.one("SELECT * FROM booth WHERE id=?", a);
    maps.edit(a, new Maps.Edit(version(a), "A", new BigDecimal("12"), "",
        db.points((String) row.get("geometry")), new BigDecimal("6"), "", "AVAILABLE", true), admin);
    assertEquals(v.id(), db.one("SELECT owner_id FROM booth WHERE id=?", a).get("owner_id"));
  }

  @Test
  void preferenceQueueWaitsWhileFairIsClosedAndResumesBeforePublicReservation() {
    service.reserve(selection(a), u, false);
    service.queue(a, version(a), v);
    db.sql.update("UPDATE fair SET published=FALSE WHERE id=?", fair);
    service.release(selection(a), u);
    assertEquals("AVAILABLE", db.one("SELECT status FROM booth WHERE id=?", a).get("status"));
    assertEquals(1, db.sql.queryForObject("SELECT COUNT(*) FROM queue_entry WHERE booth_id=?", Integer.class, a));
    db.sql.update("UPDATE fair SET published=TRUE WHERE id=?", fair);
    service.sweep();
    assertEquals(v.id(), db.one("SELECT owner_id FROM booth WHERE id=?", a).get("owner_id"));
    assertThrows(ResponseStatusException.class, () -> service.reserve(selection(a), u, false));
  }
}

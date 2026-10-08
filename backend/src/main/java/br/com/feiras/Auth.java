package br.com.feiras;

import static br.com.feiras.Db.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.validation.constraints.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.csrf.*;
import org.springframework.stereotype.*;
import org.springframework.web.filter.OncePerRequestFilter;

@Service
public class Auth {
  public record User(
      String id,
      String email,
      String company,
      String contact,
      String phone,
      String role,
      boolean enabled) {}

  public record Register(
      @Email @NotBlank @Size(max = 254) String email,
      @NotBlank String password,
      @NotBlank @Size(max = 160) String company,
      @NotBlank @Size(max = 160) String contact,
      @Size(max = 40) String phone) {}

  public record Login(@Email @NotBlank String email, @NotBlank String password) {}

  private final Db db;
  private final PasswordEncoder passwords;
  final boolean secure;
  private final boolean registrationOpen;

  public Auth(
      Db db,
      PasswordEncoder passwords,
      @Value("${app.secure-cookie}") boolean secure,
      @Value("${app.registration-open}") boolean registrationOpen) {
    this.db = db;
    this.passwords = passwords;
    this.secure = secure;
    this.registrationOpen = registrationOpen;
  }

  public static String digest(String s) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public static String token() {
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public static void strong(String password) {
    if (password == null
        || password.length() < 12
        || password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw error(
          HttpStatus.BAD_REQUEST, "Use uma senha com pelo menos 12 caracteres e até 72 bytes.");
  }

  public User user(Map<String, Object> u) {
    return new User(
        (String) u.get("id"),
        (String) u.get("email"),
        (String) u.get("company"),
        (String) u.get("contact"),
        (String) u.get("phone"),
        (String) u.get("role"),
        (Boolean) u.get("enabled"));
  }

  public void throttle(String key, int limit) {
    String h = digest(key);
    Timestamp cutoff = Timestamp.from(Instant.now().minusSeconds(900));
    int n =
        db.sql.update(
            "UPDATE auth_limit SET hits=hits+1 WHERE key_hash=? AND started_at>=?", h, cutoff);
    if (n == 0)
      n =
          db.sql.update(
              "UPDATE auth_limit SET hits=1,started_at=? WHERE key_hash=? AND started_at<?",
              now(),
              h,
              cutoff);
    if (n == 0) {
      try {
        db.sql.update("INSERT INTO auth_limit(key_hash,hits,started_at) VALUES(?,1,?)", h, now());
      } catch (DuplicateKeyException e) {
        db.sql.update("UPDATE auth_limit SET hits=hits+1 WHERE key_hash=?", h);
      }
    }
    if (((Number) db.one("SELECT hits FROM auth_limit WHERE key_hash=?", h).get("hits")).intValue()
        > limit)
      throw error(HttpStatus.TOO_MANY_REQUESTS, "Muitas tentativas. Aguarde 15 minutos.");
  }

  public void register(Register r, String ip) {
    if (!registrationOpen)
      throw error(HttpStatus.FORBIDDEN, "Cadastro fechado. Contate a organização.");
    throttle("register:" + ip, 5);
    strong(r.password());
    String email = r.email().trim().toLowerCase(Locale.ROOT);
    try {
      db.sql.update(
          "INSERT INTO"
              + " app_user(id,email,password_hash,company,contact,phone,role,enabled,created_at)"
              + " VALUES(?,?,?,?,?,?,'EXHIBITOR',FALSE,?)",
          id(),
          email,
          passwords.encode(r.password()),
          r.company().trim(),
          r.contact().trim(),
          r.phone() == null ? "" : r.phone().trim(),
          now());
    } catch (DuplicateKeyException e) {
      throw error(
          HttpStatus.CONFLICT, "Não foi possível cadastrar este e-mail. Consulte a organização.");
    }
  }

  public User login(Login r, String ip, HttpServletResponse response) {
    String email = r.email().trim().toLowerCase(Locale.ROOT);
    throttle("login-ip:" + ip, 40);
    throttle("login-email:" + email, 12);
    var rows = db.sql.queryForList("SELECT * FROM app_user WHERE email=?", email);
    // Same expensive comparison for unknown accounts to reduce timing differences.
    String hash = rows.isEmpty() ? DUMMY_HASH : (String) rows.get(0).get("password_hash");
    boolean ok = passwords.matches(r.password(), hash);
    if (!ok || rows.isEmpty()) throw error(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
    User u = user(rows.get(0));
    if (!u.enabled())
      throw error(HttpStatus.FORBIDDEN, "Seu cadastro aguarda aprovação da organização.");
    String t = token();
    db.sql.update(
        "INSERT INTO auth_session(token_hash,user_id,expires_at) VALUES(?,?,?)",
        digest(t),
        u.id(),
        Timestamp.from(Instant.now().plusSeconds(30L * 86400)));
    cookie(response, t, 30L * 86400);
    db.audit(u.email(), "LOGIN", u.id(), Map.of());
    return u;
  }

  private static final String DUMMY_HASH =
      new BCryptPasswordEncoder(12).encode("dummy-password-not-a-real-account");

  public void cookie(HttpServletResponse response, String token, long maxAge) {
    response.addHeader(
        HttpHeaders.SET_COOKIE,
        ResponseCookie.from("FEIRA_SESSION", token)
            .httpOnly(true)
            .secure(secure)
            .sameSite("Strict")
            .path("/")
            .maxAge(maxAge)
            .build()
            .toString());
  }

  public String cookieToken(HttpServletRequest r) {
    if (r.getCookies() != null)
      for (Cookie c : r.getCookies()) if (c.getName().equals("FEIRA_SESSION")) return c.getValue();
    return null;
  }

  public User authenticate(String token) {
    if (token == null || token.length() != 43) return null;
    var rows =
        db.sql.queryForList(
            "SELECT u.* FROM auth_session s JOIN app_user u ON u.id=s.user_id WHERE s.token_hash=?"
                + " AND s.expires_at>? AND u.enabled=TRUE",
            digest(token),
            now());
    return rows.isEmpty() ? null : user(rows.get(0));
  }

  public void logout(HttpServletRequest r, HttpServletResponse response) {
    String t = cookieToken(r);
    if (t != null) db.sql.update("DELETE FROM auth_session WHERE token_hash=?", digest(t));
    cookie(response, "", 0);
  }

  public String passwordHash(String value) {
    strong(value);
    return passwords.encode(value);
  }

  public static User current() {
    var a = SecurityContextHolder.getContext().getAuthentication();
    if (a == null || !(a.getPrincipal() instanceof User u))
      throw error(HttpStatus.UNAUTHORIZED, "Entre para continuar.");
    return u;
  }

  public static User admin() {
    User u = current();
    if (!u.role().equals("ADMIN"))
      throw error(HttpStatus.FORBIDDEN, "Acesso exclusivo de administradores.");
    return u;
  }
}

@Configuration
class SecurityConfig {
  @Bean
  PasswordEncoder passwords() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  SecurityFilterChain chain(HttpSecurity http, Auth auth) throws Exception {
    var csrf = new CookieCsrfTokenRepository();
    csrf.setCookieCustomizer(c -> c.secure(auth.secure).sameSite("Strict").path("/"));
    http.sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.csrf(
        c ->
            c.csrfTokenRepository(csrf)
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()));
    http.authorizeHttpRequests(
        c ->
            c.requestMatchers(
                    "/",
                    "/index.html",
                    "/assets/**",
                    "/favicon.svg",
                    "/api/health",
                    "/api/auth/csrf",
                    "/api/auth/login",
                    "/api/auth/register")
                .permitAll()
                .anyRequest()
                .authenticated());
    http.addFilterBefore(
        new OncePerRequestFilter() {
          @Override
          protected void doFilterInternal(
              HttpServletRequest req, HttpServletResponse res, FilterChain chain)
              throws ServletException, IOException {
            var u = auth.authenticate(auth.cookieToken(req));
            if (u != null)
              SecurityContextHolder.getContext()
                  .setAuthentication(
                      new UsernamePasswordAuthenticationToken(
                          u, null, List.of(new SimpleGrantedAuthority("ROLE_" + u.role()))));
            chain.doFilter(req, res);
          }
        },
        AnonymousAuthenticationFilter.class);
    http.exceptionHandling(
        c ->
            c.authenticationEntryPoint(
                    (r, s, e) -> {
                      s.setStatus(401);
                      s.setContentType("application/json");
                      s.getWriter().write("{\"message\":\"Entre para continuar.\"}");
                    })
                .accessDeniedHandler(
                    (r, s, e) -> {
                      s.setStatus(403);
                      s.setContentType("application/json");
                      s.getWriter()
                          .write(
                              "{\"message\":\"Acesso negado ou token de segurança expirado."
                                  + " Atualize a página.\"}");
                    }));
    http.headers(
        c ->
            c.contentSecurityPolicy(
                p ->
                    p.policyDirectives(
                        "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline';"
                            + " img-src 'self' data:; connect-src 'self'; object-src 'none';"
                            + " base-uri 'self'; frame-ancestors 'none'")));
    return http.build();
  }
}

package br.com.feiras;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class Db {
  public final JdbcTemplate sql;
  public final ObjectMapper json;

  public Db(JdbcTemplate sql, ObjectMapper json) {
    this.sql = sql;
    this.json = json;
  }

  public static String id() {
    return UUID.randomUUID().toString();
  }

  public static Timestamp now() {
    return Timestamp.from(Instant.now());
  }

  public static ResponseStatusException error(HttpStatus status, String message) {
    return new ResponseStatusException(status, message);
  }

  public Map<String, Object> one(String query, Object... args) {
    var rows = sql.queryForList(query, args);
    if (rows.isEmpty()) throw error(HttpStatus.NOT_FOUND, "Registro não encontrado.");
    return rows.get(0);
  }

  public String encode(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (Exception e) {
      throw new IllegalArgumentException("JSON inválido", e);
    }
  }

  public List<List<Double>> points(String value) {
    try {
      return json.readValue(value, new TypeReference<>() {});
    } catch (Exception e) {
      throw error(HttpStatus.BAD_REQUEST, "Geometria inválida.");
    }
  }

  public void audit(String actor, String action, String target, Object details) {
    sql.update(
        "INSERT INTO audit_log(id,actor,action,target,details,created_at) VALUES(?,?,?,?,?,?)",
        id(),
        actor,
        action,
        target,
        encode(details),
        now());
  }

  public void notifyUser(String user, String message) {
    sql.update(
        "INSERT INTO notification(id,user_id,message,created_at) VALUES(?,?,?,?)",
        id(),
        user,
        message,
        now());
  }

  public void lockFair(String fair) {
    one("SELECT id FROM fair WHERE id=? FOR UPDATE", fair);
  }

  public String fairOf(String booth) {
    return (String)
        one(
                "SELECT p.fair_id FROM booth b JOIN pavilion p ON p.id=b.pavilion_id WHERE b.id=?",
                booth)
            .get("fair_id");
  }

  public static Instant instant(Object value) {
    return value == null
        ? null
        : ((java.time.OffsetDateTime)
                (value instanceof Timestamp
                    ? ((Timestamp) value).toInstant().atOffset(java.time.ZoneOffset.UTC)
                    : value))
            .toInstant();
  }
}

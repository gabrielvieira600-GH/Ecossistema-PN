package br.com.feiras;

import static br.com.feiras.Db.*;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class Seed implements ApplicationRunner {
  final Db db;
  final Auth auth;
  final String email, password;

  public Seed(
      Db db,
      Auth auth,
      @Value("${app.admin-email}") String email,
      @Value("${app.admin-password}") String password) {
    this.db = db;
    this.auth = auth;
    this.email = email;
    this.password = password;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) throws Exception {
    // Serialize seeding on PostgreSQL; this runner is also used with H2 in tests.
    try (var connection = db.sql.getDataSource().getConnection()) {
      if (connection.getMetaData().getDatabaseProductName().equals("PostgreSQL"))
        db.sql.execute("SELECT pg_advisory_xact_lock(78272027)");
    }
    if (db.sql.queryForObject("SELECT COUNT(*) FROM fair", Integer.class) == 0) {
      db.sql.update("INSERT INTO fair(id,name) VALUES('navalshore-2027','Navalshore 2027')");
      db.sql.update("INSERT INTO fair(id,name) VALUES('nn-2027','NN Logística 2027')");
      var root = db.json.readTree(new ClassPathResource("data/maps.json").getInputStream());
      for (var map : root.get("pavilions")) {
        String mapId = map.get("id").asText();
        db.sql.update(
            "INSERT INTO"
                + " pavilion(id,fair_id,name,image_path,clean_path,original_pdf,map_width,map_height)"
                + " VALUES(?,?,?,?,?,?,?,?)",
            mapId,
            map.get("fairId").asText(),
            map.get("name").asText(),
            "/maps/" + mapId + ".png",
            "/maps/" + mapId + "-clean.png",
            "/maps/" + mapId + ".pdf",
            map.get("width").asDouble(),
            map.get("height").asDouble());
        for (var b : map.get("booths")) {
          String geometry = db.encode(b.get("geometry")), label = b.path("label").asText("");
          db.sql.update(
              "INSERT INTO"
                  + " booth(id,pavilion_id,code,area,dimensions,geometry,source_geometry,font_size,label,source_label,status,verified)"
                  + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
              id(),
              mapId,
              b.get("code").asText(),
              b.path("area").isNumber() ? b.get("area").decimalValue() : null,
              b.path("dimensions").asText(""),
              geometry,
              geometry,
              b.path("fontSize").asDouble(6),
              label,
              label,
              label.isBlank() ? "AVAILABLE" : "BLOCKED",
              b.path("verified").asBoolean(false));
        }
      }
    }
    if (db.sql.queryForObject(
            "SELECT COUNT(*) FROM app_user WHERE role='ADMIN' AND enabled=TRUE", Integer.class)
        == 0) {
      if (email.isBlank() || !email.contains("@"))
        throw new IllegalStateException(
            "ADMIN_EMAIL and ADMIN_PASSWORD are required for the first start.");
      db.sql.update(
          "INSERT INTO app_user(id,email,password_hash,company,contact,role,enabled,created_at)"
              + " VALUES(?,?,?,?,?,'ADMIN',TRUE,?)",
          id(),
          email.trim().toLowerCase(Locale.ROOT),
          auth.passwordHash(password),
          "Organização",
          "Administrador",
          now());
    }
  }
}

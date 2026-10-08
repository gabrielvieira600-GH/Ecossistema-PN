package br.com.feiras;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.Test;

class MapSeedTest {
  @Test
  void sourceMapsHaveUniqueIdsValidPolygonsAndNoOverlaps() throws Exception {
    var json = new ObjectMapper();
    var root = json.readTree(getClass().getResourceAsStream("/data/maps.json"));
    assertEquals(4, root.get("pavilions").size());
    for (var p : root.get("pavilions")) {
      Set<String> codes = new HashSet<>();
      List<java.awt.geom.Area> areas = new ArrayList<>();
      List<String> names = new ArrayList<>();
      for (var b : p.get("booths")) {
        assertTrue(codes.add(b.get("code").asText()), "Duplicate code");
        List<List<Double>> points = new ArrayList<>();
        for (var v : b.get("geometry")) {
          double x = v.get(0).asDouble(), y = v.get(1).asDouble();
          assertTrue(x >= 0 && x <= p.get("width").asDouble());
          assertTrue(y >= 0 && y <= p.get("height").asDouble());
          points.add(List.of(x, y));
        }
        var shape = Maps.shape(points);
        assertFalse(shape.isEmpty());
        areas.add(shape);
        names.add(b.get("code").asText());
      }
      for (int i = 0; i < areas.size(); i++)
        for (int j = i + 1; j < areas.size(); j++) {
          var intersection = new java.awt.geom.Area(areas.get(i));
          intersection.intersect(areas.get(j));
          var bounds = intersection.getBounds2D();
          assertTrue(
              intersection.isEmpty() || bounds.getWidth() < .3 || bounds.getHeight() < .3,
              "Overlap in " + p.get("id") + ": " + names.get(i) + " / " + names.get(j));
        }
    }
  }
}

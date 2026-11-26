package com.acme.coldtrace.platform.assetmanagement.domain.model.valueobjects;

import java.util.Map;
import java.util.Set;

/** Interpretation of a physical source; independent of its asset association. */
public record SourceProfile(
    String sourceId,
    String transport,
    int profileVersion,
    int schemaVersion,
    Map<String, String> fields,
    String temperatureUnit,
    String humidityUnit) {
  public SourceProfile {
    if (sourceId == null
        || !sourceId.matches("[A-Za-z0-9_-]{1,64}")
        || !("mqtt".equals(transport) || "http".equals(transport))
        || profileVersion < 1
        || schemaVersion != 1
        || !("C".equals(temperatureUnit) || "F".equals(temperatureUnit))
        || !"%RH".equals(humidityUnit)
        || fields == null
        || !fields.keySet().equals(Set.of("temperature", "humidity", "recordedAt"))
        || fields.values().stream().anyMatch(v -> v == null || v.isBlank() || v.length() > 128)
        || fields.values().stream().distinct().count() != 3) {
      throw new IllegalArgumentException("Invalid source profile");
    }
    fields = Map.copyOf(fields);
  }
}

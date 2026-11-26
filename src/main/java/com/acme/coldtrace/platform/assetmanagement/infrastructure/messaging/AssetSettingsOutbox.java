package com.acme.coldtrace.platform.assetmanagement.infrastructure.messaging;

import com.acme.coldtrace.platform.assetmanagement.application.commandservices.AssetSettingsEvents;
import com.acme.coldtrace.platform.assetmanagement.domain.model.aggregates.AssetSettings;
import com.acme.coldtrace.platform.assetmanagement.domain.repositories.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Temporary Strangler producer in the owning Asset context; no remote call inside the transaction. */
@Component
public class AssetSettingsOutbox implements AssetSettingsEvents {
  private final JdbcTemplate jdbc; private final AssetRepository assets;
  private final AssetSettingsRepository settingsRepository; private final ObjectMapper json=new ObjectMapper();
  public AssetSettingsOutbox(JdbcTemplate jdbc,AssetRepository assets,AssetSettingsRepository settingsRepository) {
    this.jdbc=jdbc;this.assets=assets;this.settingsRepository=settingsRepository;
  }
  public void changed(AssetSettings settings) {
    var targets=settings.getAssetId()==null?assets.findAllByOrganizationId(settings.getOrganizationId()):
        assets.findByIdAndOrganizationId(settings.getAssetId(),settings.getOrganizationId()).stream().toList();
    for(var asset:targets) {
      if(settings.getAssetId()==null && settingsRepository.findByOrganizationIdAndAssetId(settings.getOrganizationId(),asset.getId()).isPresent()) continue;
      jdbc.update("INSERT IGNORE INTO asset_settings_versions(asset_id,settings_version) VALUES (?,0)",asset.getId());
      long version=jdbc.queryForObject("SELECT settings_version FROM asset_settings_versions WHERE asset_id=? FOR UPDATE",Long.class,asset.getId())+1;
      jdbc.update("UPDATE asset_settings_versions SET settings_version=? WHERE asset_id=?",version,asset.getId());
      var payload=new LinkedHashMap<String,Object>();
      payload.put("assetId",asset.getId());payload.put("assetName",asset.getName());payload.put("locationId",asset.getLocationId());
      payload.put("minimumTemperature",settings.getMinimumTemperature());payload.put("maximumTemperature",settings.getMaximumTemperature());
      payload.put("minimumHumidity",settings.getMinimumHumidity());payload.put("maximumHumidity",settings.getMaximumHumidity());
      payload.put("alertThresholdMinutes",settings.getAlertThresholdMinutes());payload.put("settingsVersion",version);
      String event=UUID.randomUUID().toString();
      var envelope=Map.of("eventId",event,"eventType","asset.settings-changed","version",1,"organizationId",settings.getOrganizationId(),
          "occurredAt",Instant.now().toString(),"producer","asset-service","payload",payload);
      try { jdbc.update("INSERT INTO asset_integration_outboxes(event_id,payload,attempts) VALUES (?,?,0)",event,json.writeValueAsString(envelope)); }
      catch(Exception e) {throw new IllegalStateException("Cannot persist settings event",e);}
    }
  }
}

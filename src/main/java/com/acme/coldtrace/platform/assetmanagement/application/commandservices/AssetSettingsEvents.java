package com.acme.coldtrace.platform.assetmanagement.application.commandservices;

import com.acme.coldtrace.platform.assetmanagement.domain.model.aggregates.AssetSettings;

public interface AssetSettingsEvents {
  void changed(AssetSettings settings);
}

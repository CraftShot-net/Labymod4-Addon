package me.timeox2k.core;

import net.labymod.api.addon.AddonConfig;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.configuration.loader.annotation.ConfigName;
import net.labymod.api.configuration.loader.property.ConfigProperty;

@ConfigName("settings")
public class CraftShotConfiguration extends AddonConfig {

  @SwitchSetting
  private final ConfigProperty<Boolean> enabled = new ConfigProperty<>(true);

  @Override
  public ConfigProperty<Boolean> enabled() {
    return this.enabled;
  }

  @SwitchSetting
  private final ConfigProperty<Boolean> openBrowserOnSuccess = new ConfigProperty<>(true);

  public ConfigProperty<Boolean> openBrowserOnSuccess() {
    return this.openBrowserOnSuccess;
  }
}

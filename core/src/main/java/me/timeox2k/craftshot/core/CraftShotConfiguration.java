package me.timeox2k.craftshot.core;

import net.labymod.api.addon.AddonConfig;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.TextFieldWidget.TextFieldSetting;
import net.labymod.api.configuration.loader.annotation.ConfigName;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget.ButtonSetting;
import net.labymod.api.configuration.settings.Setting;
import net.labymod.api.util.MethodOrder;
import me.timeox2k.craftshot.core.api.CraftShotAPIClient;

@ConfigName("settings")
public class CraftShotConfiguration extends AddonConfig {

  @SwitchSetting
  private final ConfigProperty<Boolean> enabled = new ConfigProperty<>(true);

  @SwitchSetting
  private final ConfigProperty<Boolean> labyFriendsSync = new ConfigProperty<>(true);

  @SwitchSetting
  private final ConfigProperty<Boolean> openBrowserOnSuccess = new ConfigProperty<>(true);

  @TextFieldSetting
  private final ConfigProperty<String> domain = new ConfigProperty<>("craftshot.net");

  @Override
  public ConfigProperty<Boolean> enabled() {
    return this.enabled;
  }

  public ConfigProperty<Boolean> openBrowserOnSuccess() {
    return this.openBrowserOnSuccess;
  }

  // Getter für die Domain
  public ConfigProperty<String> getDomain() {
    return this.domain;
  }

  public ConfigProperty<Boolean> labyFriendsSync() {
    return this.labyFriendsSync;
  }

  @MethodOrder(after = "labyFriendsSync")
  @ButtonSetting
  public void purgeFriendshipSync(Setting setting) {
    CraftShotAPIClient.purgeFriendshipSync().thenAccept(success -> {
      if (success) {
        System.out.println("[CraftShot] Successfully purged friendship sync.");
      } else {
        System.err.println("[CraftShot] Failed to purge friendship sync.");
      }
    });
  }
}
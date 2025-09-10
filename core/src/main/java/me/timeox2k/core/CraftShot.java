package me.timeox2k.core;

import me.timeox2k.core.commands.craftshot_command;
import net.labymod.api.addon.LabyAddon;
import net.labymod.api.models.addon.annotation.AddonMain;
import me.timeox2k.core.listener.ScreenshotListener;

@AddonMain
public class CraftShot extends LabyAddon<CraftShotConfiguration> {

  @Override
  protected void enable() {
    this.registerSettingCategory();

    this.registerListener(new ScreenshotListener(this));
    this.registerCommand(new craftshot_command(this));

    this.logger().info("Enabled the Addon");
  }

  @Override
  protected Class<CraftShotConfiguration> configurationClass() {
    return CraftShotConfiguration.class;
  }
}

package me.timeox2k.craftshot.core;

import me.timeox2k.craftshot.core.commands.CraftshotCommand;
import me.timeox2k.craftshot.core.listener.ScreenshotListener;
import net.labymod.api.addon.LabyAddon;
import net.labymod.api.models.addon.annotation.AddonMain;

@AddonMain
public class CraftShotAddon extends LabyAddon<CraftShotConfiguration> {

  @Override
  protected void enable() {
    this.registerSettingCategory();

    this.registerListener(new ScreenshotListener(this));
    this.registerCommand(new CraftshotCommand(this));
  }

  @Override
  protected Class<CraftShotConfiguration> configurationClass() {
    return CraftShotConfiguration.class;
  }
}

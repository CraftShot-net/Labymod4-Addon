package me.timeox2k.craftshot.core;

import me.timeox2k.craftshot.core.commands.CraftshotCommand;
import me.timeox2k.craftshot.core.listener.ScreenshotListener;
import net.labymod.api.addon.LabyAddon;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.models.addon.annotation.AddonMain;

@AddonMain
public class CraftShotAddon extends LabyAddon<CraftShotConfiguration> {

  private static final Component PREFIX = Component.text("[CraftShot]", NamedTextColor.GOLD)
      .append(Component.space());

  @Override
  protected void enable() {
    this.registerSettingCategory();

    this.registerListener(new ScreenshotListener());
    this.registerCommand(new CraftshotCommand(this));
  }

  @Override
  protected Class<CraftShotConfiguration> configurationClass() {
    return CraftShotConfiguration.class;
  }

  public static Component prefix() {
    return PREFIX.copy();
  }
}

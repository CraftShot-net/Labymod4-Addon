package me.timeox2k.craftshot.core.listener;

import me.timeox2k.craftshot.core.CraftShotAddon;
import net.labymod.api.Laby;
import net.labymod.api.client.chat.ChatExecutor;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.event.ClickEvent;
import net.labymod.api.client.component.event.HoverEvent;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.misc.CaptureScreenshotEvent;

public class ScreenshotListener {

  private final CraftShotAddon addon;

  public ScreenshotListener(CraftShotAddon addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onScreenshot(CaptureScreenshotEvent event) {
    if (!this.addon.configuration().enabled().get()) {
      return;
    }

    Component message = Component.translatable("craftshot.screenshot.prefix")
        .color(NamedTextColor.GOLD)
        .append(Component.translatable("craftshot.screenshot.message").color(NamedTextColor.WHITE))
        .append(Component.translatable("craftshot.screenshot.clickHere").color(NamedTextColor.GREEN)
            .clickEvent(
                ClickEvent.runCommand("/craftshot " + event.getDestination().getAbsolutePath()))
            .hoverEvent(HoverEvent.showText(Component.translatable("craftshot.screenshot.hoverText")
                .color(NamedTextColor.YELLOW))));

    ChatExecutor chatExecutor = Laby.labyAPI().minecraft().chatExecutor();
    chatExecutor.displayClientMessage(message);
  }
}
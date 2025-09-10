package me.timeox2k.core.listener;

import net.labymod.api.Laby;
import net.labymod.api.client.Minecraft;
import net.labymod.api.client.chat.ChatExecutor;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.event.ClickEvent;
import net.labymod.api.client.component.event.HoverEvent;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.event.Subscribe;
import me.timeox2k.core.CraftShot;
import net.labymod.api.event.client.misc.CaptureScreenshotEvent;

public class ScreenshotListener {

  private final CraftShot addon;

  public ScreenshotListener(CraftShot addon) {
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
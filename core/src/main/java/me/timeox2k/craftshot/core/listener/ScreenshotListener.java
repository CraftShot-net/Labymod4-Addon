package me.timeox2k.craftshot.core.listener;

import me.timeox2k.craftshot.core.CraftShotAddon;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.event.ClickEvent;
import net.labymod.api.client.component.event.HoverEvent;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.misc.CaptureScreenshotEvent;

public class ScreenshotListener {

  @Subscribe
  public void onScreenshot(CaptureScreenshotEvent event) {
    Laby.labyAPI().minecraft().chatExecutor().displayClientMessage(
        Component.empty()
            .append(CraftShotAddon.prefix())
            .append(Component.translatable(
                "craftshot.screenshot.message",
                Component.translatable("craftshot.screenshot.clickHere", NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.runCommand(
                        "/craftshot " + event.getDestination().getAbsolutePath()
                    ))
                    .hoverEvent(HoverEvent.showText(Component.translatable(
                            "craftshot.screenshot.hoverText",
                            NamedTextColor.YELLOW
                        ))
                    )))
    );
  }
}
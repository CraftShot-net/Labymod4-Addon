package me.timeox2k.craftshot.core.hud.widgets;

import me.timeox2k.craftshot.core.CraftShotAddon;
import me.timeox2k.craftshot.core.manager.AddonAPIProfileStatsManager;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidget;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidgetConfig;
import net.labymod.api.client.gui.hud.hudwidget.text.TextLine;
import net.labymod.api.client.gui.hud.hudwidget.text.TextLine.State;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.resources.ResourceLocation;
import net.labymod.api.util.I18n;

public class UnreadNotificationCountHudWidget extends TextHudWidget<TextHudWidgetConfig> {

  private TextLine textLine;
  private final CraftShotAddon addon;
  private final AddonAPIProfileStatsManager profileStatsManager;

  public UnreadNotificationCountHudWidget(CraftShotAddon addon) {
    super("unreadNotificationCountWidget");
    this.bindCategory(addon.getProfileStatsCategory());
    this.addon = addon;
    this.profileStatsManager = addon.getProfileStatsManager();
  }

  @Override
  public void load(TextHudWidgetConfig config) {
    super.load(config);
    this.textLine = createLine(I18n.translate("craftshot.hudWidget.unreadNotificationCountWidget.ingameDisplay"), "0");

    setIcon(Icon.texture(ResourceLocation.create(addon.getNameSpace(), "textures/widgets/notificationicon.png")));
  }

  @Override
  public void onTick(boolean isEditorContext) {

    String unreadNotificationCount;

    if (profileStatsManager != null) {
      unreadNotificationCount = String.valueOf(profileStatsManager.getUnreadNotificationCount());

      // Update the text line an flush it
      this.textLine.updateAndFlush(unreadNotificationCount);
      this.textLine.setState(State.VISIBLE);
    }
  }

}

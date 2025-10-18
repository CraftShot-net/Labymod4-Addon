package me.timeox2k.craftshot.core;

import me.timeox2k.craftshot.core.commands.CraftshotCommand;
import me.timeox2k.craftshot.core.hud.widgets.FollowerCountHudWidget;
import me.timeox2k.craftshot.core.hud.widgets.UnreadNotificationCountHudWidget;
import me.timeox2k.craftshot.core.listener.ScreenshotListener;
import me.timeox2k.craftshot.core.manager.AddonAPIProfileStatsManager;
import me.timeox2k.craftshot.core.utils.SSLCertManager;
import net.labymod.api.Laby;
import net.labymod.api.addon.LabyAddon;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.gui.hud.binding.category.HudWidgetCategory;
import net.labymod.api.labyconnect.LabyConnectSession;
import net.labymod.api.labyconnect.TokenStorage.Purpose;
import net.labymod.api.labyconnect.TokenStorage.Token;
import net.labymod.api.models.addon.annotation.AddonMain;
import org.jetbrains.annotations.Nullable;

@AddonMain
public class CraftShotAddon extends LabyAddon<CraftShotConfiguration> {

  private static final Component PREFIX = Component.text("[CraftShot]", NamedTextColor.GOLD)
      .append(Component.space());

  private HudWidgetCategory profileStatsCategory;
  private AddonAPIProfileStatsManager profileStatsManager;

  @Override
  protected void enable() {
    this.registerSettingCategory();

    this.registerListener(new ScreenshotListener());
    this.registerCommand(new CraftshotCommand(this));

    this.profileStatsManager = new AddonAPIProfileStatsManager(this);

    labyAPI().hudWidgetRegistry().categoryRegistry().register(
        this.profileStatsCategory = new HudWidgetCategory(this.getNameSpace())
    );

    labyAPI().hudWidgetRegistry().register(new FollowerCountHudWidget(this));
    labyAPI().hudWidgetRegistry().register(new UnreadNotificationCountHudWidget(this));

  }

  public String getNameSpace() {
    return addonInfo().getNamespace();
  }

  public AddonAPIProfileStatsManager getProfileStatsManager() {
    return profileStatsManager;
  }

  public HudWidgetCategory getProfileStatsCategory() {
    return profileStatsCategory;
  }



  @Nullable
  public String getLabyConnectToken() {
    //Credits to RappyTV for providing this code snippet to me
    LabyConnectSession session = Laby.labyAPI().labyConnect().getSession();
    if (session == null) {
      return null;
    }

    Token token = session.tokenStorage().getToken(Purpose.JWT, session.self().getUniqueId());

    if (token == null || token.isExpired()) {
      return null;
    }

    return token.getToken();
  }


  @Override
  protected Class<CraftShotConfiguration> configurationClass() {
    return CraftShotConfiguration.class;
  }

  public static Component prefix() {
    return PREFIX.copy();
  }
}

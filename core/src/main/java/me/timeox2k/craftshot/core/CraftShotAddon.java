package me.timeox2k.craftshot.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.timeox2k.craftshot.core.api.CraftShotAPIClient;
import me.timeox2k.craftshot.core.api.ReverbClient;
import me.timeox2k.craftshot.core.commands.CraftshotCommand;
import me.timeox2k.craftshot.core.commands.CraftshotCommand.MSGSubcommand;
import me.timeox2k.craftshot.core.hud.widgets.FollowerCountHudWidget;
import me.timeox2k.craftshot.core.hud.widgets.UnreadNotificationCountHudWidget;
import me.timeox2k.craftshot.core.listener.ScreenshotListener;
import me.timeox2k.craftshot.core.listener.SocialEventsListener;
import me.timeox2k.craftshot.core.manager.AddonAPIProfileStatsManager;
import me.timeox2k.craftshot.core.utils.SSLCertManager;
import net.labymod.api.Laby;
import net.labymod.api.addon.LabyAddon;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.event.ClickEvent;
import net.labymod.api.client.component.event.HoverEvent;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.gui.hud.binding.category.HudWidgetCategory;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.labyconnect.LabyConnectSession;
import net.labymod.api.labyconnect.TokenStorage.Purpose;
import net.labymod.api.labyconnect.TokenStorage.Token;
import net.labymod.api.models.addon.annotation.AddonMain;
import net.labymod.api.util.concurrent.task.Task;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@AddonMain
public class CraftShotAddon extends LabyAddon<CraftShotConfiguration> {

  private static final Component PREFIX = Component.text("[CraftShot]", NamedTextColor.GOLD)
      .append(Component.space());

  public static CraftShotAddon instance;
  private HudWidgetCategory profileStatsCategory;
  private AddonAPIProfileStatsManager profileStatsManager;
  public static ReverbClient reverbClient;

  private static final AtomicBoolean initInProgress = new AtomicBoolean(false);
  private static volatile String lastKnownToken = null;

  @Override
  protected void enable() {
    instance = this;
    this.registerSettingCategory();

    this.registerListener(new ScreenshotListener());
    this.registerListener(new SocialEventsListener(this));
    this.registerCommand(new CraftshotCommand(this));

    this.profileStatsManager = new AddonAPIProfileStatsManager(this);

    labyAPI().hudWidgetRegistry().categoryRegistry()
        .register(this.profileStatsCategory = new HudWidgetCategory(this.getNameSpace()));

    labyAPI().hudWidgetRegistry().register(new FollowerCountHudWidget(this));
    labyAPI().hudWidgetRegistry().register(new UnreadNotificationCountHudWidget(this));

    SSLCertManager.disableSSLCertificateValidation();

    Task.builder(CraftShotAddon::tryInitReverb).repeat(2, TimeUnit.SECONDS).build().execute();
  }

  public static CraftShotAddon getInstance() {
    return instance;
  }

  public static void handleAccountSwitch() {
    String token = getInstance().getLabyConnectToken();
    if (token == null || token.equals(lastKnownToken)) {
      return;
    }

    if (reverbClient != null) {
      reverbClient.disconnect();
      reverbClient = null;
    }
    CraftShotAPIClient.clearCache();
    CraftShotAPIClient.myDatabaseId = -1;
    initInProgress.set(false);
    tryInitReverb();
  }

  private static void tryInitReverb() {
    String token = getInstance().getLabyConnectToken();
    if (token == null) {
      return;
    }

    if (reverbClient != null) {
      lastKnownToken = token;
      return;
    }

    if (!initInProgress.compareAndSet(false, true)) {
      return;
    }

    CraftShotAPIClient.fetchConversationsFresh().thenAccept(conversations -> {
      initInProgress.set(false);
      lastKnownToken = token;
      if (CraftShotAPIClient.myDatabaseId != -1) {
        connectReverb();
      }
    });
  }

  private static void connectReverb() {
    if (reverbClient != null || CraftShotAPIClient.myDatabaseId == -1) {
      return;
    }

    reverbClient = new ReverbClient(String.valueOf(CraftShotAPIClient.myDatabaseId),
        new ReverbClient.MessageListener() {
          public void onNewMessage(JsonObject data) {
            handleIncomingMessage(data);
          }

          public void onPresenceUpdate(JsonObject data) {
            //empty for now, used in fabric
          }
        });
    reverbClient.connect();
  }

  private static void handleIncomingMessage(JsonObject messageData) {
    try {
      JsonObject msgObj =
          messageData.has("message") ? messageData.getAsJsonObject("message") : messageData;
      long convId = msgObj.get("conversation_id").getAsLong();
      String content =
          msgObj.has("content") && !msgObj.get("content").isJsonNull() ? msgObj.get("content")
              .getAsString() : "";
      String senderName = msgObj.getAsJsonObject("sender").get("username").getAsString();

      String attachmentUrl = null;
      if (msgObj.has("attachments") && !msgObj.get("attachments").isJsonNull()) {
        JsonArray attachments = msgObj.getAsJsonArray("attachments");
        if (!attachments.isEmpty()) {
          String path = attachments.get(0).getAsString();
          attachmentUrl = path.startsWith("http") ? path : "https://craftshot.net/storage/" + path;
        }
      }

      boolean isIngame = Laby.labyAPI().minecraft().isIngame();

      if (isIngame) {
        CraftShotAPIClient.markConversationAsRead(convId);

        final String finalAttachmentUrl = attachmentUrl;

        CraftShotAPIClient.fetchUuid(senderName).thenAccept(uuid -> {
          Component messageComponent = Component.empty().append(CraftShotAddon.prefix());

          if (uuid != null) {
            messageComponent = messageComponent.append(Component.icon(Icon.head(uuid), 8,8))
                .append(Component.space());
          }

          MSGSubcommand.activeConversations.put(senderName.toLowerCase(), convId);

          Component nameComponent = Component.text(senderName, NamedTextColor.AQUA)
              .hoverEvent(HoverEvent.showText(
                  Component.translatable("craftshot.chat.hoverReply", NamedTextColor.YELLOW)))
              .clickEvent(ClickEvent.suggestCommand("/craftshot msg " + senderName + " "));

          messageComponent = messageComponent
              .append(nameComponent)
              .append(Component.text(" » ", NamedTextColor.DARK_GRAY));

          String[] lines = content.split("\n", -1);
          for (int i = 0; i < lines.length; i++) {
            messageComponent = messageComponent.append(Component.text(lines[i], NamedTextColor.WHITE));
            if (i < lines.length - 1) {
              messageComponent = messageComponent.append(Component.newline());
            }
          }

          if (finalAttachmentUrl != null) {
            Icon attachmentIcon = Icon.url(finalAttachmentUrl);

            Component imageComponent = Component.icon(attachmentIcon, 100, 60)
                .clickEvent(ClickEvent.openUrl(finalAttachmentUrl))
                .hoverEvent(HoverEvent.showText(
                    Component.text("Open Attachment", NamedTextColor.GRAY)));

            messageComponent = messageComponent
                .append(Component.newline())
                .append(imageComponent);
          }

          if (Laby.labyAPI().minecraft().chatExecutor() != null) {
            Laby.labyAPI().minecraft().chatExecutor().displayClientMessage(messageComponent);
          }
        });
      }
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
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
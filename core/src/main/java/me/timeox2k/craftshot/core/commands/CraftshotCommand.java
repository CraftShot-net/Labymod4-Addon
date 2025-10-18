package me.timeox2k.craftshot.core.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import me.timeox2k.craftshot.core.CraftShotAddon;
import net.labymod.api.Laby;
import net.labymod.api.client.chat.command.Command;
import net.labymod.api.client.chat.command.SubCommand;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.event.ClickEvent;
import net.labymod.api.client.component.event.HoverEvent;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.network.server.ServerData;
import net.labymod.api.util.io.web.request.FormData;
import net.labymod.api.util.io.web.request.Request;
import net.labymod.api.util.io.web.request.Request.Method;
import org.jetbrains.annotations.Nullable;

public class CraftshotCommand extends Command {

  private static final String API_URL = "https://craftshot.net/v1/upload";
  private final CraftShotAddon addon;

  public CraftshotCommand(CraftShotAddon addon) {
    super("craftshot");
    this.addon = addon;

    this.translationKey("craftshot.command");
    this.withSubCommand(new CopySubcommand());
  }

  @Override
  public boolean execute(String prefix, String[] arguments) {
    if (arguments.length == 0) {
      this.displayMessage(Component.empty().append(CraftShotAddon.prefix()).append(
          Component.translatable(this.getTranslationKey("missingArgument"), NamedTextColor.RED)));
      return true;
    }

    File uploadFile = new File(arguments.length == 1 ? arguments[0] : String.join(" ", arguments));
    if (!uploadFile.exists()) {
      this.displayMessage(Component.empty().append(CraftShotAddon.prefix()).append(
          Component.translatable(this.getTranslationKey("invalidFile"), NamedTextColor.RED)));
      return true;
    }


    String labyConnectToken = addon.getLabyConnectToken();
    if (labyConnectToken == null) {
      this.displayMessage(Component.empty().append(CraftShotAddon.prefix()).append(
          Component.translatable(this.getTranslationKey("invalidSession"), NamedTextColor.RED)));
      return true;
    }

    this.displayMessage(Component.empty().append(CraftShotAddon.prefix())
        .append(Component.translatable(this.getTranslationKey("uploading"), NamedTextColor.GRAY)));

    List<FormData> formData = new ArrayList<>();
    formData.add(FormData.builder().name("access_token").value(labyConnectToken).build());

    String host = this.getHost();
    if (host != null) {
      formData.add(FormData.builder().name("server_ip").value(host).build());
    }

    try {
      formData.add(
          FormData.builder().name("screenshot").fileName("screenshot.png").contentType("image/png")
              .value(uploadFile.toPath()).build());
    } catch (IOException e) {
      this.displayMessage(Component.empty().append(CraftShotAddon.prefix())
          .append(Component.translatable(this.getTranslationKey("failed"), NamedTextColor.RED)));
      this.addon.logger().error("Failed to upload screenshot", e);
      return true;
    }

    Request.ofGson(JsonElement.class).url(API_URL).method(Method.POST).form(formData)
        .handleErrorStream().async().execute((response) -> {
          if (response.hasException()) {
            this.displayMessage(Component.empty().append(CraftShotAddon.prefix())
                .append(Component.translatable(this.getTranslationKey("failed"), NamedTextColor.RED)));
            this.addon.logger().error("Failed to upload screenshot", response.exception());
            return;
          }

          if (response.getStatusCode() != 200) {
            this.displayMessage(Component.empty().append(CraftShotAddon.prefix())
                .append(Component.translatable(this.getTranslationKey("failed"), NamedTextColor.RED)));
            this.addon.logger()
                .error("Http request failed with status code " + response.getStatusCode(),
                    response.get());
            return;
          }

          try {
            JsonObject body = response.get().getAsJsonObject();
            String url = body.get("data").getAsJsonObject().get("url").getAsString();

            this.displayMessage(CraftShotAddon.prefix()
                .append(Component.translatable(this.getTranslationKey("success"), NamedTextColor.GREEN))
                .append(Component.space()).append(
                    Component.translatable(this.getTranslationKey("copyUrl"), NamedTextColor.GOLD)
                        .hoverEvent(HoverEvent.showText(
                            Component.translatable(this.getTranslationKey("hoverCopyUrl"),
                                NamedTextColor.GRAY)))
                        .clickEvent(ClickEvent.runCommand("/craftshot copy " + url))));

            if (this.addon.configuration().openBrowserOnSuccess().get()) {
              Laby.references().chatExecutor().openUrl(url + "?auth=" + labyConnectToken);
            }
          } catch (Exception e) {
            this.displayMessage(Component.empty().append(CraftShotAddon.prefix())
                .append(Component.translatable(this.getTranslationKey("failed"), NamedTextColor.RED)));
            this.addon.logger().error("Failed to parse response", e);
          }
        });
    return true;
  }



  @Nullable
  private String getHost() {
    ServerData server = Laby.labyAPI().serverController().getCurrentServerData();
    if (server == null || server.address().getHost().isEmpty()) {
      return null;
    }
    return server.address().getHost();
  }

  private static class CopySubcommand extends SubCommand {

    protected CopySubcommand() {
      super("copy");

      this.translationKey("craftshot.command");
    }

    @Override
    public boolean execute(String prefix, String[] arguments) {
      if (arguments.length == 0) {
        this.displayMessage(Component.empty().append(CraftShotAddon.prefix()).append(
            Component.translatable(this.getTranslationKey("missingArgument"), NamedTextColor.RED)));
        return true;
      }

      Laby.labyAPI().minecraft().setClipboard(arguments[0]);
      this.displayMessage(Component.empty().append(CraftShotAddon.prefix()).append(
          Component.translatable(this.getTranslationKey("urlCopied"), NamedTextColor.GREEN)));
      return true;
    }
  }
}

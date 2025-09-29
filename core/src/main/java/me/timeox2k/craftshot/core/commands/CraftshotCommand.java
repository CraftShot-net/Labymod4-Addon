package me.timeox2k.craftshot.core.commands;

import java.io.File;
import java.util.Objects;
import me.timeox2k.craftshot.core.CraftShotAddon;
import me.timeox2k.craftshot.core.utils.UploadManager;
import net.labymod.api.Laby;
import net.labymod.api.client.Minecraft;
import net.labymod.api.client.chat.ChatExecutor;
import net.labymod.api.client.chat.command.Command;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.event.ClickEvent;
import net.labymod.api.client.component.event.HoverEvent;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.session.SessionAccessor;

public class CraftshotCommand extends Command {

  private final CraftShotAddon addon;

  public CraftshotCommand(CraftShotAddon addon) {
    super("craftshot", "craftshot_upload");
    this.addon = addon;
  }

  @Override
  public boolean execute(String prefix, String[] arguments) {

    if (!this.addon.configuration().enabled().get()) {
      return true;
    }

    if (arguments.length == 0) {
      return true;
    }

    // Handle copy command
    if (arguments.length >= 2 && "copy".equals(arguments[0])) {
      String url = arguments[1];
      this.copyToClipboard(url);
      this.showCopySuccessMessage();
      return true;
    }

    String filePath;
    if (arguments.length == 1) {
      filePath = arguments[0];
    } else {
      filePath = String.join(" ", arguments);
    }
    File uploadFile = new File(filePath);

    if (!uploadFile.exists()) {
      this.showErrorMessage("File does not exist");
      return true;
    }

    final Minecraft minecraft = Laby.labyAPI().minecraft();
    final SessionAccessor sessionAccessor = minecraft.sessionAccessor();
    final String accessToken = Objects.requireNonNull(sessionAccessor.getSession())
        .getAccessToken();

    this.showUploadingMessage();

    Thread uploadThread = new Thread(() -> {
      try {
        UploadManager uploader = new UploadManager(this.addon);
        UploadManager.UploadResult result = uploader.uploadScreenshot(accessToken, uploadFile);

        Laby.labyAPI().minecraft().executeOnRenderThread(() -> {
          if (result.success()) {
            if (this.addon.configuration().openBrowserOnSuccess().get()) {
              Laby.references().chatExecutor().openUrl(result.url() + "?auth=" + accessToken);
            }
            this.showSuccessMessage(result);
          } else {
            this.showErrorMessage(result.message());
          }
        });
      } catch (Exception e) {
        this.addon.logger().error("Upload thread error: " + e.getMessage());

        Laby.labyAPI().minecraft().executeOnRenderThread(() -> {
          this.showErrorMessage("Unexpected upload error: " + e.getMessage());
        });
      }
    });

    uploadThread.setName("CraftShot-Upload");
    uploadThread.start();

    return true;
  }

  private void showSuccessMessage(UploadManager.UploadResult result) {
    Component message = Component.translatable("craftshot.upload.prefix").color(NamedTextColor.GOLD)
        .append(Component.translatable("craftshot.upload.success").color(NamedTextColor.GREEN))
        .append(Component.translatable("craftshot.upload.copyUrl").color(NamedTextColor.GOLD)
            .clickEvent(ClickEvent.runCommand("/craftshot copy " + result.url())).hoverEvent(
                HoverEvent.showText(Component.translatable("craftshot.upload.hoverCopyUrl")
                    .color(NamedTextColor.GRAY))));

    this.showMessage(message);
  }

  private void showErrorMessage(String error) {
    Component message = Component.translatable("craftshot.upload.prefix").color(NamedTextColor.GOLD)
        .append(Component.translatable("craftshot.upload.error", Component.text(error))
            .color(NamedTextColor.RED));

    this.showMessage(message);
  }

  private void showUploadingMessage() {
    Component message = Component.translatable("craftshot.upload.prefix").color(NamedTextColor.GOLD)
        .append(Component.translatable("craftshot.upload.uploading").color(NamedTextColor.GOLD));

    this.showMessage(message);
  }

  private void showCopySuccessMessage() {
    Component message = Component.translatable("craftshot.upload.prefix").color(NamedTextColor.GOLD)
        .append(Component.translatable("craftshot.upload.urlCopied").color(NamedTextColor.GREEN));

    this.showMessage(message);
  }

  private void showMessage(Component message) {
    ChatExecutor chatExecutor = Laby.labyAPI().minecraft().chatExecutor();
    chatExecutor.displayClientMessage(message);
  }

  private void copyToClipboard(String text) {
    try {
      Laby.labyAPI().minecraft().setClipboard(text);
    } catch (Exception e) {
      this.addon.logger().error("Failed to copy to clipboard: " + e.getMessage());
    }
  }
}

package me.timeox2k.core.commands;

import me.timeox2k.core.CraftShot;
import me.timeox2k.core.utils.UploadManager;
import net.labymod.api.Laby;
import net.labymod.api.client.Minecraft;
import net.labymod.api.client.chat.ChatExecutor;
import net.labymod.api.client.chat.command.Command;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.event.ClickEvent;
import net.labymod.api.client.component.event.HoverEvent;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.session.SessionAccessor;
import java.io.File;
import java.util.Objects;

public class craftshot_command extends Command {

  private final CraftShot addon;

  public craftshot_command(CraftShot addon) {
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
      copyToClipboard(url);
      showCopySuccessMessage();
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
      showErrorMessage("File does not exist");
      return true;
    }

    final Minecraft minecraft = Laby.labyAPI().minecraft();
    final SessionAccessor sessionAccessor = minecraft.sessionAccessor();
    final String accessToken = Objects.requireNonNull(sessionAccessor.getSession())
        .getAccessToken();

    showUploadingMessage();

    Thread uploadThread = new Thread(() -> {
      try {
        UploadManager uploader = new UploadManager(addon);
        UploadManager.UploadResult result = uploader.uploadScreenshot(accessToken, uploadFile);

        Laby.labyAPI().minecraft().executeOnRenderThread(() -> {
          if (result.isSuccess()) {
            if (this.addon.configuration().openBrowserOnSuccess().get()) {
              Laby.references().chatExecutor().openUrl(result.getUrl() + "?auth=" + accessToken);
            }
            showSuccessMessage(result);
          } else {
            showErrorMessage(result.getMessage());
          }
        });
      } catch (Exception e) {
        addon.logger().error("Upload thread error: " + e.getMessage());

        Laby.labyAPI().minecraft().executeOnRenderThread(() -> {
          showErrorMessage("Unexpected upload error: " + e.getMessage());
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
            .clickEvent(ClickEvent.runCommand("/craftshot copy " + result.getUrl())).hoverEvent(
                HoverEvent.showText(Component.translatable("craftshot.upload.hoverCopyUrl")
                    .color(NamedTextColor.GRAY))));

    showMessage(message);
  }

  private void showErrorMessage(String error) {
    Component message = Component.translatable("craftshot.upload.prefix").color(NamedTextColor.GOLD)
        .append(Component.translatable("craftshot.upload.error", Component.text(error))
            .color(NamedTextColor.RED));

    showMessage(message);
  }

  private void showUploadingMessage() {
    Component message = Component.translatable("craftshot.upload.prefix").color(NamedTextColor.GOLD)
        .append(Component.translatable("craftshot.upload.uploading").color(NamedTextColor.GOLD));

    showMessage(message);
  }

  private void showCopySuccessMessage() {
    Component message = Component.translatable("craftshot.upload.prefix").color(NamedTextColor.GOLD)
        .append(Component.translatable("craftshot.upload.urlCopied").color(NamedTextColor.GREEN));

    showMessage(message);
  }

  private void showMessage(Component message) {
    ChatExecutor chatExecutor = Laby.labyAPI().minecraft().chatExecutor();
    chatExecutor.displayClientMessage(message);
  }

  private void copyToClipboard(String text) {
    try {
      Laby.labyAPI().minecraft().setClipboard(text);
    } catch (Exception e) {
      addon.logger().error("Failed to copy to clipboard: " + e.getMessage());
    }
  }
}

package me.timeox2k.core.utils;

import me.timeox2k.core.CraftShot;
import net.labymod.api.Laby;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class UploadManager {

  private static final String API_URL = "https://craftshot.net/v1/upload";
  private final CraftShot addon;

  public UploadManager(CraftShot addon) {
    this.addon = addon;
  }

  public UploadResult uploadScreenshot(String accessToken, File screenshotFile) {
    try {
      String boundary = "----CraftShot" + System.currentTimeMillis();

      HttpURLConnection connection = (HttpURLConnection) new URL(API_URL).openConnection();
      connection.setRequestMethod("POST");
      connection.setDoOutput(true);
      connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

      try (OutputStream out = connection.getOutputStream(); PrintWriter writer = new PrintWriter(
          new OutputStreamWriter(out, StandardCharsets.UTF_8), true)) {

        addFormField(writer, boundary, "access_token", accessToken);
        try {
          var serverData = Laby.labyAPI().serverController().getCurrentServerData();
          if (serverData != null && serverData.address() != null
              && serverData.address().getHost() != null) {
            String serverIp = serverData.address().getHost();
            if (!serverIp.isEmpty()) {
              addFormField(writer, boundary, "server_ip", serverIp);
            }
          }
        } catch (Exception e) {
          addon.logger().debug("Server IP not available: " + e.getMessage());
        }

        // Add file
        addFileField(writer, out, boundary, screenshotFile);

        // End
        writer.append("--").append(boundary).append("--").append("\r\n");
        writer.flush();
      }

      int responseCode = connection.getResponseCode();
      String responseBody = readResponse(connection);

      if (responseCode == 200) {
        return parseSuccessResponse(responseBody);
      } else {
        return new UploadResult(false, "HTTP " + responseCode, null);
      }

    } catch (Exception e) {
      addon.logger().error("Upload error: " + e.getMessage());
      return new UploadResult(false, e.getMessage(), null);
    }
  }

  private String readResponse(HttpURLConnection connection) throws IOException {
    StringBuilder response = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
        connection.getResponseCode() >= 400 ? connection.getErrorStream()
            : connection.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        response.append(line);
      }
    }
    return response.toString();
  }

  private UploadResult parseSuccessResponse(String responseBody) {
    try {
      // Simple JSON parsing for the expected response
      String message = extractJsonValue(responseBody, "message");
      String url = extractNestedJsonValue(responseBody, "data", "url");

      return new UploadResult(true, message, url);
    } catch (Exception e) {
      return new UploadResult(false, "Failed to parse response", null);
    }
  }

  private String extractJsonValue(String json, String key) {
    try {
      String searchPattern = "\"" + key + "\"";
      int keyIndex = json.indexOf(searchPattern);
      if (keyIndex == -1) {
        return "";
      }

      int colonIndex = json.indexOf(":", keyIndex + searchPattern.length());
      if (colonIndex == -1) {
        return "";
      }

      int startIndex = json.indexOf("\"", colonIndex);
      if (startIndex == -1) {
        return "";
      }

      int endIndex = json.indexOf("\"", startIndex + 1);
      if (endIndex == -1) {
        return "";
      }

      String value = json.substring(startIndex + 1, endIndex);

      // Unescape JSON escaped characters
      value = value.replace("\\/", "/");
      value = value.replace("\\\"", "\"");
      value = value.replace("\\\\", "\\");

      return value;
    } catch (Exception e) {
      return "";
    }
  }

  private String extractNestedJsonValue(String json, String parentKey, String childKey) {
    try {
      String parentPattern = "\"" + parentKey + "\"";
      int parentIndex = json.indexOf(parentPattern);
      if (parentIndex == -1) {
        return "";
      }

      int openBrace = json.indexOf("{", parentIndex);
      if (openBrace == -1) {
        return "";
      }

      int closeBrace = findMatchingBrace(json, openBrace);
      if (closeBrace == -1) {
        return "";
      }

      String dataSection = json.substring(openBrace, closeBrace + 1);
      return extractJsonValue(dataSection, childKey);
    } catch (Exception e) {
      return "";
    }
  }

  private int findMatchingBrace(String json, int openBraceIndex) {
    int braceCount = 1;
    for (int i = openBraceIndex + 1; i < json.length(); i++) {
      if (json.charAt(i) == '{') {
        braceCount++;
      }
      if (json.charAt(i) == '}') {
        braceCount--;
      }
      if (braceCount == 0) {
        return i;
      }
    }
    return -1;
  }


  private void addFormField(PrintWriter writer, String boundary, String name, String value) {
    writer.append("--").append(boundary).append("\r\n");
    writer.append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n");
    writer.append("\r\n");
    writer.append(value).append("\r\n");
  }

  private void addFileField(PrintWriter writer, OutputStream output, String boundary, File file)
      throws IOException {
    writer.append("--").append(boundary).append("\r\n");
    writer.append("Content-Disposition: form-data; name=\"screenshot\"; filename=\"")
        .append(file.getName()).append("\"\r\n");
    writer.append("Content-Type: image/png\r\n");
    writer.append("\r\n");
    writer.flush();

    Files.copy(file.toPath(), output);
    output.flush();

    writer.append("\r\n");
  }

  public static class UploadResult {

    private final boolean success;
    private final String message;
    private final String url;

    public UploadResult(boolean success, String message, String url) {
      this.success = success;
      this.message = message;
      this.url = url;
    }

    public boolean isSuccess() {
      return success;
    }

    public String getMessage() {
      return message;
    }

    public String getUrl() {
      return url;
    }
  }
}

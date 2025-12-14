package me.timeox2k.craftshot.core.manager;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.timeox2k.craftshot.core.CraftShotAddon;
import me.timeox2k.craftshot.core.interfaces.ResponseCallBack;
import net.labymod.api.util.concurrent.task.Task;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class AddonAPIProfileStatsManager {

  private final CraftShotAddon addon;

  private int followerCount = 0;
  private int unreadNotificationCount = 0;

  public AddonAPIProfileStatsManager(CraftShotAddon addon) {
    this.addon = addon;

    Task.builder(() -> this.getCraftShotAddonAPIStats(new ResponseCallBack() {
      @Override
      public void onSuccess(JsonObject response) {
        followerCount = response.get("follower_count").getAsInt();
        unreadNotificationCount = response.get("unread_notifications_count").getAsInt();
      }

      @Override
      public void onFailure(int errorCode, String responseBody) {
        System.out.println("API-Error: " + errorCode + " - " + responseBody);
      }
    })).repeat(10, TimeUnit.SECONDS).build().execute();
  }

  public int getFollowerCount() {
    return followerCount;
  }

  public int getUnreadNotificationCount() {
    return unreadNotificationCount;
  }

  private void getCraftShotAddonAPIStats(ResponseCallBack callback) {
    if (addon.getLabyConnectToken() == null) {
      callback.onFailure(-1, "No LabyConnect Token found!");
      return;
    }

    try {
      String apiUrl =
          "https://craftshot.net/api/v1/get-labymod-addon-stats/" + addon.getLabyConnectToken();
      URL url = new URL(apiUrl);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();

      connection.setRequestMethod("GET");
      connection.setRequestProperty("Accept", "application/json");

      int responseCode = connection.getResponseCode();
      if (responseCode == 200) {
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
          response.append(inputLine);
        }
        in.close();

        JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
        callback.onSuccess(jsonResponse);
      } else {
        // Read error response body
        BufferedReader errorReader = new BufferedReader(
            new InputStreamReader(connection.getErrorStream()));
        StringBuilder errorResponse = new StringBuilder();
        String errorLine;

        while ((errorLine = errorReader.readLine()) != null) {
          errorResponse.append(errorLine);
        }
        errorReader.close();

        callback.onFailure(responseCode, errorResponse.toString());
      }
    } catch (Exception e) {
      e.printStackTrace();
      callback.onFailure(-1, e.getMessage());
    }
  }

}

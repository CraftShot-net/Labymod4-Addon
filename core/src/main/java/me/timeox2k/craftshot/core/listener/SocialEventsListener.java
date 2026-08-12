package me.timeox2k.craftshot.core.listener;

import com.google.gson.JsonObject;
import me.timeox2k.craftshot.core.CraftShotAddon;
import me.timeox2k.craftshot.core.utils.LabyUtils;
import net.labymod.api.client.network.server.ServerData;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.lifecycle.ShutdownEvent;
import net.labymod.api.event.client.network.server.ServerDisconnectEvent;
import net.labymod.api.event.client.network.server.ServerJoinEvent;
import net.labymod.api.event.client.network.server.ServerSwitchEvent;
import net.labymod.api.event.client.session.SessionUpdateEvent;
import net.labymod.api.event.labymod.labyconnect.session.friend.LabyConnectFriendAddEvent;
import net.labymod.api.event.labymod.labyconnect.session.friend.LabyConnectFriendRemoveEvent;
import net.labymod.api.util.concurrent.task.Task;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class SocialEventsListener {

  private final CraftShotAddon addon;
  private String currentServerIp = null;

  public SocialEventsListener(CraftShotAddon addon) {
    this.addon = addon;

    Task.builder(() -> {
      sendApiRequest("/v1/client/heartbeat", null);
      if (currentServerIp != null) {
        sendApiRequest("/v1/server/heartbeat", currentServerIp);
      }
    }).repeat(15, TimeUnit.SECONDS).build().execute();
  }

  @Subscribe
  public void onSessionUpdateEvent(SessionUpdateEvent event) {
    CraftShotAddon.handleAccountSwitch();
  }

  @Subscribe
  public void onServerJoinEvent(ServerJoinEvent event) {
    handleServerData(event.serverData());
  }

  @Subscribe
  public void onServerSwitchEvent(ServerSwitchEvent event) {
    handleServerData(event.newServerData());
  }

  @Subscribe
  public void onServerDisconnectEvent(ServerDisconnectEvent event) {
    if (currentServerIp != null) {
      sendApiRequest("/v1/server/leave", currentServerIp);
      currentServerIp = null;
    }
  }

  @Subscribe
  public void onShutdown(ShutdownEvent event) {
    sendApiRequest("/v1/client/offline", null);
    if (currentServerIp != null) {
      sendApiRequest("/v1/server/leave", currentServerIp);
      currentServerIp = null;
    }
  }

  private void handleServerData(ServerData serverData) {
    if (serverData != null && serverData.address() != null) {
      String ip = serverData.address().getHost();
      int port = serverData.address().getPort();

      this.currentServerIp = ip + ":" + port;

      sendApiRequest("/v1/server/join", currentServerIp);
    } else {
      this.currentServerIp = "Singleplayer";
      sendApiRequest("/v1/server/join", currentServerIp);
    }
  }

  @Subscribe
  public void LabyConnectFriendAddEvent(LabyConnectFriendAddEvent event) {
    System.out.println("Status changed!");
    LabyUtils.fetchLabyFriends();
  }

  @Subscribe
  public void LabyConnectFriendRemoveEvent(LabyConnectFriendRemoveEvent event) {
    System.out.println("Status changed!");
    LabyUtils.fetchLabyFriends();
  }

  private void sendApiRequest(String endpoint, String serverIp) {
    String token = addon.getLabyConnectToken();

    if (token == null) {
      return;
    }

    Task.builder(() -> {
      try {
        String domain = addon.configuration().getDomain().get();
        String fullUrl = "https://" + domain + endpoint;

        URL url = new URL(fullUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);

        JsonObject json = new JsonObject();
        json.addProperty("access_token", token);
        if (serverIp != null) {
          json.addProperty("server_ip", serverIp);
        }

        try (OutputStream os = connection.getOutputStream()) {
          byte[] input = json.toString().getBytes("utf-8");
          os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();

        InputStream stream = (responseCode >= 200 && responseCode < 300)
            ? connection.getInputStream()
            : connection.getErrorStream();

        if (stream != null) {
          BufferedReader reader = new BufferedReader(new InputStreamReader(stream,
              StandardCharsets.UTF_8));
          StringBuilder responseBody = new StringBuilder();
          String line;
          while ((line = reader.readLine()) != null) {
            responseBody.append(line);
          }
          reader.close();
        }

      } catch (Exception e) {
        e.printStackTrace();
      }
    }).build().execute();
  }
}
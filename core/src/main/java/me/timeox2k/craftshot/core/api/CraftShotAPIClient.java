package me.timeox2k.craftshot.core.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.timeox2k.craftshot.core.CraftShotAddon;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CraftShotAPIClient {

  private static final String BASE_URL = "https://craftshot.net/api/v2/messages";
  private static final String UUID_BASE_URL = "https://craftshot.net/api/minecraft-uuid/";
  private static final HttpClient CLIENT = createHttpClient();
  private static final Gson GSON = new Gson();

  public static long myDatabaseId = -1;

  private record CachedConversations(List<ConversationDTO> conversations, long cachedAtMillis) {

  }

  public static void clearCache() {
    CONVERSATION_CACHE = null;
  }

  private static volatile CachedConversations CONVERSATION_CACHE;

  private static HttpClient createHttpClient() {
    HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10));
    return builder.build();
  }

  private static String getSessionToken() {
    return CraftShotAddon.getInstance().getLabyConnectToken();
  }

  public record ConversationDTO(long id, long otherUserId, String name, boolean isOnline,
                                String serverIp) {

  }

  public record MessageDTO(long id, String sender, String content, String attachmentUrl) {

  }

  public static CompletableFuture<List<ConversationDTO>> fetchConversationsFresh() {
    return CompletableFuture.supplyAsync(() -> {
      try {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE_URL))
            .header("Authorization", "Bearer " + getSessionToken())
            .header("Accept", "application/json").GET().build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
          JsonObject json = GSON.fromJson(response.body(), JsonObject.class);

          if (json.has("my_id") && !json.get("my_id").isJsonNull()) {
            myDatabaseId = json.get("my_id").getAsLong();
          }
          JsonArray convosArray = json.getAsJsonArray("conversations");
          List<ConversationDTO> list = getConversationDTOS(convosArray);
          CONVERSATION_CACHE = new CachedConversations(List.copyOf(list),
              System.currentTimeMillis());
          return list;
        } else {
          System.err.println("API Error fetchConversations - Status: " + response.statusCode());
          System.err.println("API Response: " + response.body());
        }
      } catch (Exception e) {
        System.err.println("Error fetching conversations: " + e.getMessage());
      }
      return Collections.emptyList();
    });
  }

  public static void markConversationAsRead(long conversationId) {
    String token = CraftShotAddon.getInstance().getLabyConnectToken();

    String url = "https://craftshot.net/api/v2/messages/" + conversationId + "/read";

    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
        .header("Authorization", "Bearer " + token).header("Accept", "application/json")
        .POST(HttpRequest.BodyPublishers.noBody()) // Leerer Body
        .build();

    HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
        .thenApply(response -> {
          if (response.statusCode() == 200 || response.statusCode() == 204) {
            return true;
          } else {
            System.err.println(
                "[API Error] Failed to mark chat as read - Status: " + response.statusCode());
            return false;
          }
        }).exceptionally(e -> {
          System.err.println("[API Error] Exception while marking chat as read: " + e.getMessage());
          return false;
        });
  }

  public record UserLookupDTO(UUID uuid, long userId) {}

  public static CompletableFuture<UserLookupDTO> fetchUserLookup(String username) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(UUID_BASE_URL + username))
            .header("Accept", "application/json").GET().build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
          JsonObject json = GSON.fromJson(response.body(), JsonObject.class);
          if (json.has("id") && json.has("user_id")
              && !json.get("id").isJsonNull() && !json.get("user_id").isJsonNull()) {
            UUID uuid = parseUuid(json.get("id").getAsString());
            long userId = json.get("user_id").getAsLong();
            return new UserLookupDTO(uuid, userId);
          }
        }
      } catch (Exception e) {
        System.err.println("Error fetching user lookup for " + username + ": " + e.getMessage());
      }
      return null;
    });
  }

  public record ConversationResult(Long conversationId, String error) {

    public static ConversationResult success(long id) {
      return new ConversationResult(id, null);
    }

    public static ConversationResult failure(String error) {
      return new ConversationResult(null, error);
    }
  }

  public static CompletableFuture<ConversationResult> getOrCreateConversation(String username) {
    CachedConversations cached = CONVERSATION_CACHE;
    if (cached != null) {
      for (ConversationDTO convo : cached.conversations()) {
        if (convo.name().equalsIgnoreCase(username)) {
          return CompletableFuture.completedFuture(ConversationResult.success(convo.id()));
        }
      }
    }

    return fetchUserLookup(username).thenCompose(lookup -> {
      if (lookup == null) {
        return CompletableFuture.completedFuture(ConversationResult.failure("User not found"));
      }

      return CompletableFuture.supplyAsync(() -> {
        try {
          JsonObject payload = new JsonObject();
          JsonArray userIds = new JsonArray();
          userIds.add(lookup.userId());
          payload.add("user_ids", userIds);

          HttpRequest request = HttpRequest.newBuilder()
              .uri(URI.create(BASE_URL + "/conversations/init"))
              .header("Authorization", "Bearer " + getSessionToken())
              .header("Content-Type", "application/json")
              .header("Accept", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
              .build();

          HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

          if (response.statusCode() == 200) {
            JsonObject json = GSON.fromJson(response.body(), JsonObject.class);
            if (json.has("redirect") && !json.get("redirect").isJsonNull()) {
              String redirect = json.get("redirect").getAsString(); // "/messages/{id}"
              String idPart = redirect.substring(redirect.lastIndexOf('/') + 1);
              return ConversationResult.success(Long.parseLong(idPart));
            }
            return ConversationResult.failure("Unexpected response format");
          }

          if (response.statusCode() == 403) {
            return ConversationResult.failure("Not mutuals");
          }

          try {
            JsonObject json = GSON.fromJson(response.body(), JsonObject.class);
            if (json.has("error") && !json.get("error").isJsonNull()) {
              return ConversationResult.failure(json.get("error").getAsString());
            }
          } catch (Exception ignored) {
          }

          return ConversationResult.failure("HTTP " + response.statusCode());
        } catch (Exception e) {
          System.err.println("Error creating conversation with " + username + ": " + e.getMessage());
          return ConversationResult.failure(e.getMessage());
        }
      });
    });
  }

  private static List<ConversationDTO> getConversationDTOS(JsonArray convosArray) {
    List<ConversationDTO> list = new ArrayList<>();

    for (JsonElement elem : convosArray) {

      if (!elem.isJsonObject()) {
        System.err.println("[API Error] Found non-object element in conversations array: " + elem);
        continue;
      }

      JsonObject convObj = elem.getAsJsonObject();
      long id = convObj.get("id").getAsLong();

      String name = "Unknown";
      boolean isOnline = false;
      String serverIp = null;
      long otherUserId = -1;

      if (convObj.has("other_users")) {
        JsonArray otherUsers = convObj.getAsJsonArray("other_users");
        if (!otherUsers.isEmpty()) {
          JsonObject otherUser = otherUsers.get(0).getAsJsonObject();

          if (otherUser.has("id") && !otherUser.get("id").isJsonNull()) {
            otherUserId = otherUser.get("id").getAsLong(); // neu
          }

          if (otherUser.has("username") && !otherUser.get("username").isJsonNull()) {
            name = otherUser.get("username").getAsString();
          }

          if (otherUser.has("is_online") && !otherUser.get("is_online").isJsonNull()) {
            isOnline = otherUser.get("is_online").getAsBoolean();
          }
          if (otherUser.has("current_server") && !otherUser.get("current_server").isJsonNull()) {
            serverIp = otherUser.get("current_server").getAsString();
          }
        }
      }

      // If the conversation has a specific name, it overrides the user's name
      if (convObj.has("name") && !convObj.get("name").isJsonNull()) {
        name = convObj.get("name").getAsString();
      }

      list.add(new ConversationDTO(id, otherUserId, name, isOnline, serverIp));
    }
    return list;
  }

  /**
   * Returns null on success, or the server error message string on failure.
   */
  public static CompletableFuture<String> sendMessage(long conversationId, String content) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        JsonObject payload = new JsonObject();
        payload.addProperty("content", content);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/" + conversationId))
            .header("Authorization", "Bearer " + getSessionToken())
            .header("Content-Type", "application/json").header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload))).build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
          return null; // success
        }

        // Try to extract the "error" field from the JSON response
        try {
          JsonObject json = GSON.fromJson(response.body(), JsonObject.class);
          if (json.has("error") && !json.get("error").isJsonNull()) {
            return json.get("error").getAsString();
          }
        } catch (Exception ignored) {
        }

        return "HTTP " + response.statusCode();

      } catch (Exception e) {
        System.err.println("Error sending message: " + e.getMessage());
        return e.getMessage();
      }
    });
  }

  public static CompletableFuture<UUID> fetchUuid(String username) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        String url = UUID_BASE_URL + username;

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
            .header("Accept", "application/json").GET().build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
          JsonObject json = GSON.fromJson(response.body(), JsonObject.class);

          if (json.has("id") && !json.get("id").isJsonNull()) {
            String idStr = json.get("id").getAsString();
            return parseUuid(idStr);
          }
        }
      } catch (Exception e) {
        System.err.println("Error fetching UUID for " + username + ": " + e.getMessage());
      }
      return null;
    });
  }

  private static UUID parseUuid(String uuidString) {
    if (uuidString.contains("-")) {
      return UUID.fromString(uuidString);
    }
    if (uuidString.length() == 32) {
      String formatted = uuidString.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
          "$1-$2-$3-$4-$5");
      return UUID.fromString(formatted);
    }
    throw new IllegalArgumentException("Invalid UUID Format: " + uuidString);
  }
}
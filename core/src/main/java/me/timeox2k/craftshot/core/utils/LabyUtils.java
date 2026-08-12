package me.timeox2k.craftshot.core.utils;

import me.timeox2k.craftshot.core.CraftShotAddon;
import me.timeox2k.craftshot.core.api.CraftShotAPIClient;
import net.labymod.api.Laby;
import net.labymod.api.labyconnect.LabyConnect;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LabyUtils {

  public static void fetchLabyFriends() {
    if (!CraftShotAddon.getInstance().configuration().labyFriendsSync().get()) {
      return;
    }

    LabyConnect labyConnect = Laby.labyAPI().labyConnect();

    if (labyConnect.isAuthenticated() && labyConnect.getSession() != null) {
      var friends = labyConnect.getSession().getFriends();
      List<CraftShotAPIClient.FriendDTO> friendList = new ArrayList<>();

      for (var friend : friends) {
        String name = friend.getName();
        UUID uuid = friend.getUniqueId();
        System.out.println("LabyMod friend found: " + name + " " + uuid);
        friendList.add(new CraftShotAPIClient.FriendDTO(uuid, name));
      }

      CraftShotAPIClient.syncFriends(friendList).thenAccept(success -> {
        if (success) {
          System.out.println("Successfully synced LabyMod friends to CraftShot.");
        } else {
          System.err.println("Failed to sync LabyMod friends to CraftShot.");
        }
      });
    } else {
      System.out.println("User is currently not connected to LabyConnect.");
    }
  }

}

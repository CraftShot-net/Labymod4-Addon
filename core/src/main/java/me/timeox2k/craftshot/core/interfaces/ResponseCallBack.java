package me.timeox2k.craftshot.core.interfaces;

import com.google.gson.JsonObject;

public interface ResponseCallBack {

  void onSuccess(JsonObject response);

  void onFailure(int responseCode, String responseBody);
}

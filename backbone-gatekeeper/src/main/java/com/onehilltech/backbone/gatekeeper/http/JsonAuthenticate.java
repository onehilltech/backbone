package com.onehilltech.backbone.gatekeeper.http;

import com.google.gson.annotations.SerializedName;

public class JsonAuthenticate
{
  public static class Content
  {
    String password;
  }

  @SerializedName("authenticate")
  public JsonAuthenticate.Content content;

  public JsonAuthenticate (String password)
  {
    this.content = new Content ();
    this.content.password = password;
  }
}

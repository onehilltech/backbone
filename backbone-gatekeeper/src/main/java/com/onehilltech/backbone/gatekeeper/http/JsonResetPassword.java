package com.onehilltech.backbone.gatekeeper.http;

import com.google.gson.annotations.SerializedName;

public class JsonResetPassword
{
  public static class Content
  {
    public String token;

    public String password;
  }

  @SerializedName ("reset-password")
  public Content content;

  public JsonResetPassword (String token, String password)
  {
    this.content = new Content ();
    this.content.token = token;
    this.content.password = password;
  }

}

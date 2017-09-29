package com.onehilltech.backbone.gatekeeper.http;

import com.google.gson.annotations.SerializedName;

public class JsonChangePassword
{
  @SerializedName ("current")
  public String currentPassword;

  @SerializedName ("new")
  public String newPassword;
}

package com.onehilltech.backbone.gatekeeper.http;

import java.util.List;

public class JsonPassword extends JsonGrant
{
  public String username;

  public String password;

  public Boolean refreshable;

  public List<String> scope;
}

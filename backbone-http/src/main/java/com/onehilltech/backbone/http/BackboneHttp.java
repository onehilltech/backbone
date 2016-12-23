package com.onehilltech.backbone.http;

import com.raizlabs.android.dbflow.config.*;

public class BackboneHttp
{
  public static void initialize ()
  {
    FlowManager.initModule (BackboneHttpGeneratedDatabaseHolder.class);
  }
}

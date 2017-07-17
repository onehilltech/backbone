package com.onehilltech.backbone.http;

import com.raizlabs.android.dbflow.annotation.Database;

@Database (
    name= BackboneHttpDatabase.DATABASE_NAME,
    version= BackboneHttpDatabase.DATABASE_VERSION)
public class BackboneHttpDatabase
{
  static final String DATABASE_NAME = "backbone_http";
  static final int DATABASE_VERSION = 1;
}

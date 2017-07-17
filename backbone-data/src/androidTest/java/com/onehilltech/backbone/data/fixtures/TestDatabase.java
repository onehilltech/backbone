package com.onehilltech.backbone.data.fixtures;

import com.raizlabs.android.dbflow.annotation.Database;

@Database (
    name = TestDatabase.NAME,
    version = TestDatabase.VERSION,
    foreignKeyConstraintsEnforced = true)
public class TestDatabase
{
  public static final String NAME = "backbone_data_test";
  public static final int VERSION = 1;
}

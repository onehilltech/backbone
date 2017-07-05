package com.onehilltech.backbone.data.fixtures;

import com.raizlabs.android.dbflow.annotation.Database;

@Database (
    name = TestDatabase.NAME,
    version = TestDatabase.VERSION,
    foreignKeysSupported = true,
    generatedClassSeparator = "$")
public class TestDatabase
{
  static final String NAME = "backbone_data_test";
  static final int VERSION = 1;
}

package com.onehilltech.backbone.data;

import com.raizlabs.android.dbflow.sql.QueryBuilder;

public class TableUtils
{
  public static String getRawTableName (String tableName)
  {
    return QueryBuilder.stripQuotes (tableName);
  }
}

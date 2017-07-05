package com.onehilltech.backbone.data;

/**
 * Created by hilljh on 7/5/17.
 */

public class TableUtils
{
  public static String getRawTableName (String tableName)
  {
    return tableName.substring (1, tableName.length () - 1);
  }
}

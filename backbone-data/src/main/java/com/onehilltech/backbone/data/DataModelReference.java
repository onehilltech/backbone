package com.onehilltech.backbone.data;

import android.support.annotation.NonNull;

import com.raizlabs.android.dbflow.structure.container.ForeignKeyContainer;
import com.raizlabs.android.dbflow.structure.container.ModelContainer;

import java.util.Map;

public class DataModelReference <T extends DataModel> extends ForeignKeyContainer <T>
{
  public DataModelReference (Class<T> table)
  {
    super (table);
  }

  public DataModelReference (Class<T> table, Map<String, Object> data)
  {
    super (table, data);
  }

  public DataModelReference (@NonNull ModelContainer<T, ?> existingContainer)
  {
    super (existingContainer);
  }

  public String getId ()
  {
    return this.getStringValue ("_id");
  }
}

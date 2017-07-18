package com.onehilltech.backbone.data;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;

public class DataModelList <T extends DataModel> extends ArrayList <T>
{
  public DataModelList (int initialCapacity)
  {
    super (initialCapacity);
  }

  public DataModelList ()
  {
  }

  void setDataStore (DataStore store)
  {
    for (DataModel model: this)
      model.setDataStore (store);
  }

  public DataModelList (@NonNull Collection<? extends T> c)
  {
    super (c);
  }
}

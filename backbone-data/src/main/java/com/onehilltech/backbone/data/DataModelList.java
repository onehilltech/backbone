package com.onehilltech.backbone.data;

import java.util.ArrayList;
import java.util.Collection;

import androidx.annotation.NonNull;

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
      model.assignTo (store);
  }

  public DataModelList (@NonNull Collection<? extends T> c)
  {
    super (c);
  }
}

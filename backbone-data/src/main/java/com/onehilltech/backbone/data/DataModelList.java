package com.onehilltech.backbone.data;

import android.support.annotation.NonNull;

import com.raizlabs.android.dbflow.structure.database.DatabaseWrapper;

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

  public DataModelList (@NonNull Collection<? extends T> c)
  {
    super (c);
  }

  public void save ()
  {
    for (T t : this)
      t.save ();
  }

  public void save (DatabaseWrapper databaseWrapper)
  {
    for (T t : this)
      t.save (databaseWrapper);
  }
}

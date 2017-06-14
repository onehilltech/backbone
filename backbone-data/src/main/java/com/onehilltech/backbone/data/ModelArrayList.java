package com.onehilltech.backbone.data;

import android.support.annotation.NonNull;

import com.raizlabs.android.dbflow.structure.BaseModel;
import com.raizlabs.android.dbflow.structure.database.DatabaseWrapper;

import java.util.ArrayList;
import java.util.Collection;

public class ModelArrayList <T extends BaseModel> extends ArrayList <T>
{
  public ModelArrayList (int initialCapacity)
  {
    super (initialCapacity);
  }

  public ModelArrayList ()
  {
  }

  public ModelArrayList (@NonNull Collection<? extends T> c)
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

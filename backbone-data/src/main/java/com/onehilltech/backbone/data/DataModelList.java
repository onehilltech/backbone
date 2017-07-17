package com.onehilltech.backbone.data;

import android.support.annotation.NonNull;

import com.onehilltech.promises.Promise;
import com.raizlabs.android.dbflow.structure.database.DatabaseWrapper;

import java.util.ArrayList;
import java.util.Collection;

import static com.onehilltech.promises.Promise.rejected;
import static com.onehilltech.promises.Promise.resolved;

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

  public Promise <Void> save (DatabaseWrapper databaseWrapper)
  {
    return new Promise<> (settlement -> {
      ArrayList <Promise <?>> promises = new ArrayList<> (this.size ());

      for (T model : this)
        promises.add (model.save (databaseWrapper));

      Promise.all (promises)
             .then (resolved (value -> settlement.resolve (null)))
             ._catch (rejected (settlement::reject));
    });
  }
}

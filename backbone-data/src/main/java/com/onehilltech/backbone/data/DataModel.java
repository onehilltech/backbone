package com.onehilltech.backbone.data;

import com.onehilltech.promises.Promise;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.structure.ModelAdapter;
import com.raizlabs.android.dbflow.structure.database.DatabaseWrapper;

public abstract class DataModel
{
  private DataStore store_;

  void setDataStore (DataStore store)
  {
    this.store_ = store;
  }

  public DataStore getDataStore ()
  {
    return this.store_;
  }

  @SuppressWarnings ("unchecked")
  public Promise <Void> save (DatabaseWrapper databaseWrapper)
  {
    if (this.store_ == null)
      return Promise.reject (new IllegalStateException ("Model must belong to a data store"));

    return new Promise<> (settlement -> {
      ModelAdapter modelAdapter = FlowManager.getModelAdapter (this.getClass ());
      modelAdapter.save (this, databaseWrapper);

      settlement.resolve (null);
    });
  }

  @SuppressWarnings ("unchecked")
  public Promise <Void> save ()
  {
    if (this.store_ == null)
      return Promise.reject (new IllegalStateException ("Model must belong to a data store"));

    return new Promise<> (settlement -> {
      ModelAdapter modelAdapter = FlowManager.getModelAdapter (this.getClass ());
      modelAdapter.save (this);

      settlement.resolve (null);
    });
  }
}

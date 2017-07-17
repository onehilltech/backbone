package com.onehilltech.backbone.data;

import com.onehilltech.promises.Promise;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.structure.ModelAdapter;
import com.raizlabs.android.dbflow.structure.database.DatabaseWrapper;

public abstract class DataModel
{
  public abstract String getId ();

  public abstract void setId (String id);

  @SuppressWarnings ("unchecked")
  public Promise <Void> save (DatabaseWrapper databaseWrapper)
  {
    return new Promise<> (settlement -> {
      ModelAdapter modelAdapter = FlowManager.getModelAdapter (this.getClass ());
      modelAdapter.save (this, databaseWrapper);

      settlement.resolve (null);
    });
  }

  @SuppressWarnings ("unchecked")
  public Promise <Void> save ()
  {
    return new Promise<> (settlement -> {
      ModelAdapter modelAdapter = FlowManager.getModelAdapter (this.getClass ());
      modelAdapter.save (this);

      settlement.resolve (null);
    });
  }
}

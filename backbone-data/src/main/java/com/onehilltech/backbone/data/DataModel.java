package com.onehilltech.backbone.data;

import com.raizlabs.android.dbflow.structure.BaseModel;
import com.raizlabs.android.dbflow.structure.database.DatabaseWrapper;

public abstract class DataModel extends BaseModel
{
  public abstract String getId ();

  public abstract void setId (String id);

  @Override
  public void delete ()
  {
    // We need to delete the data object remotely, then delete it locally.
    // DataStore dataStore = DataStore.lookup (this.getClass ());

    super.delete ();
  }

  @Override
  public void delete (DatabaseWrapper databaseWrapper)
  {
    super.delete (databaseWrapper);
  }
}

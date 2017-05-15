package com.onehilltech.backbone.data;

import com.raizlabs.android.dbflow.structure.BaseModel;

public abstract class DataModel extends BaseModel
{
  public abstract String getId ();

  public abstract void setId (String id);
}

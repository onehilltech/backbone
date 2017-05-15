package com.onehilltech.backbone.data;

import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.structure.BaseModel;

public abstract class DataModel extends BaseModel
{
  @PrimaryKey
  public String _id;

  protected DataModel ()
  {

  }

  protected DataModel (String id)
  {
    this._id = id;
  }

  public String getId ()
  {
    return this._id;
  }

  void setId (String id)
  {
    this._id = id;
  }
}

package com.onehilltech.backbone.data;

import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.structure.BaseModel;

public abstract class BaseDataModel extends BaseModel
  implements DataModel
{
  @PrimaryKey
  public String _id;

  protected BaseDataModel ()
  {

  }

  protected BaseDataModel (String id)
  {
    this._id = id;
  }

  public String getId ()
  {
    return this._id;
  }

  public void setId (String id)
  {
    this._id = id;
  }
}

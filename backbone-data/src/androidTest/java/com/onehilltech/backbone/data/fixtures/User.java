package com.onehilltech.backbone.data.fixtures;

import com.google.gson.annotations.SerializedName;
import com.onehilltech.backbone.data.DataModel;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;

@Table (name = "users", database = TestDatabase.class)
public class User extends DataModel
{
  @PrimaryKey
  public long _id;

  @Column(name = "first_name")
  @SerializedName ("first_name")
  public String firstName;

  @Column(name = "last_name")
  @SerializedName ("last_name")
  public String lastName;

  public User ()
  {

  }

  public User (long id)
  {
    this._id = id;
  }

  @Override
  public String getId ()
  {
    return Long.toString (this._id);
  }

  @Override
  public void setId (String id)
  {
    this._id = Long.parseLong (id);
  }

  @Override
  public boolean equals (Object obj)
  {
    if (!(obj instanceof User))
      return false;

    User user = (User)obj;
    return user.firstName.equals (this.firstName) && user.lastName.equals (this.lastName);
  }
}

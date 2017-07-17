package com.onehilltech.backbone.data.fixtures;

import com.onehilltech.backbone.data.DataModel;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ForeignKey;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;

@Table(name = "books", database = TestDatabase.class)
public class Book extends DataModel
{
  @PrimaryKey
  public long _id;


  @ForeignKey(stubbedRelationship = true)
  public User author;

  @Column
  public String title;

  public Book ()
  {

  }

  public Book (long _id)
  {
    this._id = _id;
  }
}

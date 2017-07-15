package com.onehilltech.backbone.data.fixtures;

import com.onehilltech.backbone.data.DataModel;
import com.onehilltech.backbone.data.serializers.DateTimeSerializer;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ForeignKey;
import com.raizlabs.android.dbflow.annotation.ForeignKeyAction;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.container.ForeignKeyContainer;

import org.joda.time.DateTime;

@Table(name = "messages", database = TestDatabase.class)
public class Message extends DataModel
{
  @PrimaryKey
  public long _id;

  @ForeignKey(
      onDelete = ForeignKeyAction.CASCADE,
      onUpdate = ForeignKeyAction.CASCADE
  )
  public ForeignKeyContainer<User> owner;

  @Column(typeConverter = DateTimeSerializer.class)
  public DateTime date;

  @Column
  public String message;

  public Message ()
  {

  }

  public Message (long _id)
  {
    this._id = _id;
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
}

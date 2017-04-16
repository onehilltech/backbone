package com.onehilltech.backbone.http;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.Index;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.annotation.Unique;
import com.raizlabs.android.dbflow.structure.BaseModel;

import org.joda.time.DateTime;

import java.net.URL;

@Table (name="backbone_resource_cache", database = BackboneHttpDatabase.class)
public class ResourceCacheModel extends BaseModel
{
  @PrimaryKey(autoincrement = true)
  long _id;

  public long getId ()
  {
    return this._id;
  }

  @Column(typeConverter = URLTypeConverter.class)
  @Unique
  @Index
  public URL url;

  @Column(name="etag")
  @Index
  public String ETag;

  @Column(name="last_modified", typeConverter = DateTimeTypeConverter.class)
  public DateTime lastModified;
}

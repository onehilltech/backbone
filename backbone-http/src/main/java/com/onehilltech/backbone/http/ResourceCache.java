package com.onehilltech.backbone.http;

import android.database.sqlite.SQLiteConstraintException;

import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.net.URL;

public final class ResourceCache
{
  private static ResourceCache instance_;

  public static ResourceCache getInstance ()
  {
    if (instance_ != null)
      return instance_;

    instance_ = new ResourceCache ();
    return instance_;
  }

  public ResourceCacheModel get (URL url)
  {
    return
        SQLite.select ()
              .from (ResourceCacheModel.class)
              .where (ResourceCacheModel$Table.url.eq (url))
              .querySingle ();
  }

  public void add (URL url, String eTag, DateTime lastModified)
  {
    DateTime utcDateTime = lastModified.withZone (DateTimeZone.UTC);

    try
    {
      SQLite.insert (ResourceCacheModel.class)
            .columns (ResourceCacheModel$Table.url, ResourceCacheModel$Table.etag, ResourceCacheModel$Table.last_modified)
            .values (url, eTag, utcDateTime)
            .execute ();
    }
    catch (SQLiteConstraintException e)
    {
      // The insert failed, lets do an update.
      SQLite.update (ResourceCacheModel.class)
            .set (ResourceCacheModel$Table.etag.eq (eTag),
                  ResourceCacheModel$Table.last_modified.eq (lastModified))
            .where (ResourceCacheModel$Table.url.eq (url))
            .execute ();
    }
  }

  public void remove (URL url)
  {
    SQLite.delete ()
          .from (ResourceCacheModel.class)
          .where (ResourceCacheModel$Table.url.eq (url))
          .execute ();
  }

  public void clear ()
  {
    SQLite.delete ().from (ResourceCacheModel.class).execute ();
  }

  public boolean hasBeenModified (URL url, DateTime moment)
  {
    ResourceCacheModel model = this.get (url);
    return model == null || model.lastModified.isAfter (moment);
  }
}

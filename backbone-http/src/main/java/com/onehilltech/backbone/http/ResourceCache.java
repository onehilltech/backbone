package com.onehilltech.backbone.http;

import android.database.sqlite.SQLiteConstraintException;

import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.net.URL;

public final class ResourceCache
{
  private static ResourceCache instance_;

  private boolean enabled_ = true;

  public static ResourceCache getInstance ()
  {
    if (instance_ != null)
      return instance_;

    instance_ = new ResourceCache ();
    return instance_;
  }

  public void enableCaching (boolean state)
  {
    this.enabled_ = state;
  }

  public boolean getIsCachingEnabled ()
  {
    return this.enabled_;
  }

  public ResourceCacheModel get (URL url)
  {
    return
        SQLite.select ()
              .from (ResourceCacheModel.class)
              .where (ResourceCacheModel_Table.url.eq (url))
              .querySingle ();
  }

  public void add (URL url, String eTag, DateTime lastModified)
  {
    DateTime utcDateTime =
        lastModified != null ?
            lastModified.withZone (DateTimeZone.UTC) :
            null;

    try
    {
      SQLite.insert (ResourceCacheModel.class)
            .columns (ResourceCacheModel_Table.url, ResourceCacheModel_Table.etag, ResourceCacheModel_Table.last_modified)
            .values (url, eTag, utcDateTime)
            .execute ();
    }
    catch (SQLiteConstraintException e)
    {
      // The insert failed, lets do an update.
      SQLite.update (ResourceCacheModel.class)
            .set (ResourceCacheModel_Table.etag.eq (eTag),
                  ResourceCacheModel_Table.last_modified.eq (lastModified))
            .where (ResourceCacheModel_Table.url.eq (url))
            .execute ();
    }
  }

  public void remove (URL url)
  {
    SQLite.delete ()
          .from (ResourceCacheModel.class)
          .where (ResourceCacheModel_Table.url.eq (url))
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

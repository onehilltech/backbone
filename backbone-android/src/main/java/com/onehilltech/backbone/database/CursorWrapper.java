package com.onehilltech.backbone.database;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;

/**
 * @class CursorWrapper
 *
 * Wrapper class for the Cursor. The CursorWrapper class allows clients to implement
 * the Proxy pattern via the Cursor, and selectively implement the desired methods
 * on the proxy.
 */
public class CursorWrapper implements Cursor
{
  private final Cursor cursor_;

  public CursorWrapper (Cursor cursor)
  {
    this.cursor_ = cursor;
  }

  @Override
  public void close ()
  {
    this.cursor_.close ();
  }

  @Override
  public int getCount ()
  {
    return this.cursor_.getCount ();
  }

  @Override
  public int getPosition ()
  {
    return this.cursor_.getPosition ();
  }

  @Override
  public boolean move (int i)
  {
    return this.cursor_.move (i);
  }

  @Override
  public boolean moveToPosition (int i)
  {
    return this.cursor_.moveToPosition (i);
  }

  @Override
  public boolean moveToFirst ()
  {
    return this.cursor_.moveToFirst ();
  }

  @Override
  public boolean moveToLast ()
  {
    return this.cursor_.moveToLast ();
  }

  @Override
  public boolean moveToNext ()
  {
    return this.cursor_.moveToNext ();
  }

  @Override
  public boolean moveToPrevious ()
  {
    return this.cursor_.moveToPrevious ();
  }

  @Override
  public boolean isFirst ()
  {
    return this.cursor_.isFirst ();
  }

  @Override
  public boolean isLast ()
  {
    return this.cursor_.isLast ();
  }

  @Override
  public boolean isBeforeFirst ()
  {
    return this.cursor_.isBeforeFirst ();
  }

  @Override
  public boolean isAfterLast ()
  {
    return this.cursor_.isAfterLast ();
  }

  @Override
  public int getColumnIndex (String s)
  {
    return this.cursor_.getColumnIndex (s);
  }

  @Override
  public int getColumnIndexOrThrow (String s)
      throws IllegalArgumentException
  {
    return this.cursor_.getColumnIndexOrThrow (s);
  }

  @Override
  public String getColumnName (int i)
  {
    return this.cursor_.getColumnName (i);
  }

  @Override
  public String[] getColumnNames ()
  {
    return this.cursor_.getColumnNames ();
  }

  @Override
  public int getColumnCount ()
  {
    return this.cursor_.getColumnCount ();
  }

  @Override
  public byte[] getBlob (int i)
  {
    return this.cursor_.getBlob (i);
  }

  @Override
  public String getString (int i)
  {
    return this.cursor_.getString (i);
  }

  @Override
  public void copyStringToBuffer (int i, CharArrayBuffer charArrayBuffer)
  {
    this.cursor_.copyStringToBuffer (i, charArrayBuffer);
  }

  @Override
  public short getShort (int i)
  {
    return this.cursor_.getShort (i);
  }

  @Override
  public int getInt (int i)
  {
    return this.cursor_.getInt (i);
  }

  @Override
  public long getLong (int i)
  {
    return this.cursor_.getLong (i);
  }

  @Override
  public float getFloat (int i)
  {
    return this.cursor_.getFloat (i);
  }

  @Override
  public double getDouble (int i)
  {
    return this.cursor_.getDouble (i);
  }

  @TargetApi (11)
  @Override
  public int getType (int i)
  {
    return this.cursor_.getType (i);
  }

  @Override
  public boolean isNull (int i)
  {
    return this.cursor_.isNull (i);
  }

  @Deprecated
  @Override
  public void deactivate ()
  {
    this.cursor_.deactivate ();
  }

  @Deprecated
  @Override
  public boolean requery ()
  {
    return this.cursor_.requery ();
  }

  @Override
  public boolean isClosed ()
  {
    return this.cursor_.isClosed ();
  }

  @Override
  public void registerContentObserver (ContentObserver contentObserver)
  {
    this.cursor_.registerContentObserver (contentObserver);
  }

  @Override
  public void unregisterContentObserver (ContentObserver contentObserver)
  {
    this.cursor_.unregisterContentObserver (contentObserver);
  }

  @Override
  public void registerDataSetObserver (DataSetObserver dataSetObserver)
  {
    this.cursor_.registerDataSetObserver (dataSetObserver);
  }

  @Override
  public void unregisterDataSetObserver (DataSetObserver dataSetObserver)
  {
    this.cursor_.unregisterDataSetObserver (dataSetObserver);
  }

  @Override
  public void setNotificationUri (ContentResolver contentResolver, Uri uri)
  {
    this.cursor_.setNotificationUri (contentResolver, uri);
  }

  @TargetApi (19)
  @Override
  public Uri getNotificationUri ()
  {
    return this.cursor_.getNotificationUri ();
  }

  @Override
  public boolean getWantsAllOnMoveCalls ()
  {
    return this.cursor_.getWantsAllOnMoveCalls ();
  }

  @TargetApi (23)
  @Override
  public void setExtras (Bundle bundle)
  {
    this.cursor_.setExtras (bundle);
  }

  @Override
  public Bundle getExtras ()
  {
    return this.cursor_.getExtras ();
  }

  @Override
  public Bundle respond (Bundle bundle)
  {
    return this.cursor_.respond (bundle);
  }
}

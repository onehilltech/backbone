package com.onehilltech.backbone.database;

import android.database.Cursor;

/**
 * @class ReverseCursor
 *
 * Wrapper for a Cursor object that accesses the elements in reverse order.
 */
public class ReverseCursor extends CursorWrapper
{
  public ReverseCursor (Cursor cursor)
  {
    super (cursor);
  }

  @Override
  public boolean isFirst ()
  {
    return super.isLast ();
  }

  @Override
  public boolean isLast ()
  {
    return super.isFirst ();
  }

  @Override
  public boolean move (int i)
  {
    return super.move (-i);
  }

  @Override
  public boolean moveToFirst ()
  {
    return super.moveToLast ();
  }

  @Override
  public boolean moveToLast ()
  {
    return super.moveToFirst ();
  }

  @Override
  public boolean moveToNext ()
  {
    return super.moveToPrevious ();
  }

  @Override
  public boolean moveToPosition (int i)
  {
    return super.moveToPosition (this.translatePosition (i));
  }

  @Override
  public boolean moveToPrevious ()
  {
    return super.moveToNext ();
  }

  @Override
  public boolean isAfterLast ()
  {
    return super.isBeforeFirst ();
  }

  @Override
  public boolean isBeforeFirst ()
  {
    return super.isAfterLast ();
  }

  @Override
  public int getPosition ()
  {
    return this.translatePosition (super.getPosition ());
  }

  private int translatePosition (int i)
  {
    return this.getCount () - i - 1;
  }
}

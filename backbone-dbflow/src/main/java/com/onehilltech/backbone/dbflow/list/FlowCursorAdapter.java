package com.onehilltech.backbone.dbflow.list;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.widget.CursorAdapter;

import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.structure.ModelAdapter;
import com.raizlabs.android.dbflow.structure.database.FlowCursor;

/**
 * Specialization of CursorAdapter for DBFLow models.
 *
 * @param <TModel>
 */
public abstract class FlowCursorAdapter <TModel> extends CursorAdapter
{
  private final ModelAdapter<TModel> modelAdapter_;

  public FlowCursorAdapter (Context context, Class<TModel> modelClass, Cursor c, boolean autoRequery)
  {
    this (context, modelClass, c, 0);
  }

  @TargetApi(11)
  public FlowCursorAdapter (Context context, Class<TModel> modelClass, Cursor c, int flags)
  {
    super (context, c, flags);
    this.modelAdapter_ = FlowManager.getModelAdapter (modelClass);
  }

  @Override
  public TModel getItem (int position)
  {
    Cursor cursor = (Cursor) super.getItem (position);
    return cursor != null ? this.modelAdapter_.loadFromCursor (FlowCursor.from (cursor)) : null;
  }
}

package com.onehilltech.backbone.dbflow.list;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.widget.CursorAdapter;

import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.structure.Model;
import com.raizlabs.android.dbflow.structure.ModelAdapter;
import com.raizlabs.android.dbflow.structure.database.FlowCursor;

/**
 * Specialization of CursorAdapter for DBFLow models.
 *
 * @param <TModel>
 */
public abstract class FlowCursorAdapter <TModel extends Model> extends CursorAdapter
{
  private final Class<TModel> modelClass_;
  private final ModelAdapter<TModel> modelAdapter_;

  public FlowCursorAdapter (Context context, Class<TModel> modelClass, Cursor c, boolean autoRequery)
  {
    super (context, c, autoRequery);

    this.modelClass_ = modelClass;
    this.modelAdapter_ = FlowManager.getModelAdapter (modelClass);
  }

  @TargetApi(11)
  public FlowCursorAdapter (Context context, Class<TModel> modelClass, Cursor c, int flags)
  {
    super (context, c, flags);

    this.modelClass_ = modelClass;
    this.modelAdapter_ = FlowManager.getModelAdapter (modelClass);
  }

  @Override
  public TModel getItem (int position)
  {
    Cursor cursor = (Cursor) super.getItem (position);

    if (cursor == null)
      return null;

    FlowCursor flowCursor = FlowCursor.from (cursor);
    return this.modelAdapter_.loadFromCursor (flowCursor);
  }
}

package com.onehilltech.backbone.dbflow.list;

import android.annotation.TargetApi;
import android.content.Context;
import android.widget.SimpleCursorAdapter;

import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.structure.ModelAdapter;
import com.raizlabs.android.dbflow.structure.database.FlowCursor;

/**
 * Utility class to be added to DBFlow.
 *
 * @param <TModel>
 */
public class FlowSimpleCursorAdapter <TModel> extends SimpleCursorAdapter
{
    private final Class<TModel> mModel;
    private final ModelAdapter<TModel> mModelAdapter;

    @TargetApi (11)
    public FlowSimpleCursorAdapter (Context context, Class<TModel> modelClass, int layout, FlowCursor c, String[] from, int[] to, int flags)
    {
        super (context, layout, c, from, to, flags);

        this.mModel = modelClass;
        this.mModelAdapter = FlowManager.getModelAdapter (modelClass);
    }

    @Override
    public TModel getItem (int position)
    {
        FlowCursor cursor = (FlowCursor) super.getItem (position);

        if (cursor == null)
            return null;

        return this.mModelAdapter.loadFromCursor (cursor);
    }
}

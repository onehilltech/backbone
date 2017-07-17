package com.onehilltech.backbone.dbflow.single;

import android.annotation.TargetApi;
import android.content.Context;

import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.queriable.Queriable;

/**
 * Utility class to be added to DBFlow.
 *
 * @param <TModelView>
 */
@TargetApi(11)
public class FlowModelViewLoader <TModelView>
  extends FlowSingleModelLoader<TModelView>
{
  public FlowModelViewLoader (Context context,
                              Class <TModelView> modelView,
                              Queriable queriable)
  {
    super (context, modelView, FlowManager.getModelViewAdapter (modelView), queriable);
  }
}
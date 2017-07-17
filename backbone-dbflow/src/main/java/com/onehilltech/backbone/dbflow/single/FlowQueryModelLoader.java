package com.onehilltech.backbone.dbflow.single;

import android.annotation.TargetApi;
import android.content.Context;

import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.queriable.Queriable;

/**
 * Utility class to be added to DBFlow.
 *
 * @param <TQueryModel>
 */
@TargetApi(11)
public class FlowQueryModelLoader <TQueryModel>
  extends FlowSingleModelLoader<TQueryModel>
{
  public FlowQueryModelLoader (Context context,
                               Class <TQueryModel> model,
                               Queriable queriable)
  {
    super (context, model, FlowManager.getQueryModelAdapter (model), queriable);
    this.setObserveModel (false);
  }
}
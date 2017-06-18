package com.onehilltech.backbone.data;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.onehilltech.backbone.app.Promise;
import com.onehilltech.backbone.dbflow.single.FlowModelLoader;
import com.onehilltech.backbone.http.retrofit.ResourceEndpoint;
import com.raizlabs.android.dbflow.config.DatabaseDefinition;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.sql.language.property.Property;
import com.raizlabs.android.dbflow.sql.language.property.PropertyFactory;
import com.raizlabs.android.dbflow.sql.queriable.Queriable;
import com.raizlabs.android.dbflow.structure.BaseModel;
import com.raizlabs.android.dbflow.structure.ModelAdapter;

import retrofit2.Retrofit;

import static com.onehilltech.backbone.app.Promise.rejected;
import static com.onehilltech.backbone.app.Promise.resolved;

public class DataStore
{
  private static final Property <String> _ID = PropertyFactory.from ("_id");

  private final Context context_;

  private final Class <?> databaseClass_;

  private final DatabaseDefinition databaseDefinition_;

  private final Retrofit retrofit_;

  public DataStore (Context context, @NonNull Class <?> databaseClass, @NonNull Retrofit retrofit)
  {
    this.context_ = context;
    this.databaseClass_ = databaseClass;
    this.retrofit_ = retrofit;
    this.databaseDefinition_ = FlowManager.getDatabase (this.databaseClass_);

    if (this.databaseDefinition_ == null)
      throw new IllegalArgumentException ("Cannot locate database for " + databaseClass.getName ());
  }

  public interface OnModelLoaded <T extends BaseModel>
  {
    void onModelLoaded (T model);
  }

  public <T extends DataModel>  LoaderManager.LoaderCallbacks <T>
  createSingleModelLoaderCallback (@NonNull Class <T> modelClass, @NonNull String id, @NonNull OnModelLoaded <T> onModelLoaded)
  {
    return new LoaderManager.LoaderCallbacks<T> ()
    {
      @Override
      public Loader<T> onCreateLoader (int i, Bundle bundle)
      {
        Queriable queriable =
            SQLite.select ()
                  .from (modelClass)
                  .where (DataStore._ID.eq (id));

        return new FlowModelLoader<> (context_, modelClass, queriable);
      }

      @Override
      public void onLoadFinished (Loader<T> loader, T t)
      {
        onModelLoaded.onModelLoaded (t);
      }

      @Override
      public void onLoaderReset (Loader<T> loader)
      {

      }
    };
  }

  /**
   * Get a single model element by making a network request. If the server returns that
   * the resource has not been modified, then we load the resource from our local disk.
   *
   * @param dataClass       Data model class
   * @param id              Id of model
   * @return                The model, or null
   */
  public <T extends DataModel> Promise <T> get (Class <T> dataClass, String id)
  {
    return new Promise<> (settlement -> {
      ModelAdapter modelAdapter = this.databaseDefinition_.getModelAdapterForTable (dataClass);

      if (modelAdapter != null)
      {
        String tableName = modelAdapter.getTableName ();
        String singular = Pluralize.singularize (tableName);

        ResourceEndpoint <T> endpoint = ResourceEndpoint.create (this.retrofit_, singular, tableName);

        endpoint.get (id)
                .then (resolved (r -> {
                  // Get the result, and save to the database.
                  T model = r.get (singular);
                  model.save ();

                  // Resolve the result.
                  settlement.resolve (model);
                }))
                ._catch (rejected (reason -> {
                  if (reason.getLocalizedMessage ().toLowerCase ().equals ("not modified"))
                  {
                    // The server said that the data has not been modified. This means we
                    // already have the data cached locally. Let's use the peek () method
                    // to load the data from disk, and resolve our promise.

                    this.peek (dataClass, id)
                        .then (resolved (settlement::resolve))
                        ._catch (rejected (settlement::reject));
                  }
                  else
                  {
                    settlement.reject (reason);
                  }
                }));
      }
      else
      {
        settlement.reject (new IllegalArgumentException ("Cannot locate model adapter for " + dataClass.getName ()));
      }
    });
  }

  /**
   * Get a single model element without making a network request. It is assumed that
   * the model element already exist in the data store. If the element does not exist,
   * then a null value is returned via the Promise.
   *
   * @param dataClass           Data model class
   * @param id                  Id of model
   * @return                    The model, or null
   */
  public <T extends DataModel> Promise <T> peek (Class <T> dataClass, String id)
  {
    return new Promise<> (settlement -> {
      T dataModel =
          SQLite.select ()
                .from (dataClass)
                .where (_ID.eq (id))
                .querySingle ();

      settlement.resolve (dataModel);
    });
  }
}

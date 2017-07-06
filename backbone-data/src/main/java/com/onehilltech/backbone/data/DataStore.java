package com.onehilltech.backbone.data;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.onehilltech.backbone.data.serializers.DateTimeSerializer;
import com.onehilltech.backbone.dbflow.single.FlowModelLoader;
import com.onehilltech.backbone.http.HttpError;
import com.onehilltech.backbone.http.Resource;
import com.onehilltech.backbone.http.retrofit.ResourceCacheInterceptor;
import com.onehilltech.backbone.http.retrofit.ResourceEndpoint;
import com.onehilltech.backbone.http.retrofit.gson.GsonResourceManager;
import com.onehilltech.backbone.http.retrofit.gson.GsonResourceMarshaller;
import com.onehilltech.promises.Promise;
import com.raizlabs.android.dbflow.config.DatabaseDefinition;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.Condition;
import com.raizlabs.android.dbflow.sql.language.NameAlias;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.sql.queriable.Queriable;
import com.raizlabs.android.dbflow.structure.BaseModel;
import com.raizlabs.android.dbflow.structure.ModelAdapter;

import org.joda.time.DateTime;

import java.util.List;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.onehilltech.promises.Promise.rejected;
import static com.onehilltech.promises.Promise.resolved;

public class DataStore
{
  public static class Builder
  {
    private final Context context_;

    private Class <?> databaseClass_;

    private OkHttpClient httpClient_;

    private String baseUrl_;

    public Builder (Context context)
    {
      this.context_ = context;
    }

    public Builder setDatabaseClass (Class <?> databaseClass)
    {
      this.databaseClass_ = databaseClass;
      return this;
    }

    public Builder setBaseUrl (String baseUrl)
    {
      this.baseUrl_ = baseUrl;
      return this;
    }

    public Builder setHttpClient (OkHttpClient httpClient)
    {
      this.httpClient_ = httpClient;
      return this;
    }

    public DataStore build ()
    {
      if (this.databaseClass_ == null)
        throw new IllegalStateException ("You must set a database class");

      if (this.baseUrl_ == null)
        throw new IllegalStateException ("You must provide a base URL for the data store");

      // We need to create our own HttpClient, but we need to use the provided on
      // as the foundation. This allows us to merge the client's configuration with
      // the required configuration for the data store.
      OkHttpClient.Builder httpClientBuilder =
          this.httpClient_ != null ?
              this.httpClient_.newBuilder () :
              new OkHttpClient.Builder ();

      httpClientBuilder.addInterceptor (new ResourceCacheInterceptor ());

      // Register the different models in the database with Gson, and then register the
      // Gson instance with the Retrofit builder.
      GsonResourceManager resourceManager = this.makeResourceManagerFromDatabase ();

      GsonResourceMarshaller resourceMarshaller =
          new GsonResourceMarshaller.Builder ()
              .setResourceManager (resourceManager)
              .build ();

      Gson gson =
          new GsonBuilder ()
              .registerTypeAdapter (Resource.class, resourceMarshaller)
              .registerTypeAdapter (DateTime.class, new DateTimeSerializer ())
              .registerTypeAdapterFactory (new DataModelTypeAdapterFactory ())
              .create ();

      resourceMarshaller.setGson (gson);

      // Build the Retrofit instance for the data store.
      Retrofit.Builder retrofitBuilder =
          new Retrofit.Builder ()
              .baseUrl (this.baseUrl_);

      if (this.httpClient_ != null)
        retrofitBuilder.client (this.httpClient_);

      Retrofit retrofit =
          retrofitBuilder
              .addConverterFactory (GsonConverterFactory.create (gson))
              .build ();

      return new DataStore (this.context_, this.databaseClass_, retrofit);
    }

    private GsonResourceManager makeResourceManagerFromDatabase ()
    {
      GsonResourceManager manager = new GsonResourceManager ();

      DatabaseDefinition databaseDefinition = FlowManager.getDatabase (this.databaseClass_);
      List<ModelAdapter> modelAdapters = databaseDefinition.getModelAdapters ();

      for (ModelAdapter modelAdapter : modelAdapters)
      {
        String rawTableName = TableUtils.getRawTableName (modelAdapter.getTableName ());
        String rcName = Pluralize.singular (rawTableName);
        Class <?> modelClass = modelAdapter.getModelClass ();

        // Register the a single model and a list of models based on the name of the
        // model table.
        manager.registerType (rcName, modelClass);
      }

      return manager;
    }
  }

  private static final NameAlias _ID = NameAlias.builder ("_id").build ();

  private final Context context_;

  private final Class <?> databaseClass_;

  private final DatabaseDefinition databaseDefinition_;

  private final Retrofit retrofit_;

  public interface OnModelLoaded <T extends BaseModel>
  {
    void onModelLoaded (T model);
  }
  private DataStore (@NonNull Context context, @NonNull Class <?> databaseClass, @NonNull Retrofit retrofit)
  {
    this.context_ = context;
    this.databaseClass_ = databaseClass;
    this.retrofit_ = retrofit;
    this.databaseDefinition_ = FlowManager.getDatabase (this.databaseClass_);

    if (this.databaseDefinition_ == null)
      throw new IllegalArgumentException ("Cannot locate database for " + databaseClass.getName ());
  }


  public <T extends DataModel>  LoaderManager.LoaderCallbacks <T>
  createSingleModelLoaderCallback (@NonNull Class <T> modelClass, @NonNull Object id, @NonNull OnModelLoaded <T> onModelLoaded)
  {
    return new LoaderManager.LoaderCallbacks<T> ()
    {
      @Override
      public Loader<T> onCreateLoader (int i, Bundle bundle)
      {
        Queriable queriable =
            SQLite.select ()
                  .from (modelClass)
                  .where (Condition.column (_ID).eq (id));

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
  public <T extends DataModel> Promise <T> get (Class <T> dataClass, Object id)
  {
    ModelAdapter modelAdapter = this.databaseDefinition_.getModelAdapterForTable (dataClass);

    if (modelAdapter == null)
      return Promise.reject (new IllegalArgumentException ("Cannot locate model adapter for " + dataClass.getName ()));

    return new Promise<> (settlement -> {
      String tableName = TableUtils.getRawTableName (modelAdapter.getTableName ());
      String singular = Pluralize.singular (tableName);

      ResourceEndpoint <T> endpoint = ResourceEndpoint.create (this.retrofit_, singular, tableName);

      endpoint.get (id.toString ())
              .then (resolved (r -> {
                // Get the result, and save to the database.
                T model = r.get (singular);

                if (model != null)
                  model.save ();

                // Resolve the result.
                settlement.resolve (model);
              }))
              ._catch (rejected (reason -> {
                if ((reason instanceof HttpError))
                {
                  HttpError httpError = (HttpError)reason;

                  if (httpError.getStatusCode () == 304)
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
                }
                else
                {
                  settlement.reject (reason);
                }
              }));
    });
  }

  /**
   * Get a single model element without making a network request. It is assumed that
   * the model element already exist in the data store. If the element does not exist,
   * then a null value is returned via the Promise.
   *
   * @param dataClass           Data model class
   * @param id                  Id of model
   * @return                    Promise object
   */
  public <T extends DataModel> Promise <T> peek (Class <T> dataClass, Object id)
  {
    return new Promise<> (settlement -> {
      T dataModel =
          SQLite.select ()
                .from (dataClass)
                .where (Condition.column (_ID).eq (id))
                .querySingle ();

      settlement.resolve (dataModel);
    });
  }
}

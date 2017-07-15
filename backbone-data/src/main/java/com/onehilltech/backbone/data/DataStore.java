package com.onehilltech.backbone.data;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.onehilltech.backbone.data.serializers.DateTimeSerializer;
import com.onehilltech.backbone.dbflow.single.FlowModelLoader;
import com.onehilltech.backbone.http.HttpError;
import com.onehilltech.backbone.http.Resource;
import com.onehilltech.promises.Promise;
import com.raizlabs.android.dbflow.config.DatabaseDefinition;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.SqlUtils;
import com.raizlabs.android.dbflow.sql.language.Condition;
import com.raizlabs.android.dbflow.sql.language.From;
import com.raizlabs.android.dbflow.sql.language.NameAlias;
import com.raizlabs.android.dbflow.sql.language.SQLCondition;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.sql.queriable.Queriable;
import com.raizlabs.android.dbflow.structure.BaseModel;
import com.raizlabs.android.dbflow.structure.ModelAdapter;
import com.raizlabs.android.dbflow.structure.container.ForeignKeyContainer;

import org.joda.time.DateTime;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.onehilltech.promises.Promise.rejected;
import static com.onehilltech.promises.Promise.resolved;

public class DataStore
{
  public static class Builder
  {
    private Class <?> databaseClass_;

    private OkHttpClient httpClient_;

    private String baseUrl_;

    public Builder (Class <?> databaseClass)
    {
      this.databaseClass_ = databaseClass;
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
      ResourceSerializer resourceSerializer = this.makeResourceSerializeFromDatabase ();

      Gson gson =
          new GsonBuilder ()
              .registerTypeAdapter (Resource.class, resourceSerializer)
              .registerTypeAdapter (DateTime.class, new DateTimeSerializer ())
              .registerTypeAdapterFactory (new DataModelTypeAdapterFactory ())
              .create ();

      resourceSerializer.setGson (gson);

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

      return new DataStore (this.databaseClass_, retrofit);
    }

    private ResourceSerializer makeResourceSerializeFromDatabase ()
    {
      ResourceSerializer serializer = new ResourceSerializer ();
      DatabaseDefinition databaseDefinition = FlowManager.getDatabase (this.databaseClass_);
      List<ModelAdapter> modelAdapters = databaseDefinition.getModelAdapters ();

      for (ModelAdapter modelAdapter : modelAdapters)
      {
        String pluralName = TableUtils.getRawTableName (modelAdapter.getTableName ());
        String singularName = Pluralize.singular (pluralName);
        Class <?> modelClass = modelAdapter.getModelClass ();

        serializer.put (pluralName, modelClass);

        if (!pluralName.equals (singularName))
          serializer.put (singularName, modelClass);
      }

      return serializer;
    }
  }

  private static final NameAlias _ID = NameAlias.builder ("_id").build ();

  private final Class <?> databaseClass_;

  private final DatabaseDefinition databaseDefinition_;

  private final Retrofit retrofit_;

  public interface OnModelLoaded <T extends BaseModel>
  {
    void onModelLoaded (T model);
  }

  private DataStore (@NonNull Class <?> databaseClass, @NonNull Retrofit retrofit)
  {
    this.databaseClass_ = databaseClass;
    this.retrofit_ = retrofit;
    this.databaseDefinition_ = FlowManager.getDatabase (this.databaseClass_);

    if (this.databaseDefinition_ == null)
      throw new IllegalArgumentException ("Cannot locate database for " + databaseClass.getName ());

    this.initDependencyGraph ();
  }

  private void initDependencyGraph ()
  {
    for (ModelAdapter modelAdapter: this.databaseDefinition_.getModelAdapters ())
    {
      Class <?> modelClass = modelAdapter.getModelClass ();

      Field [] fields = modelClass.getDeclaredFields ();

      for (Field field : fields)
      {
        System.err.println (field.getName () + ": " + field.getType ().getName ());

        if (field.getType ().equals (ForeignKeyContainer.class))
        {

        }
      }
    }
  }

  public <T extends DataModel>  LoaderManager.LoaderCallbacks <T>
  createSimpleModelLoaderCallback (@NonNull Context context,
                                   @NonNull Class <T> modelClass,
                                   @NonNull Object id,
                                   @NonNull OnModelLoaded <T> onModelLoaded)
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

        return new FlowModelLoader<> (context, modelClass, queriable);
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

  public Class <?> getDatabaseClass ()
  {
    return this.databaseClass_;
  }

  /**
   * Create a new object in the data store.
   *
   * @param dataClass           The data class
   * @param value               The new value
   * @return                    Promise object
   */
  public <T extends DataModel> Promise <T> create (Class <T> dataClass, T value)
  {
    return new Promise<> (settlement -> {
      ResourceEndpoint <T> endpoint = this.getEndpoint (dataClass);

      endpoint.create (value)
              .then (resolved (resource -> {
                T newValue = resource.get (endpoint.getName ());
                newValue.save ();

                settlement.resolve (newValue);
              }))
              ._catch (rejected (settlement::reject));
    });
  }

  /**
   * Get all the models of a single data class. This method will make a network request
   * to get the data.
   *
   * @param dataClass       Data model class
   * @return                The model, or null
   */
  public <T extends DataModel> Promise <DataModelList <T>> get (Class <T> dataClass)
  {
    return new Promise<> (settlement -> {
      ResourceEndpoint <T> endpoint = this.getEndpoint (dataClass);

      endpoint.get ()
              .then (resolved (r -> {
                // Get the result, and save to the database.
                DataModelList <T> modelList = r.get (endpoint.getName ());

                if (modelList != null)
                  modelList.save ();

                // Resolve the result.
                settlement.resolve (modelList);
              }))
              ._catch (rejected (handleErrorOrLoadFromCache (settlement, () ->
                  this.peek (dataClass)
                      .then (resolved (settlement::resolve))
                      ._catch (rejected (settlement::reject))
              )));
    });
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
    ModelAdapter <T> modelAdapter = this.getModelAdapter (dataClass);

    return new Promise<> (settlement -> {
      ResourceEndpoint <T> endpoint = this.getEndpoint (dataClass);

      endpoint.get (id.toString ())
              .then (resolved (r -> {
                // Get the result, and save to the database.
                T model = r.get (endpoint.getName ());

                if (model != null)
                  model.save ();

                // Resolve the result.
                settlement.resolve (model);
              }))
              ._catch (rejected (handleErrorOrLoadFromCache (settlement, () ->
                  this.peek (dataClass, id)
                      .then (resolved (settlement::resolve))
                      ._catch (rejected (settlement::reject))
              )));
    });
  }

  /**
   * Query for a set of models by making a network request. If the server returns that the
   * list of models has not be modified, then we load the list of models from local disk.
   *
   * @param dataClass
   * @param query
   * @param <T>
   * @return
   */
  public <T extends DataModel> Promise <DataModelList <T>> query (Class <T> dataClass, Map <String, Object> query)
  {
    return new Promise<> (settlement -> {
      ModelAdapter <T> modelAdapter = this.getModelAdapter (dataClass);
      String tableName = TableUtils.getRawTableName (modelAdapter.getTableName ());
      String singular = Pluralize.singular (tableName);
      ResourceEndpoint <T> endpoint = ResourceEndpoint.create (this.retrofit_, singular, tableName);

      endpoint.get (query)
              .then (resolved (r -> {
                // Get the result, and save to the database.
                DataModelList <T> modelList = r.get (tableName);

                if (modelList != null)
                  modelList.save ();

                // Resolve the result.
                settlement.resolve (modelList);
              }))
              ._catch (rejected (handleErrorOrLoadFromCache (settlement, () ->
                  this.select (dataClass, query)
                      .then (resolved (settlement::resolve))
                      ._catch (rejected (settlement::reject))
              )));
    });
  }

  /**
   * Query for a list of models.
   *
   * @param dataClass         Data class
   * @param query             Query parameters
   * @return                  Promise object
   */
  public <T extends DataModel> Promise <Cursor> queryCursor (Class <T> dataClass, Map <String, Object> query)
  {
    return new Promise<> (settlement -> {
      ModelAdapter modelAdapter = this.getModelAdapter (dataClass);
      String tableName = TableUtils.getRawTableName (modelAdapter.getTableName ());
      ResourceEndpoint <T> endpoint = this.getEndpoint (dataClass);

      endpoint.get (query)
              .then (resolved (r -> {
                DataModelList <T> list = r.get (tableName);

                if (list != null && !list.isEmpty ())
                  list.save ();

                this.selectCursor (dataClass, query)
                    .then (resolved (settlement::resolve))
                    ._catch (rejected (settlement::reject));
              }))
              ._catch (rejected (handleErrorOrLoadFromCache (settlement, () ->
                  this.selectCursor (dataClass, query)
                      .then (resolved (settlement::resolve))
                      ._catch (rejected (settlement::reject))
              )));
    });
  }

  /**
   * Update an existing model element.
   *
   * @param dataClass
   * @param model
   * @param <T>
   * @return
   */
  public <T extends DataModel> Promise <T> update (Class <T> dataClass, T model)
  {
    return new Promise<> (settlement -> {
      ResourceEndpoint <T> endpoint = this.getEndpoint (dataClass);

      endpoint.update (model.getId (), model)
              .then (resolved (resource -> {
                // Get the new value and save it to the database. We do this just in
                // case the update value is not the same as the value we receive from
                // the service.
                T newValue = resource.get (endpoint.getName ());
                newValue.save ();

                settlement.resolve (newValue);
              }))
              ._catch (rejected (settlement::reject));
    });
  }

  /**
   * Delete a single model from the data store.
   *
   * @param dataClass           Data class
   * @param id                  Model id
   * @return                    Promise object
   */
  public <T extends DataModel> Promise <Boolean> delete (Class <T> dataClass, Object id)
  {
    return new Promise<> (settlement -> {
      ResourceEndpoint <T> endpoint = this.getEndpoint (dataClass);

      endpoint.delete (id.toString ())
              .then (resolved (result -> {
                if (result)
                {
                  SQLCondition [] condition = {Condition.column (_ID).eq (id)};

                  // Delete the object from our local cache, then notify all that the model
                  // has indeed been deleted.
                  SQLite.delete ()
                        .from (dataClass)
                        .where (condition)
                        .execute ();

                  SqlUtils.notifyModelChanged (dataClass, BaseModel.Action.DELETE, Arrays.asList (condition));
                }

                settlement.resolve (result);
              }))
              ._catch (rejected (settlement::reject));
    });
  }

  /**
   * Get all the models without making a network request.
   *
   * @param dataClass           Data model class
   * @return                    Promise object
   */
  public <T extends DataModel> Promise <Cursor> peekCursor (Class <T> dataClass)
  {
    return new Promise<> (settlement -> {
      Cursor cursor =
          SQLite.select ()
                .from (dataClass)
                .query ();

      settlement.resolve (cursor);
    });
  }

  /**
   * Get all the models without making a network request.
   *
   * @param dataClass           Data model class
   * @return                    Promise object
   */
  public <T extends DataModel> Promise <DataModelList <T>> peek (Class <T> dataClass)
  {
    ModelAdapter <T> modelAdapter = this.getModelAdapter (dataClass);

    return new Promise<> (settlement ->
      this.peekCursor (dataClass)
          .then (resolved (cursor -> {
            DataModelList <T> modelList = new DataModelList<> (cursor.getCount ());

            while (cursor.moveToNext ())
            {
              T model = modelAdapter.loadFromCursor (cursor);
              modelList.add (model);
            }

            settlement.resolve (modelList);
          }))
          ._catch (rejected (settlement::reject))
    );
  }

  /**
   * Get the models for the data class from the local data store.
   *
   * @param dataClass
   * @param query
   * @param <T>
   * @return
   */
  public <T extends DataModel> Promise <DataModelList <T>> select (Class <T> dataClass, Map <String, Object> query)
  {
    ModelAdapter <T> modelAdapter = this.getModelAdapter (dataClass);

    return new Promise<> (settlement ->
      this.selectCursor (dataClass, query)
          .then (resolved (cursor -> {
            DataModelList<T> modelList = new DataModelList<> (cursor.getCount ());

            while (cursor.moveToNext ())
            {
              T model = modelAdapter.loadFromCursor (cursor);
              modelList.add (model);
            }

            settlement.resolve (modelList);
          }))
          ._catch (rejected (settlement::reject))
    );
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
    this.getModelAdapter (dataClass);

    return new Promise<> (settlement -> {
      T dataModel =
          SQLite.select ()
                .from (dataClass)
                .where (Condition.column (_ID).eq (id))
                .querySingle ();

      settlement.resolve (dataModel);
    });
  }

  /**
   * Get all model elements that meet the specified criteria without making a
   * network request.
   *
   * @param dataClass       Data model class
   * @param params          Criteria
   * @return                Promise object
   */
  public <T extends DataModel> Promise <Cursor> selectCursor (Class <T> dataClass, Map <String, Object> params)
  {
    this.getModelAdapter (dataClass);

    return new Promise<> (settlement -> {
      From<?> from = SQLite.select ().from (dataClass);

      for (Map.Entry <String, Object> param: params.entrySet ())
        from.where (Condition.column (NameAlias.rawBuilder (param.getKey ()).build ()).eq (param.getValue ()));

      Cursor cursor = from.query ();
      settlement.resolve (cursor);
    });
  }

  private <T extends DataModel> ResourceEndpoint <T> getEndpoint (Class <T> dataClass)
  {
    ModelAdapter <T> modelAdapter = this.getModelAdapter (dataClass);
    String tableName = TableUtils.getRawTableName (modelAdapter.getTableName ());
    String singular = Pluralize.singular (tableName);

    return ResourceEndpoint.create (this.retrofit_, singular, tableName);
  }

  /**
   * Get the model adapter for the database.
   *
   * @param dataClass       Data model class
   * @return                ModelAdapter object
   */
  private <T extends DataModel> ModelAdapter <T> getModelAdapter (Class <T> dataClass)
  {
    @SuppressWarnings ("unchecked")
    ModelAdapter <T> modelAdapter = this.databaseDefinition_.getModelAdapterForTable (dataClass);

    if (modelAdapter != null)
      return modelAdapter;

    throw new IllegalArgumentException ("Cannot locate model adapter for " + dataClass.getName ());
  }


  private interface OnNotModified <T>
  {
    void onNotMofified ();
  }

  private static <T> HandleErrorOrLoadFromCache<T> handleErrorOrLoadFromCache (Promise.Settlement <T> settlement, OnNotModified <T> onNotModified)
  {
    return new HandleErrorOrLoadFromCache <> (settlement, onNotModified);
  }

  private static class HandleErrorOrLoadFromCache <T> implements Promise.RejectNoReturn
  {
    private Promise.Settlement <T> settlement_;

    private OnNotModified <T> onNotModified_;

    HandleErrorOrLoadFromCache (Promise.Settlement <T> settlement, OnNotModified <T> onNotModified)
    {
      this.settlement_ = settlement;
      this.onNotModified_ = onNotModified;
    }

    @Override
    public void rejectNoReturn (Throwable reason)
    {
      if ((reason instanceof HttpError))
      {
        HttpError httpError = (HttpError) reason;

        if (httpError.getStatusCode () == 304)
        {
          // The server said that the data has not been modified. This means we
          // already have the data cached locally. Let's use the peek () method
          // to load the data from disk, and resolve our promise.

          this.onNotModified_.onNotMofified ();
        }
        else
        {
          this.settlement_.reject (reason);
        }
      }
      else
      {
        this.settlement_.reject (reason);
      }
    }
  }
}

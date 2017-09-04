package com.onehilltech.backbone.data;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import com.onehilltech.backbone.data.serializers.DateTimeSerializer;
import com.onehilltech.backbone.dbflow.single.FlowModelLoader;
import com.onehilltech.backbone.http.HttpError;
import com.onehilltech.backbone.http.Resource;
import com.onehilltech.promises.Promise;
import com.raizlabs.android.dbflow.config.DatabaseDefinition;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.SqlUtils;
import com.raizlabs.android.dbflow.sql.language.From;
import com.raizlabs.android.dbflow.sql.language.NameAlias;
import com.raizlabs.android.dbflow.sql.language.Operator;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.sql.queriable.Queriable;
import com.raizlabs.android.dbflow.structure.BaseModel;
import com.raizlabs.android.dbflow.structure.ModelAdapter;
import com.raizlabs.android.dbflow.structure.database.FlowCursor;

import org.joda.time.DateTime;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.onehilltech.promises.Promise.rejected;
import static com.onehilltech.promises.Promise.resolved;

/**
 * @class DataStore
 *
 * The DataStore is an abstraction that manages different data objects. The DataStore has
 * methods for retrieving data from both local and remote storage. Data from remote storage
 * is stored locally for faster access.
 */
public class DataStore
{
  public static class Builder
  {
    private Class <?> databaseClass_;

    private OkHttpClient httpClient_;

    private String baseUrl_;

    private final LinkedHashMap <Type, Object> typeAdapters_ = new LinkedHashMap <> ();

    private final ArrayList <TypeAdapterFactory> typeAdapterFactories_ = new ArrayList<> ();

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

    public Builder addTypeAdapter (Type type, Object typeAdapter)
    {
      this.typeAdapters_.put (type, typeAdapter);
      return this;
    }

    public Builder addTypeAdapterFactory (TypeAdapterFactory typeAdapterFactory)
    {
      this.typeAdapterFactories_.add (typeAdapterFactory);
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
      resourceSerializer.put ("errors", HttpError.class);

      GsonBuilder gsonBuilder =
          new GsonBuilder ()
              .registerTypeAdapter (Resource.class, resourceSerializer)
              .registerTypeAdapter (DateTime.class, new DateTimeSerializer ())
              .registerTypeAdapterFactory (new DataModelTypeAdapterFactory ());

      for (Map.Entry <Type, Object> entry: this.typeAdapters_.entrySet ())
        gsonBuilder.registerTypeAdapter (entry.getKey (), entry.getValue ());

      for (TypeAdapterFactory typeAdapterFactory: this.typeAdapterFactories_)
        gsonBuilder.registerTypeAdapterFactory (typeAdapterFactory);

      Gson gson = gsonBuilder.create ();
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
        String singularName = Pluralize.getInstance ().singular (pluralName);
        Class <?> modelClass = modelAdapter.getModelClass ();

        serializer.put (pluralName, modelClass);

        if (!pluralName.equals (singularName))
          serializer.put (singularName, modelClass);
      }

      return serializer;
    }
  }

  private static final String FIELD_ID = "_id";
  private static final NameAlias _ID = NameAlias.of (FIELD_ID);

  private final Class <?> databaseClass_;

  private final DatabaseDefinition databaseDefinition_;

  private final Retrofit retrofit_;

  private final Map <Class <?>, ResourceEndpoint <?>> endpoints_ = new LinkedHashMap<> ();

  public interface OnModelLoaded <T>
  {
    void onModelLoaded (T model);
  }

  private DataStore (@NonNull Class <?> databaseClass, @NonNull Retrofit retrofit)
  {
    this.databaseClass_ = databaseClass;
    this.retrofit_ = retrofit;
    this.databaseDefinition_ = FlowManager.getDatabase (this.databaseClass_);
    this.initDependencyGraph ();
  }

  private void initDependencyGraph ()
  {

  }

  public <T extends DataModel>  LoaderManager.LoaderCallbacks <T>
  createSimpleModelLoaderCallback (@NonNull Context context,
                                   @NonNull Class <T> modelClass,
                                   @NonNull Object id,
                                   @NonNull OnModelLoaded <T> onModelLoaded)
  {
    return new LoaderManager.LoaderCallbacks<T> () {
      @Override
      public Loader<T> onCreateLoader (int i, Bundle bundle)
      {
        Queriable queriable =
            SQLite.select ()
                  .from (modelClass)
                  .where (Operator.op (_ID).eq (id));

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
                // Get the new value, and associate it with this data store.
                T newValue = resource.get (endpoint.getName ());

                // Insert the created value in our database.
                this.push (dataClass, newValue)
                    .then (resolved (settlement::resolve))
                    ._catch (rejected (settlement::reject));
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
      ModelAdapter <T> modelAdapter = this.getModelAdapter (dataClass);
      String tableName = TableUtils.getRawTableName (modelAdapter.getTableName ());

      endpoint.get ()
              .then (resolved (r -> {
                // Get the result, and insert to the database.
                DataModelList <T> modelList = r.get (tableName);

                if (modelList != null)
                {
                  this.insertIntoDatabase (dataClass, modelList)
                      .then (resolved (settlement::resolve))
                      ._catch (rejected (settlement::reject));
                }
                else
                {
                  settlement.resolve (new DataModelList<> ());
                }
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
    return new Promise<> (settlement -> {
      this.getModelAdapter (dataClass);
      ResourceEndpoint <T> endpoint = this.getEndpoint (dataClass);

      endpoint.get (id.toString ())
              .then (resolved (r -> {
                // Get the result, and insert to the database.
                T model = r.get (endpoint.getName ());

                if (model != null)
                {
                  this.push (dataClass, model)
                      .then (resolved (settlement::resolve))
                      ._catch (rejected (settlement::reject));
                }
                else
                {
                  settlement.resolve (null);
                }
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
   * @param dataClass           Class object
   * @param query               Query strings
   * @return                    Promise object
   */
  public <T extends DataModel> Promise <DataModelList <T>> query (Class <T> dataClass, Map <String, Object> query)
  {
    return new Promise<> (settlement -> {
      ResourceEndpoint <T> endpoint = this.getEndpoint (dataClass);
      ModelAdapter <T> modelAdapter = this.getModelAdapter (dataClass);
      String tableName = TableUtils.getRawTableName (modelAdapter.getTableName ());

      endpoint.get (query)
              .then (resolved (r -> {
                // Check the dependencies for this data class. Make sure we input all
                // the dependent models first before we insert the models for this data
                // class.

                // Get the result, and insert to the database.
                DataModelList <T> modelList = r.get (tableName);

                if (modelList != null)
                {
                  this.insertIntoDatabase (dataClass, modelList)
                      .then (resolved (settlement::resolve))
                      ._catch (rejected (settlement::reject));
                }
                else
                {
                  settlement.resolve (new DataModelList<> ());
                }
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
  public <T extends DataModel> Promise <FlowCursor> queryCursor (Class <T> dataClass, Map <String, Object> query)
  {
    return new Promise<> (settlement -> {
      ResourceEndpoint <T> endpoint = this.getEndpoint (dataClass);
      ModelAdapter modelAdapter = this.getModelAdapter (dataClass);
      String tableName = TableUtils.getRawTableName (modelAdapter.getTableName ());

      endpoint.get (query)
              .then (r -> {
                DataModelList <T> list = r.get (tableName);
                return list != null ? this.insertIntoDatabase (dataClass, list) : Promise.resolve (null);
              })
              .then (resolved (result ->
                this.selectCursor (dataClass, query)
                    .then (resolved (settlement::resolve))
                    ._catch (rejected (settlement::reject))
              ))
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
   * @param dataClass       Class object
   * @param model           Updated model
   * @return                Promise object
   */
  public <T extends DataModel> Promise <T> update (Class <T> dataClass, T model)
  {
    return new Promise<> (settlement -> {
      try
      {
        ResourceEndpoint <T> endpoint = this.getEndpoint (dataClass);

        Field idField = dataClass.getField (FIELD_ID);
        Object id = idField.get (model);

        endpoint.update (id.toString (), model)
                .then (resolved (resource -> {
                  // Get the new value and insert it to the database. We do this just in
                  // case the update value is not the same as the value we receive from
                  // the service.
                  T newValue = resource.get (endpoint.getName ());

                  this.push (dataClass, newValue)
                      .then (resolved (settlement::resolve))
                      ._catch (rejected (settlement::reject));
                }))
                ._catch (rejected (settlement::reject));
      }
      catch (IllegalAccessException e)
      {
        throw new AssertionError (e);
      }
      catch (NoSuchFieldException e)
      {
        throw new AssertionError (e);
      }
    });
  }

  /**
   * Delete a single model from the data store.
   *
   * @param dataClass           Data class
   * @return                    Promise object
   */
  public <T extends DataModel> Promise <Boolean> delete (Class <T> dataClass, T model)
  {
    return new Promise<> (settlement -> {
      try
      {
        ResourceEndpoint <T> endpoint = this.getEndpoint (dataClass);
        Field idField = dataClass.getField (FIELD_ID);
        Object id = idField.get (model);

        endpoint.delete (id.toString ())
                .then (result -> result ? this.deleteFromDatabase (dataClass, id, model) : Promise.resolve (false))
                .then (resolved (settlement::resolve))
                ._catch (rejected (settlement::reject));
      }
      catch (NoSuchFieldException e)
      {
        settlement.reject (new AssertionError (e));
      }
      catch (IllegalAccessException e)
      {
        settlement.reject (new AssertionError (e));
      }
    });
  }

  /**
   * Helper method for deleting a model from the database.
   *
   * @param dataClass         Data class
   * @param id                Id of the object to delete
   * @param model             Model to delete
   * @return                  Promise object
   */
  private <T extends DataModel> Promise <Boolean> deleteFromDatabase (Class <T> dataClass, Object id, T model)
  {
    return new Promise<> (settlement -> {
      Operator [] condition = {Operator.op (_ID).eq (id)};

      // Delete the object from our local cache, then notify all that the model
      // has indeed been deleted.
      SQLite.delete ()
            .from (dataClass)
            .where (condition)
            .execute ();

      // Unset the data store for the model.
      model.assignTo (null);

      Uri changeUri = SqlUtils.getNotificationUri (dataClass,
                                                   BaseModel.Action.DELETE,
                                                   Arrays.asList (condition));

      FlowManager.getContext()
                 .getContentResolver()
                 .notifyChange (changeUri, null, true);

      settlement.resolve (true);
    });
  }

  /**
   * Get all the models without making a network request.
   *
   * @param dataClass           Data model class
   * @return                    Promise object
   */
  public <T extends DataModel> Promise <FlowCursor> peekCursor (Class <T> dataClass)
  {
    return new Promise<> (settlement -> {
      FlowCursor cursor =
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
    return new Promise<> (settlement -> {
      ModelAdapter <T> modelAdapter = this.getModelAdapter (dataClass);

      this.peekCursor (dataClass)
          .then (resolved (cursor -> {
            DataModelList <T> modelList = new DataModelList<> (cursor.getCount ());

            while (cursor.moveToNext ())
            {
              T model = modelAdapter.loadFromCursor (cursor);
              model.assignTo (this);

              modelList.add (model);
            }

            settlement.resolve (modelList);
          }))
          ._catch (rejected (settlement::reject));
    });
  }

  /**
   * Get the models for the data class from the local data store.
   *
   * @param dataClass       Class object
   * @param query           Query string
   * @return                Promise object
   */
  public <T extends DataModel> Promise <DataModelList <T>> select (Class <T> dataClass, Map <String, Object> query)
  {
    return new Promise<> (settlement -> {
      ModelAdapter <T> modelAdapter = this.getModelAdapter (dataClass);

      this.selectCursor (dataClass, query)
          .then (resolved (cursor -> {
            DataModelList<T> modelList = new DataModelList<> (cursor.getCount ());

            while (cursor.moveToNext ())
            {
              T model = modelAdapter.loadFromCursor (cursor);
              model.assignTo (this);

              modelList.add (model);
            }

            settlement.resolve (modelList);
          }))
          ._catch (rejected (settlement::reject));
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
      this.getModelAdapter (dataClass);

      T dataModel =
          SQLite.select ()
                .from (dataClass)
                .where (Operator.op (_ID).eq (id))
                .querySingle ();

      if (dataModel != null)
        dataModel.assignTo (this);

      settlement.resolve (dataModel);
    });
  }

  /**
   * Load the contents of a model. It is assume the id of the model already
   * exists in the target model object.
   *
   * @param model
   * @param <T>
   */
  public <T extends DataModel> Promise <T> load (T model)
  {
    return new Promise<> (settlement -> {
      @SuppressWarnings ("unchecked")
      ModelAdapter <T> modelAdapter = (ModelAdapter <T>)this.getModelAdapter (model.getClass ());
      modelAdapter.load (model);

      model.assignTo (this);

      settlement.resolve (model);
    });
  }

  /**
   * Load a model from a cursor.
   *
   * @param dataClass
   * @param cursor
   * @param <T>
   * @return
   */
  public <T extends DataModel> Promise <T> loadFromCursor (Class <T> dataClass, FlowCursor cursor)
  {
    return new Promise<> (settlement -> {
      ModelAdapter <T> modelAdapter = this.getModelAdapter (dataClass);
      T model = modelAdapter.loadFromCursor (cursor);
      model.assignTo (this);

      settlement.resolve (model);
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
  public <T extends DataModel> Promise <FlowCursor> selectCursor (Class <T> dataClass, Map <String, Object> params)
  {
    return new Promise<> (settlement -> {
      this.getModelAdapter (dataClass);

      From<?> from = SQLite.select ().from (dataClass);

      for (Map.Entry <String, Object> param: params.entrySet ())
        from.where (Operator.op (NameAlias.of (param.getKey ())).eq (param.getValue ()));

      FlowCursor cursor = from.query ();
      settlement.resolve (cursor);
    });
  }

  /**
   * Get the resource endpoint for the data class.
   *
   * @param dataClass         Class object
   * @return                  ResourceEndpoint object
   */
  private <T> ResourceEndpoint <T> getEndpoint (Class <T> dataClass)
  {
    @SuppressWarnings ("unchecked")
    ResourceEndpoint <T> endpoint = (ResourceEndpoint <T>)this.endpoints_.get (dataClass);

    if (endpoint != null)
      return endpoint;

    ModelAdapter <T> modelAdapter = this.getModelAdapter (dataClass);
    String tableName = TableUtils.getRawTableName (modelAdapter.getTableName ());
    String singular = Pluralize.getInstance ().singular (tableName);

    // Cache the endpoint for later lookup.
    endpoint = ResourceEndpoint.create (this.retrofit_, singular, tableName);
    this.endpoints_.put (dataClass, endpoint);

    return endpoint;
  }

  /**
   * Get the model adapter for the database.
   *
   * @param dataClass       Data model class
   * @return                ModelAdapter object
   */
  private <T> ModelAdapter <T> getModelAdapter (Class <T> dataClass)
  {
    @SuppressWarnings ("unchecked")
    ModelAdapter <T> modelAdapter = this.databaseDefinition_.getModelAdapterForTable (dataClass);

    if (modelAdapter != null)
      return modelAdapter;

    throw new IllegalArgumentException ("Cannot locate model adapter for " + dataClass.getName ());
  }

  /**
   * Push a single model onto the data store.
   *
   * @param dataClass         Class object
   * @param model             Model to save
   * @return                  Promise object
   */
  public <T extends DataModel> Promise <T> push (Class <T> dataClass, T model)
  {
    return new Promise<> (settlement -> {
      // Save the model to our local database.
      ModelAdapter <T> modelAdapter = FlowManager.getModelAdapter (dataClass);
      modelAdapter.save (model);

      // Set the data store for the model.
      model.assignTo (this);

      settlement.resolve (model);
    });
  }

  /**
   * Push a collection of models onto the data store.
   *
   * @param dataClass       Class object
   * @param modelList       List of model elements
   * @return                Promise object
   */
  private <T extends DataModel> Promise <DataModelList <T>> insertIntoDatabase (Class <T> dataClass, DataModelList <T> modelList)
  {
    return new Promise<> (settlement -> {
      ModelAdapter <T> modelAdapter = FlowManager.getModelAdapter (dataClass);

      FlowManager.getDatabase (this.databaseClass_)
                 .beginTransactionAsync (databaseWrapper -> {
                   for (T dataModel: modelList)
                   {
                     // Save the model to our local database, then set its data store.
                     modelAdapter.save (dataModel, databaseWrapper);
                     dataModel.assignTo (this);
                   }
                 })
                 .success (transaction -> settlement.resolve (modelList))
                 .error ((transaction, error) -> settlement.reject (error))
                 .build ()
                 .execute ();
    });
  }

  private <T> void insertDependencies (Class <T> dataClass, String root, Resource r)
  {

  }

  private interface OnNotModified
  {
    void onNotModified ();
  }

  private static <T> HandleErrorOrLoadFromCache<T> handleErrorOrLoadFromCache (Promise.Settlement <T> settlement, OnNotModified onNotModified)
  {
    return new HandleErrorOrLoadFromCache <> (settlement, onNotModified);
  }

  private static class HandleErrorOrLoadFromCache <T> implements Promise.RejectNoReturn
  {
    private Promise.Settlement <T> settlement_;

    private OnNotModified onNotModified_;

    HandleErrorOrLoadFromCache (Promise.Settlement <T> settlement, OnNotModified onNotModified)
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

          this.onNotModified_.onNotModified ();
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

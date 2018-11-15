package com.onehilltech.backbone.data;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.net.Uri;
import android.os.Bundle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapterFactory;
import com.onehilltech.backbone.data.serializers.DateTimeSerializer;
import com.onehilltech.backbone.dbflow.single.FlowModelLoader;
import com.onehilltech.promises.Promise;
import com.raizlabs.android.dbflow.config.DatabaseDefinition;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.SqlUtils;
import com.raizlabs.android.dbflow.sql.language.From;
import com.raizlabs.android.dbflow.sql.language.NameAlias;
import com.raizlabs.android.dbflow.sql.language.Operator;
import com.raizlabs.android.dbflow.sql.language.SQLOperator;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.sql.queriable.Queriable;
import com.raizlabs.android.dbflow.structure.BaseModel;
import com.raizlabs.android.dbflow.structure.ModelAdapter;
import com.raizlabs.android.dbflow.structure.database.FlowCursor;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.onehilltech.promises.Promise.await;
import static com.onehilltech.promises.Promise.resolved;

/**
 * The DataStore is an abstraction that manages different data objects. The DataStore has
 * methods for retrieving data from both local and remote storage. Data from remote storage
 * is stored locally for faster access.
 */
public class DataStore
{
  /**
   * Build a DataStore object.
   */
  public static class Builder
  {
    private Class <?> databaseClass_;

    private OkHttpClient httpClient_;

    private String baseUrl_;

    private boolean enableCache_ = true;

    /// Default cache size is 10 MB
    private int cacheSize_ = 10 * 1024 * 1024;

    private final LinkedHashMap <Type, Object> typeAdapters_ = new LinkedHashMap <> ();

    private final ArrayList <TypeAdapterFactory> typeAdapterFactories_ = new ArrayList<> ();

    private final Context context_;

    private DataStoreAdapter appAdapter_;

    private String typeDiscriminator_ = "type";

    public Builder (Context context, Class <?> databaseClass)
    {
      this.context_ = context;
      this.databaseClass_ = databaseClass;
    }

    public Builder setBaseUrl (String baseUrl)
    {
      this.baseUrl_ = baseUrl;
      return this;
    }

    public Builder setTypeDiscriminator (String discriminator)
    {
      this.typeDiscriminator_ = discriminator;
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

    public Builder setEnableCache (boolean enableCache)
    {
      this.enableCache_ = enableCache;
      return this;
    }

    public Builder setCacheSize (int cacheSize)
    {
      this.cacheSize_ = cacheSize;
      return this;
    }

    public Builder setApplicationAdapter (DataStoreAdapter adapter)
    {
      this.appAdapter_ = adapter;
      return this;
    }

    public DataStore build ()
    {
      if (this.databaseClass_ == null)
        throw new IllegalStateException ("You must set a database class");

      if (this.baseUrl_ == null)
        throw new IllegalStateException ("You must provide a base URL for the data store");

      return new DataStore (this);
    }
  }

  private static final String FIELD_ID = "_id";

  private static final NameAlias _ID = NameAlias.of (FIELD_ID);

  private final Class <?> databaseClass_;

  private final DatabaseDefinition databaseDefinition_;

  private final Retrofit retrofit_;

  private final DependencyGraph dependencyGraph_;

  private final Map <Class <?>, ResourceEndpoint <?>> endpoints_ = new LinkedHashMap<> ();

  private final Logger logger_ = LoggerFactory.getLogger (DataStore.class);

  private Cache cache_;

  private Gson gson_;

  private String typeDiscriminator_;

  private final Logger LOG = LoggerFactory.getLogger (DataStore.class);

  public interface OnModelLoaded <T>
  {
    void onModelLoaded (T model);
  }

  private DataStore (Builder builder)
  {
    this.databaseClass_ = builder.databaseClass_;
    this.typeDiscriminator_ = builder.typeDiscriminator_;
    this.databaseDefinition_ = FlowManager.getDatabase (this.databaseClass_);
    this.dependencyGraph_ = new DependencyGraph.Builder (this.databaseDefinition_).build ();

    // We need to create our own HttpClient, but we need to use the provided on
    // as the foundation. This allows us to merge the client's configuration with
    // the required configuration for the data store.
    OkHttpClient.Builder httpClientBuilder =
        builder.httpClient_ != null ?
            builder.httpClient_.newBuilder () :
            new OkHttpClient.Builder ();

    if (builder.enableCache_)
    {
      // The data store support caching.
      String cacheFileName = FlowManager.getDatabase (this.databaseClass_).getDatabaseName () + ".store.cache";
      File cacheDir = builder.context_.getCacheDir ();
      File cacheFile = new File (cacheDir, cacheFileName);

      this.cache_ = new Cache (cacheFile, builder.cacheSize_);

      httpClientBuilder.cache (this.cache_);
    }

    if (builder.appAdapter_ != null)
    {
      // The data store has an application adapter.
      httpClientBuilder.addInterceptor (chain -> {
        Request origRequest = chain.request ();
        Request newRequest = builder.appAdapter_.handleRequest (origRequest);
        return chain.proceed (newRequest);
      });
    }

    // Register the different models in the database with Gson, and then register the
    // Gson instance with the Retrofit builder.
    ResourceSerializer resourceSerializer = this.makeResourceSerializeFromDatabase ();
    resourceSerializer.put ("errors", HttpError.class);

    GsonBuilder gsonBuilder =
        new GsonBuilder ()
            .registerTypeAdapter (Resource.class, resourceSerializer)
            .registerTypeAdapter (DateTime.class, new DateTimeSerializer ())
            .registerTypeAdapterFactory (new DataModelTypeAdapterFactory ());

    for (Map.Entry <Type, Object> entry: builder.typeAdapters_.entrySet ())
      gsonBuilder.registerTypeAdapter (entry.getKey (), entry.getValue ());

    for (TypeAdapterFactory typeAdapterFactory: builder.typeAdapterFactories_)
      gsonBuilder.registerTypeAdapterFactory (typeAdapterFactory);

    this.gson_ = gsonBuilder.create ();
    resourceSerializer.setGson (this.gson_);

    // Build the Retrofit instance for the data store.
    Retrofit.Builder retrofitBuilder =
        new Retrofit.Builder ()
            .baseUrl (builder.baseUrl_);

    retrofitBuilder.client (httpClientBuilder.build ());

    this.retrofit_ =
        retrofitBuilder
            .addConverterFactory (GsonConverterFactory.create (this.gson_))
            .build ();
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
      Class<?> modelClass = modelAdapter.getModelClass ();

      serializer.put (pluralName, modelClass);

      if (!pluralName.equals (singularName))
        serializer.put (singularName, modelClass);
    }

    return serializer;
  }

  /**
   * Clear the data store cache.
   */
  public void clearCache ()
  {
    try
    {
      if (this.cache_ != null)
        this.cache_.evictAll ();
    }
    catch (IOException e)
    {
      throw new IllegalStateException ("Failed to clear cache", e);
    }
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
    return Promise.resolve (null)
                  .then (nothing -> {
                    ResourceEndpoint <T> endpoint = this.getEndpoint (dataClass);
                    Resource resource = await (endpoint.create (value));

                    // Get the new value, and associate it with this data store.
                    T newValue = resource.get (endpoint.getName ());

                    // Insert the created value in our database.
                    return this.push (dataClass, newValue);
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
    return Promise.resolve (null)
                  .then (nothing -> {
                    ResourceEndpoint<T> endpoint = this.getEndpoint (dataClass);
                    ModelAdapter<T> modelAdapter = this.getModelAdapter (dataClass);
                    String tableName = TableUtils.getRawTableName (modelAdapter.getTableName ());

                    Resource r = await (endpoint.get ());
                    await (this.insertIntoDatabase (r, dataClass));

                    DataModelList<T> modelList = r.get (tableName);

                    if (modelList == null)
                      modelList = new DataModelList<> ();

                    return modelList;
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
    return Promise.resolve (null)
                  .then (nothing -> {
                    this.getModelAdapter (dataClass);
                    ResourceEndpoint<T> endpoint = this.getEndpoint (dataClass);

                    Resource r = await (endpoint.get (id.toString ()));
                    await (this.insertIntoDatabase (r, dataClass));

                    return r.get (endpoint.getName ());
                  });
  }

  /**
   *
   * @param dataClass
   * @param id
   * @param options
   * @param <T>
   * @return
   */
  public <T extends DataModel> Promise <T> get (Class <T> dataClass, Object id, HashMap <String, Object> options)
  {
    return Promise.resolve (null)
                  .then (result -> {
                    this.getModelAdapter (dataClass);
                    ResourceEndpoint<T> endpoint = this.getEndpoint (dataClass);

                    Resource r = await (endpoint.get (id.toString (), options));
                    await (this.insertIntoDatabase (r, dataClass));

                    return r.get (endpoint.getName ());
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
    return Promise.resolve (null)
                  .then (nothing -> {
                    ResourceEndpoint<T> endpoint = this.getEndpoint (dataClass);
                    ModelAdapter<T> modelAdapter = this.getModelAdapter (dataClass);
                    String tableName = TableUtils.getRawTableName (modelAdapter.getTableName ());

                    // Insert the resources into the database. We need to account for the
                    // resource containing data for other model classes.
                    Resource r = await (endpoint.get (query));
                    await (this.insertIntoDatabase (r, dataClass));

                    DataModelList<T> modelList = r.get (tableName);

                    if (modelList == null)
                      modelList = new DataModelList<> ();

                    return modelList;
                  });
  }

  /**
   * Insert the resource into the database. We need to know the start node
   * so that we can insert the resources into the database in the correct
   * order to ensure dependencies are meet.
   *
   * @param r
   * @param startsAt
   */
  @SuppressWarnings ("unchecked")
  private Promise <Void> insertIntoDatabase (Resource r, Class <? extends DataModel> startsAt)
  {
    return new Promise<> ("data-store:insertIntoDatabase", settlement -> {
      LOG.info ("Inserting {} resources into the database", r.entityCount ());

      this.databaseDefinition_
          .beginTransactionAsync (transaction -> {
            List <DependencyGraph.Node> insertOrder = this.dependencyGraph_.getInsertOrder (startsAt);

            for (DependencyGraph.Node node: insertOrder)
            {
              if (node.isPluraleTantum () && r.contains (node.getSingularName ()))
              {
                // The plural and singular name are the same.

                this.logger_.info ("Inserting {} into the database", node.getSingularName ());

                Object value = r.get (node.getSingularName ());
                ModelAdapter modelAdapter = node.getModelAdapter ();
                Class <?> valueClass = value.getClass ();

                if (valueClass.equals (DataModel.class))
                {
                  DataModel <?> dataModel = (DataModel <?>)value;
                  this.saveModel (modelAdapter, dataModel);
                }
                else if (valueClass.equals (DataModelList.class))
                {
                  DataModelList <? extends DataModel> dataModels = (DataModelList <? extends DataModel>)value;

                  for (DataModel model: dataModels)
                    this.saveModel (modelAdapter, model);
                }
                else
                {
                  this.logger_.info ("Unexpected data model type: {}", valueClass.getName ());
                }
              }
              else
              {
                // We could have either the plural and singular name.

                if (r.contains (node.getPluralName ()))
                {
                  this.logger_.info ("Inserting {} into the database", node.getPluralName ());

                  DataModelList<? extends DataModel> dataModels = r.get (node.getPluralName ());
                  ModelAdapter modelAdapter = node.getModelAdapter ();

                  for (DataModel model : dataModels)
                    this.saveModel (modelAdapter, model);
                }

                if (r.contains (node.getSingularName ()))
                {
                  this.logger_.info ("Inserting {} into the database", node.getSingularName ());

                  DataModel dataModel = r.get (node.getSingularName ());
                  ModelAdapter modelAdapter = node.getModelAdapter ();

                  this.saveModel (modelAdapter, dataModel);
                }
              }
            }
          })
          .success (transaction -> settlement.resolve (null))
          .error ((transaction, throwable) -> settlement.reject (throwable))
          .build ()
          .execute ();
    });
  }

  @SuppressWarnings ("unchecked")
  private void saveModel (ModelAdapter modelAdapter, DataModel <?> dataModel)
  {
    modelAdapter.save (dataModel);
    dataModel.assignTo (this);
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
    return Promise.resolve (null)
                  .then (nothing -> {
                    ResourceEndpoint<T> endpoint = this.getEndpoint (dataClass);
                    ModelAdapter modelAdapter = this.getModelAdapter (dataClass);
                    String tableName = TableUtils.getRawTableName (modelAdapter.getTableName ());

                    Resource r = await (endpoint.get (query));
                    DataModelList<T> list = r.get (tableName);

                    return list != null ? this.insertIntoDatabase (dataClass, list) : null;
                  })
                  .then (resolved (ignore -> this.selectCursor (dataClass, query)));
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
    return Promise.resolve (null)
                  .then (nothing -> {
                    ResourceEndpoint<T> endpoint = this.getEndpoint (dataClass);
                    Field idField = dataClass.getField (FIELD_ID);
                    Object id = idField.get (model);

                    // Perform the update and wait for completion.
                    Resource resource = await (endpoint.update (id.toString (), model));

                    // Get the new value and insert it to the database. We do this just in
                    // case the update value is not the same as the value we receive from
                    // the service.
                    T newValue = resource.get (endpoint.getName ());
                    return this.push (dataClass, newValue);
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
    return Promise.resolve (null)
                  .then (nothing -> {
                    ResourceEndpoint <T> endpoint = this.getEndpoint (dataClass);
                    Field idField = dataClass.getField (FIELD_ID);
                    Object id = idField.get (model);

                    boolean result = await (endpoint.delete (id.toString ()));
                    return result ? this.deleteFromDatabase (dataClass, id, model) : false;
                  });
  }

  /**
   * Remove a model from the local database.
   *
   * @param dataClass       Data class
   * @param model           Model to remove
   * @param <T>
   * @return
   * @throws NoSuchFieldException
   * @throws IllegalAccessException
   */
  public <T extends DataModel> Promise <Boolean> remove (Class <T> dataClass, T model) {
    return Promise.resolve (null)
                  .then (nothing -> {
                    Field idField = dataClass.getField (FIELD_ID);
                    return idField.get (model);
                  })
                  .then (id -> this.deleteFromDatabase (dataClass, id, model));
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
    return Promise.resolve (null)
                  .then (nothing -> {
                    ModelAdapter<T> modelAdapter = this.getModelAdapter (dataClass);
                    FlowCursor cursor = await (this.peekCursor (dataClass));
                    DataModelList<T> modelList = new DataModelList<> (cursor.getCount ());

                    while (cursor.moveToNext ())
                    {
                      T model = modelAdapter.loadFromCursor (cursor);
                      model.assignTo (this);

                      modelList.add (model);
                    }

                    return modelList;
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
    return Promise.resolve (null)
                  .then (nothing -> {
                    ModelAdapter <T> modelAdapter = this.getModelAdapter (dataClass);
                    FlowCursor cursor = await (this.selectCursor (dataClass, query));
                    DataModelList<T> modelList = new DataModelList<> (cursor.getCount ());

                    while (cursor.moveToNext ())
                    {
                      T model = modelAdapter.loadFromCursor (cursor);
                      model.assignTo (this);

                      modelList.add (model);
                    }

                    return modelList;
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
    return Promise.resolve (null)
                  .then (nothing -> {
                    this.getModelAdapter (dataClass);

                    T dataModel =
                        SQLite.select ()
                              .from (dataClass)
                              .where (Operator.op (_ID).eq (id))
                              .querySingle ();

                    if (dataModel != null)
                      dataModel.assignTo (this);

                    return dataModel;
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
    return Promise.resolve (null)
                  .then (nothing -> {
                    @SuppressWarnings("unchecked")
                    ModelAdapter<T> modelAdapter = (ModelAdapter<T>) this.getModelAdapter (model.getClass ());
                    modelAdapter.load (model);

                    model.assignTo (this);

                    return model;
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
    ModelAdapter <T> modelAdapter = this.getModelAdapter (dataClass);

    T model = modelAdapter.loadFromCursor (cursor);
    model.assignTo (this);

    return Promise.resolve (model);
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
    return Promise.resolve (null)
                  .then (nothing -> {
                    this.getModelAdapter (dataClass);

                    From<?> from = SQLite.select ().from (dataClass);

                    ArrayList<SQLOperator> conditions = new ArrayList<> ();

                    for (Map.Entry<String, Object> param : params.entrySet ())
                      conditions.add (Operator.op (NameAlias.of (param.getKey ())).eq (param.getValue ()));

                    Queriable queriable = conditions.isEmpty () ? from : from.where (conditions.toArray (new SQLOperator[0]));
                    return queriable.query ();
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
    return Promise.resolve (null)
                  .then (nothing -> {
                    LOG.info ("Pushing data model onto the database [class={}]", dataClass);

                    // Save the model to our local database.
                    ModelAdapter <T> modelAdapter = FlowManager.getModelAdapter (dataClass);
                    modelAdapter.save (model);

                    // Set the data store for the model.
                    model.assignTo (this);

                    return model;
                  });
  }

  /**
   * Push a data object onto the store.
   *
   * @param dataClass       Class object
   * @param data            Map containing the objects data
   * @return                Promise object
   */
  public <T extends DataModel> Promise <T> push (Class <T> dataClass, Map <String, String> data)
  {
    return Promise.resolve (null)
                  .then (nothing -> {
                    LOG.info ("Pushing data onto the database [class={}]", dataClass);

                    // Locate the model adapter.
                    ModelAdapter<T> modelAdapter = this.getModelAdapter (dataClass);

                    // Convert the data map into a model.
                    JsonObject json = (JsonObject) this.gson_.toJsonTree (data);
                    T model = this.gson_.fromJson (json, modelAdapter.getModelClass ());

                    // Save the model to our local database.
                    modelAdapter.save (model);
                    model.assignTo (this);

                    return model;
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
      LOG.info ("Inserting {} model(s) into database [class={}]", modelList.size (), dataClass);

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
}

package com.onehilltech.backbone.http.retrofit;

import com.onehilltech.backbone.http.HttpError;
import com.onehilltech.backbone.http.Resource;
import com.onehilltech.promises.Promise;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

/**
 * Endpoint for managing a resource type.
 *
 * @param <T>
 */
public class ResourceEndpoint <T>
{
  /// Name of the resource
  private final String name_;

  /// Path for the resource.
  private final String path_;

  /// Methods for interacting with the resource
  private final ResourceEndpoint.Methods methods_;

  private final Retrofit retrofit_;

  private final Converter<ResponseBody, Resource> resourceConverter_;

  /**
   * Create a new instance of the resource endpoint.
   *
   * @param retrofit        Retrofit instance
   * @param name            Name of the resource
   * @return
   */
  public static <T> ResourceEndpoint<T> create (Retrofit retrofit, String name)
  {
    return create (retrofit, name, name);
  }

  /**
   * Create a new instance of the resource endpoint.
   *
   * @param retrofit        Retrofit instance
   * @param name            Name of the resource
   * @param path            Absolute/relative path to the resource
   * @return
   */
  public static <T> ResourceEndpoint<T> create (Retrofit retrofit, String name, String path)
  {
    return new ResourceEndpoint<> (retrofit, name, path);
  }

  /**
   * Initializing constructor.
   *
   * @param name            Name of the resource
   * @param path            Absolute/relative path to the resource
   */
  private ResourceEndpoint (Retrofit retrofit, String name, String path)
  {
    this.retrofit_ = retrofit;
    this.methods_ = this.retrofit_.create (ResourceEndpoint.Methods.class);
    this.resourceConverter_ = this.retrofit_.responseBodyConverter (Resource.class, new Annotation[0]);

    this.name_ = name;
    this.path_ = path;
  }

  /**
   * Get the name of the resource.
   *
   * @return
   */
  public String getName ()
  {
    return this.name_;
  }

  /**
   * Create a new resource in the endpoint.
   *
   * @param obj       Resource values
   * @return
   */
  public Promise<Resource> create (T obj)
  {
    Call <Resource> call = this.methods_.create (this.path_, new Resource (this.name_, obj));
    return this.executeCall (call);
  }

  /**
   * Create a new resource, passing optional query parameters.
   *
   * @param obj
   * @param query
   * @return
   */
  public Promise<Resource> create (T obj, HashMap <String, Object> query)
  {
    return new Promise<> ((settlement) -> {
      this.methods_.create (this.path_, new Resource (this.name_, obj), query).enqueue (new Callback<Resource> ()
      {
        @Override
        public void onResponse (Call<Resource> call, Response<Resource> response)
        {
          if (response.isSuccessful ())
          {
            settlement.resolve (response.body ());
          }
          else
          {
            settlement.reject (new IllegalStateException (response.message ()));
          }
        }

        @Override
        public void onFailure (Call<Resource> call, Throwable t)
        {
          settlement.reject (t);
        }
      });
    });
  }

  /**
   * Get a single resource by its id.
   *
   * @param id
   * @return
   */
  public Promise<Resource> get (String id)
  {
    Call <Resource> call = this.methods_.get (this.path_, id);
    return this.executeCall (call);
  }

  public Promise<Resource> get (String id, HashMap <String, Object> query)
  {
    Call <Resource> call = this.methods_.get (this.path_, id, query);
    return this.executeCall (call);
  }

  /**
   * Get all the resources
   *
   * @return
   */
  public Promise<Resource> get ()
  {
    Call <Resource> call = this.methods_.get (this.path_);
    return this.executeCall (call);
  }

  /**
   * Get all the resources that match the specified query string.
   *
   * @param params
   * @return
   */
  public Promise<Resource> get (Map <String, Object> params)
  {
    Call <Resource> call = this.methods_.get (this.path_, params);
    return this.executeCall (call);
  }

  /**
   * Update an existing resource.
   *
   * @param id
   * @param value
   * @return
   */
  public Promise<Resource> update (String id, T value)
  {
    Call<Resource> call = this.methods_.update (this.path_, id, new Resource (this.name_, value));
    return this.executeCall (call);
  }

  /**
   * Delete a resource.
   *
   * @param id
   * @return
   */
  public Promise<Boolean> delete (String id)
  {
    Call <Boolean> call = this.methods_.delete (this.path_, id);
    return this.executeCall (call);
  }

  /**
   * Retrieve the number of resources.
   *
   * @return
   */
  public Promise<Resource> count ()
  {
    Call <Resource> call = this.methods_.count (this.path_);
    return this.executeCall (call);
  }

  /**
   * Retrieve the number of resources that match the specified criteria.
   *
   * @param query
   * @return
   */
  public Promise<Resource> count (Map <String, Object> query)
  {
    Call <Resource> call = this.methods_.count (this.path_, query);
    return this.executeCall (call);
  }

  private <T> Promise <T> executeCall (Call <T> call)
  {
    return new Promise<> (settlement ->
      call.enqueue (new Callback<T> ()
      {
        @Override
        public void onResponse (Call<T> call, Response<T> response)
        {
          if (response.isSuccessful ())
          {
            settlement.resolve (response.body ());
          }
          else if (response.code () == 304)
          {
            HttpError httpError = new HttpError ("NotModified", "Not Modified");
            httpError.setStatusCode (response.code ());

            settlement.reject (httpError);
          }
          else
          {
            try
            {
              // Get the errors from the response message.
              Resource r = resourceConverter_.convert (response.errorBody ());
              HttpError httpError = r.get ("errors");
              httpError.setStatusCode (response.code ());

              settlement.reject (httpError);
            }
            catch (IOException e)
            {
              settlement.reject (e);
            }
          }
        }

        @Override
        public void onFailure (Call<T> call, Throwable t)
        {
          settlement.reject (t);
        }
      })
    );
  }

  /**
   * @interface Methods
   *
   * Methods for managing a resource in Retrofit. The ResourceEndpoint uses
   * Retrofit to instantiate this interface.
   */
  interface Methods
  {
    /**
     * Create a new resource.
     *
     * @param name
     * @param rc
     * @return
     */
    @POST("{name}")
    Call<Resource> create (@Path ("name") String name, @Body Resource rc);

    /**
     * Create a new resource.
     *
     * @param name
     * @param rc
     * @param options
     * @return
     */
    @POST("{name}")
    Call<Resource> create (@Path ("name") String name, @Body Resource rc,  @QueryMap(encoded = true) Map<String, Object> options);

    /**
     * Query a list of resources.
     *
     * @param name
     * @return
     */
    @GET("{name}")
    Call<Resource> get (@Path("name") String name);

    /**
     * Query a list of resources.
     *
     * @param name
     * @param options
     * @return
     */
    @GET("{name}")
    Call<Resource> get (@Path("name") String name, @QueryMap(encoded = true) Map<String, Object> options);

    /**
     * Get a single resources.
     *
     * @param name
     * @return
     */
    @GET("{name}/{id}")
    Call<Resource> get (@Path("name") String name, @Path("id") String id);

    /**
     * Get a single resource.
     *
     * @param name
     * @param id
     * @param options
     * @return
     */
    @GET("{name}/{id}")
    Call<Resource> get (@Path("name") String name, @Path("id") String id, @QueryMap(encoded = true) Map<String, Object> options);

    /**
     * Update an existing resource.
     *
     * @param name
     * @param id
     * @param rc
     * @return
     */
    @PUT("{name}/{id}")
    Call<Resource> update (@Path("name") String name, @Path("id") String id, @Body Resource rc);

    @DELETE("{name}/{id}")
    Call<Boolean> delete (@Path("name") String name, @Path("id") String id);

    /**
     * Retrieve the number of resources from the server.
     *
     * @return
     */
    @GET("{name}/count")
    Call <Resource> count (@Path("name") String name);

    /**
     * Get the number of resources that match the specified query.
     *
     * @param query
     * @return
     */
    @GET("{name}/count")
    Call <Resource> count (@Path("name") String name, @QueryMap(encoded = true) Map <String, Object> query);
  }
}

package com.onehilltech.backbone.http.retrofit;

import com.onehilltech.backbone.http.Resource;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
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
    ResourceEndpoint.Methods methods = retrofit.create (ResourceEndpoint.Methods.class);
    return new ResourceEndpoint<> (methods, name, path);
  }

  /**
   * Initializing constructor.
   *
   * @param methods         Instantiated Methods interface
   * @param name            Name of the resource
   * @param path            Absolute/relative path to the resource
   */
  private ResourceEndpoint (ResourceEndpoint.Methods methods, String name, String path)
  {
    this.methods_ = methods;
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
  public Call<Resource> create (T obj)
  {
    return this.methods_.create (this.path_, new Resource (this.name_, obj));
  }

  public Call<Resource> get (String id)
  {
    return this.methods_.get (this.path_, id);
  }

  public Call<Resource> get (String id, HashMap <String, Object> query)
  {
    return this.methods_.get (this.path_, id, query);
  }

  /**
   * Get all the resources
   *
   * @return
   */
  public Call<Resource> get ()
  {
    return this.methods_.get (this.path_);
  }

  /**
   * Get all the resources that match the specified query string.
   *
   * @param params
   * @return
   */
  public Call<Resource> get (Map <String, Object> params)
  {
    return this.methods_.get (this.path_, params);
  }

  /**
   * Update an existing resource.
   *
   * @param id
   * @param value
   * @return
   */
  public Call<Resource> update (String id, T value)
  {
    return this.methods_.update (this.path_, id, new Resource (this.name_, value));
  }

  /**
   * Delete a resource.
   *
   * @param id
   * @return
   */
  public Call<Boolean> delete (String id)
  {
    return this.methods_.delete (this.path_, id);
  }

  /**
   * Retrieve the number of resources.
   *
   * @return
   */
  public Call<Resource> count ()
  {
    return this.methods_.count (this.path_);
  }

  /**
   * Retrieve the number of resources that match the specified criteria.
   *
   * @param query
   * @return
   */
  public Call<Resource> count (Map <String, Object> query)
  {
    return this.methods_.count (this.path_, query);
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
    Call<Resource> create (@Path("name") String name, @Body Resource rc);

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

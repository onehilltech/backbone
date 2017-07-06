package com.onehilltech.backbone.data;

import java.util.HashMap;

/**
 * @class GsonResourceManager
 *
 * Resource manager for the Gson serialization library.
 */
public class GsonResourceManager
{
  private static GsonResourceManager instance_;

  private final HashMap<String, ElementAdapter> adapters_ = new HashMap<> ();

  /**
   * Get the singleton instance.
   *
   * @return
   */
  public static GsonResourceManager getInstance ()
  {
    if (instance_ != null)
      return instance_;

    instance_ = new GsonResourceManager ();
    return instance_;
  }

  /**
   * Create a new resource manager.
   */
  public GsonResourceManager ()
  {

  }

  /**
   * Register a resource type.
   *
   * @param name
   * @param adapter
   */
  public void registerType (String name, ElementAdapter adapter)
  {
    this.adapters_.put (name, adapter);
  }

  /**
   * Get a registered resource type.
   *
   * @param name
   * @return
   */
  public ElementAdapter getAdapter (String name)
  {
    return this.adapters_.get (name);
  }
}

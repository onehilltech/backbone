package com.onehilltech.backbone.data;

import java.util.HashMap;

/**
 * @class ElementAdapterManager
 *
 * Resource manager for the Gson serialization library.
 */
public class ElementAdapterManager
{
  private static ElementAdapterManager instance_;

  private final HashMap<String, ElementAdapter> adapters_ = new HashMap<> ();

  /**
   * Get the singleton instance.
   *
   * @return
   */
  public static ElementAdapterManager getInstance ()
  {
    if (instance_ != null)
      return instance_;

    instance_ = new ElementAdapterManager ();
    return instance_;
  }

  /**
   * Create a new resource manager.
   */
  public ElementAdapterManager ()
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

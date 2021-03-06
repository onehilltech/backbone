package com.onehilltech.backbone.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @class Resource
 *
 * Resource message sent to/from the server.
 */
public class Resource
{
  /// Collection of values in the resource.
  private final HashMap <String, Object> values_ = new HashMap<> ();

  /**
   * Default constructor.
   */
  public Resource ()
  {

  }

  /**
   * Create a single value resource.
   *
   * @param name
   * @param value
   */
  public Resource (String name, Object value)
  {
    this.values_.put (name, value);
  }

  /**
   * Create a multi-value resource.
   *
   * @param values
   */
  public Resource (Map<String, Object> values)
  {
    this.values_.putAll (values);
  }

  /**
   * Add a new value to the resource.
   *
   * @param name
   * @param value
   */
  public void add (String name, Object value)
  {
    this.values_.put (name, value);
  }

  @SuppressWarnings ("unchecked")
  public <T> T get (String name)
  {
    return (T)this.values_.get (name);
  }

  /**
   * Get the entries in the resource.
   *
   * @return
   */
  public Set<Map.Entry <String, Object>> entitySet ()
  {
    return this.values_.entrySet ();
  }

  public boolean contains (String name)
  {
    return this.values_.containsKey (name);
  }

  /**
   * Get the number of entities in the resource.
   *
   * @return
   */
  public int entityCount ()
  {
    return this.values_.size ();
  }
}

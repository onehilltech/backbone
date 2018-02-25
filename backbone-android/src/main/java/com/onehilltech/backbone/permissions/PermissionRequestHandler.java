package com.onehilltech.backbone.permissions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Utility class that allows you to register handler for specific permissions. This
 * is an alternative to iterating over the set of granted and denied permissions.
 */
public class PermissionRequestHandler
    implements PermissionGranter.OnPermissionsResult
{
  public interface OnPermissionGrantedCallback
  {
    void onPermissionGranted (PermissionGranter granter, Permission permission);
  }

  public interface OnPermissionDeniedCallback
  {
    void onPermissionDenied (PermissionGranter granter, Permission permission);
  }

  public interface OnPermissionResultCallback
  {
    void onPermissionResult (Set<Permission> granted, Set<Permission> denied);
  }

  /// Collection of granted callback handlers mapped to permission strings.
  private final HashMap <String, OnPermissionGrantedCallback> grantedHandler_ = new HashMap<> ();

  /// Collection of denied callback handlers mapped to permission strings.
  private final HashMap <String, OnPermissionDeniedCallback> deniedHandler_ = new HashMap<> ();

  private final ArrayList <OnPermissionResultCallback> onPermissionResultCallbacks_ = new ArrayList<> ();

  /**
   * Add a granted callback handler for a permission.
   *
   * @param permission          
   * @param callback
   */
  public void onGranted (String permission, OnPermissionGrantedCallback callback)
  {
    this.grantedHandler_.put (permission, callback);
  }

  /**
   * Add a denied callback handler for a permission.
   *
   * @param permission
   * @param callback
   */
  public void onDenied (String permission, OnPermissionDeniedCallback callback)
  {
    this.deniedHandler_.put (permission, callback);
  }

  public void addOnPermissionResultCallback (OnPermissionResultCallback callback)
  {
    this.onPermissionResultCallbacks_.add (callback);
  }

  public boolean removeOnPermissionResultCallback (OnPermissionResultCallback callback)
  {
    return this.onPermissionResultCallbacks_.remove (callback);
  }

  @Override
  public void onPermissionsResult (PermissionGranter granter, Set<Permission> granted, Set<Permission> denied)
  {
    // Call the generic handler.

    for (OnPermissionResultCallback permissionResultCallback: this.onPermissionResultCallbacks_)
      permissionResultCallback.onPermissionResult (granted, denied);

    // Call the permission granted handlers.

    for (Permission permission: granted)
    {
      OnPermissionGrantedCallback handler = this.grantedHandler_.get (permission.name);

      if (handler != null)
        handler.onPermissionGranted (granter, permission);
    }

    // Call the permission denied handlers.

    for (Permission permission: denied)
    {
      OnPermissionDeniedCallback handler = this.deniedHandler_.get (permission.name);

      if (handler != null)
        handler.onPermissionDenied (granter, permission);
    }
  }
}

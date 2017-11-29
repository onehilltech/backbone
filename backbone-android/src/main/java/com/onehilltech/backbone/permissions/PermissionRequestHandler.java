package com.onehilltech.backbone.permissions;

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

  /// Collection of granted callback handlers mapped to permission strings.
  private final HashMap <String, OnPermissionGrantedCallback> grantedHandler_ = new HashMap<> ();

  /// Collection of denied callback handlers mapped to permission strings.
  private final HashMap <String, OnPermissionDeniedCallback> deniedHandler_ = new HashMap<> ();

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

  @Override
  public void onPermissionsResult (PermissionGranter granter, Set<Permission> granted, Set<Permission> denied)
  {
    for (Permission permission: granted)
    {
      OnPermissionGrantedCallback handler = this.grantedHandler_.get (permission.name);

      if (handler != null)
        handler.onPermissionGranted (granter, permission);
    }

    for (Permission permission: denied)
    {
      OnPermissionDeniedCallback handler = this.deniedHandler_.get (permission.name);

      if (handler != null)
        handler.onPermissionDenied (granter, permission);
    }
  }
}

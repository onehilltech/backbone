package com.onehilltech.backbone.permissions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Permission request handler that requires all the requested permissions
 * to be granted for the request to be considered granted.
 */
public class AllPermissionsRequestHandler implements PermissionRequestHandler.OnPermissionResultCallback
{
  private final ArrayList <String> permissions_ = new ArrayList<> ();

  private final OnPermissionsGranted onPermissionsGranted_;

  /**
   * Callback for all permissions granted.
   */
  public interface OnPermissionsGranted
  {
    void onPermissionGranted ();
  }

  /**
   * Initializing constructor.
   *
   * @param permissions               Set of permissions
   * @param onPermissionsGranted       Callback for permissions granted
   */
  public AllPermissionsRequestHandler (List <String> permissions, OnPermissionsGranted onPermissionsGranted)
  {
    this.permissions_.addAll (permissions);
    this.onPermissionsGranted_ = onPermissionsGranted;
  }

  @Override
  public void onPermissionResult (Set<Permission> granted, Set<Permission> denied)
  {
    for (String expected: this.permissions_)
    {
      boolean found = false;

      for (Permission permission: granted)
      {
        if (permission.name.equals (expected))
        {
          found = true;
          break;
        }
      }

      // If we did not find the permission, then we need to just return.
      if (!found)
        return;
    }

    // Since we did not leave the function, this means we have found
    // all the permissions we requested.
    this.onPermissionsGranted_.onPermissionGranted ();
  }
}

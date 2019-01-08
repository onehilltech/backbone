package com.onehilltech.backbone.permissions;

import java.util.ArrayList;
import java.util.HashSet;
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

  private final OnPermissionsDenied onPermissionsDenied_;

  /**
   * Initializing constructor.
   *
   * @param permissions           Set of permissions
   * @param granted               Callback for permissions granted
   *
   */
  public AllPermissionsRequestHandler (List <String> permissions, OnPermissionsGranted granted, OnPermissionsDenied denied)
  {
    this.permissions_.addAll (permissions);

    this.onPermissionsGranted_ = granted;
    this.onPermissionsDenied_ = denied;
  }

  @Override
  public void onPermissionResult (Set<Permission> granted, Set<Permission> denied)
  {
    Set <Permission> found = new HashSet<> ();

    for (String expected: this.permissions_)
    {
      for (Permission permission: granted)
      {
        if (permission.name.equals (expected))
        {
          found.add (permission);
          break;
        }
      }
    }

    // Since we did not leave the function, this means we have found
    // all the permissions we requested.
    if (found.size () == this.permissions_.size ())
      this.onPermissionsGranted_.onPermissionsGranted (found);
    else
      this.onPermissionsDenied_.onPermissionsDenied (denied);
  }
}

package com.onehilltech.backbone.permissions;

import java.util.HashSet;
import java.util.Set;

class PermissionRequest
{
  private final int id_;

  private final PermissionGranter.OnPermissionsResult callback_;

  private final HashSet<Permission> permissions_ = new HashSet<> ();

  private final HashSet<Permission> denied_ = new HashSet<> ();

  PermissionRequest (int id, PermissionGranter.OnPermissionsResult callback, Set<Permission> permissions, Set<Permission> denied)
  {
    this.id_ = id;
    this.callback_ = callback;
    this.permissions_.addAll (permissions);
    this.denied_.addAll (denied);
  }

  public int getId ()
  {
    return this.id_;
  }

  Permission getRequestedPermission (String name)
  {
    for (Permission permission : this.denied_)
    {
      if (permission.name.equals (name))
        return permission;
    }

    return null;
  }

  public Set <Permission> getPermissions ()
  {
    return this.permissions_;
  }

  public String [] getRequestedPermissions ()
  {
    // Get an array of permission strings we are requested.
    String [] requested = new String[this.denied_.size ()];
    int i = 0;

    for (Permission permission: this.denied_)
      requested[i ++] = permission.name;

    return requested;
  }

  public PermissionGranter.OnPermissionsResult getCallback ()
  {
    return this.callback_;
  }
}

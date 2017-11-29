package com.onehilltech.backbone.permissions;

import java.util.HashSet;
import java.util.Set;

class PermissionRequest
{
  private final int id_;

  private PermissionGranter.OnPermissionsResult callback_;

  private HashSet<Permission> permissions_ = new HashSet<> ();

  private HashSet<Permission> requested_ = new HashSet<> ();

  PermissionRequest (int id, PermissionGranter.OnPermissionsResult callback, Set<Permission> permissions, Set<Permission> requested)
  {
    this.id_ = id;
    this.callback_ = callback;
    this.permissions_.addAll (permissions);
    this.requested_.addAll (requested);
  }

  Permission getRequestedPermission (String name)
  {
    for (Permission permission : this.requested_)
    {
      if (permission.name.equals (name))
        return permission;
    }

    return null;
  }

  public PermissionGranter.OnPermissionsResult getCallback ()
  {
    return this.callback_;
  }
}

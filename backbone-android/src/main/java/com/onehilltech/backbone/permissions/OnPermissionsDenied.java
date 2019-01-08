package com.onehilltech.backbone.permissions;

import java.util.Set;

/**
 * Callback for one or more permissions is denied.
 */
public interface OnPermissionsDenied
{
  void onPermissionsDenied (Set <Permission> denied);
}

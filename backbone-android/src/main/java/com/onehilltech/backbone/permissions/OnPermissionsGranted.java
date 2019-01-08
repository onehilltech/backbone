package com.onehilltech.backbone.permissions;

import java.util.Set;

/**
 * Callback for all permissions granted.
 */
public interface OnPermissionsGranted
{
  void onPermissionGranted (Set <Permission> granted);
}

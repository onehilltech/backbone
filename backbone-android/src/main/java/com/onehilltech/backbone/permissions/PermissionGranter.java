package com.onehilltech.backbone.permissions;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.SparseArray;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PermissionGranter
{
  public interface OnPermissionsResult
  {
    void onPermissionsResult (PermissionGranter granter, Set <Permission> granted, Set <Permission> denied);
  }

  private static PermissionGranter instance_;

  private int nextRequestId_;

  private final SparseArray<PermissionRequest> requests_ = new SparseArray<> ();

  private static final Set <Permission> EMPTY_PERMISSION_SET = Collections.unmodifiableSet (new HashSet<> ());

  public static PermissionGranter getInstance ()
  {
    if (instance_ != null)
      return instance_;

    instance_ = new PermissionGranter ();
    return instance_;
  }

  public static int checkPermission (Context context, String permission)
  {
    return ContextCompat.checkSelfPermission (context, permission);
  }

  public void requestPermission (Activity activity, Permission permission, OnPermissionsResult callback)
  {
    HashSet<Permission> permissions = new HashSet<> ();
    permissions.add (permission);

    this.requestPermissions (activity, permissions, callback);
  }

  public void requestPermissions (Activity activity, Set<Permission> permissions, OnPermissionsResult callback)
  {
    HashSet<Permission> denied = new HashSet<> ();

    // First, we need to determine what permissions have been granted, and what
    // permissions have not been granted.
    for (Permission permission : permissions)
    {
      int result = ContextCompat.checkSelfPermission (activity, permission.name);

      if (result == PackageManager.PERMISSION_DENIED)
        denied.add (permission);
    }

    if (denied.isEmpty ())
    {
      // All the requested permissions have been granted. We can just
      // notify the callback, and continue with regular business.
      callback.onPermissionsResult (this, permissions, EMPTY_PERMISSION_SET);
    }
    else
    {
      // Should we show an explanation?
      //if (ActivityCompat.shouldShowRequestPermissionRationale (activity, permission.name)) {

        // Show an explanation to the user *asynchronously* -- don't block
        // this thread waiting for the user's response! After the user
        // sees the explanation, try again to request the permission.

      //}

      //else
      {
        // Get an array of permission strings we are requested.
        String [] requested = new String[denied.size ()];
        int i = 0;

        for (Permission permission: denied)
          requested[i ++] = permission.name;

        // Create a new permission request object, and then make the request. We are going
        // to use the request id to lookup this permission request when the activity receives
        // an answer.
        int requestId = this.nextRequestId_ ++;
        PermissionRequest request = new PermissionRequest (requestId, callback, permissions, denied);

        this.requests_.put (requestId, request);

        ActivityCompat.requestPermissions (activity, requested, requestId);
      }
    }
  }

  public void onRequestPermissionsResult (int requestCode, String [] permissions, int[] results)
  {
    PermissionRequest request = this.requests_.get (requestCode);

    if (request == null)
      throw new IllegalArgumentException ("The request code does not exist");

    HashSet <Permission> granted = new HashSet<> ();
    HashSet <Permission> denied = new HashSet <> ();

    for (int i = 0, length = permissions.length; i < length; ++ i)
    {
      String name = permissions[i];
      int result = results[i];
      Permission permission = request.getRequestedPermission (name);

      if (result == PackageManager.PERMISSION_GRANTED)
        granted.add (permission);
      else
        denied.add (permission);
    }

    // Notify the client of the permissions that were granted and denied.
    request.getCallback ().onPermissionsResult (this, granted, denied);

    this.requests_.remove (requestCode);
  }
}

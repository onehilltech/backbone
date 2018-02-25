package com.onehilltech.backbone.permissions;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v13.app.FragmentCompat;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.SparseArray;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @class PermissionGranter
 *
 * Manages the process for requesting permissions. The PermissionGranter allows clients
 * to request a set of permissions. If any of the permissions are denied, it will request
 * the permissions from the user. Upon receiving the result of a permission request, the
 * granter will then process the requests.
 *
 * The PermissionGranter requires the user to register a callback object, which is called
 * when the Activity or Fragment receives the results of a permission request.
 *
 * @see PermissionRequestHandler
 */
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

  /**
   * Get the singleton instance.
   *
   * @return    A PermissionGranter object.
   */
  public static PermissionGranter getInstance ()
  {
    if (instance_ != null)
      return instance_;

    instance_ = new PermissionGranter ();
    return instance_;
  }

  /**
   * Check the status of a permission.
   *
   * @param context
   * @param permission
   * @return
   */
  public static int checkPermission (Context context, String permission)
  {
    return ContextCompat.checkSelfPermission (context, permission);
  }

  /**
   * Request permissions from an Activity.
   *
   * @param activity
   * @param permission
   * @param callback
   */
  public void requestPermission (Activity activity, Permission permission, OnPermissionsResult callback)
  {
    HashSet<Permission> permissions = new HashSet<> ();
    permissions.add (permission);

    this.requestPermissions (activity, permissions, callback);
  }

  public void requestPermissions (Activity activity, Set <Permission> permissions, OnPermissionsResult callback)
  {
    this.requestPermissions (new ActivityPermissionImpl (activity), permissions, callback);
  }

  /**
   * Request permissions from a Fragment.
   *
   * @param fragment
   * @param permission
   * @param callback
   */
  public void requestPermission (Fragment fragment, Permission permission, OnPermissionsResult callback)
  {
    HashSet<Permission> permissions = new HashSet<> ();
    permissions.add (permission);

    this.requestPermissions (fragment, permissions, callback);
  }

  public void requestPermissions (Fragment fragment, Set <Permission> permissions, OnPermissionsResult callback)
  {
    this.requestPermissions (new FragmentPermissionImpl (fragment), permissions, callback);
  }

  /**
   * Implementation for requesting the permissions.
   *
   * @param impl
   * @param permissions
   * @param callback
   */
  private void requestPermissions (PermissionImpl impl, Set<Permission> permissions, OnPermissionsResult callback)
  {
    Set<Permission> denied = this.getDeniedPermissions (impl.getActivity (), permissions);

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
        // Create a new permission request object, and then make the request.
        PermissionRequest request = this.newPermissionRequest (permissions, denied, callback);
        impl.requestPermission (request);
      }
    }
  }

  /**
   * Handle the result of a permission request. This method should be called from either
   * the Activity or Fragment that made the original permission request.
   *
   * @param requestCode         The request id
   * @param permissions         Set of permissions
   * @param results             Result for each permission
   */
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

  /**
   * Get the set of permissions from the list of permissions that are denied.
   *
   * @param activity
   * @param permissions
   * @return
   */
  private Set <Permission> getDeniedPermissions (Activity activity, Set <Permission> permissions)
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

    return denied;
  }

  /**
   * Factory method responsible for creating a new PermissionRequest object.
   *
   * @param permissions       Set of requested permissions
   * @param denied            Set of denied permissions
   * @param callback          Callback object
   * @return                  A PermissionRequest object.
   */
  private PermissionRequest newPermissionRequest (Set<Permission> permissions, Set <Permission> denied, OnPermissionsResult callback)
  {
    // Create a new permission request object, and then make the request. We are going
    // to use the request id to lookup this permission request when the activity receives
    // an answer.
    int requestId = this.nextRequestId_ ++;
    PermissionRequest request = new PermissionRequest (requestId, callback, permissions, denied);

    this.requests_.put (requestId, request);

    return request;
  }

  interface PermissionImpl
  {
    void requestPermission (PermissionRequest request);

    Activity getActivity ();
  }

  private static class ActivityPermissionImpl implements PermissionImpl
  {
    private final Activity activity_;

    ActivityPermissionImpl (Activity activity)
    {
      this.activity_ = activity;
    }

    @Override
    public Activity getActivity ()
    {
      return this.activity_;
    }

    @Override
    public void requestPermission (PermissionRequest request)
    {
      String [] permissions = request.getRequestedPermissions ();
      int id = request.getId ();

      ActivityCompat.requestPermissions (this.activity_, permissions, id);
    }
  }

  private static class FragmentPermissionImpl implements PermissionImpl
  {
    private final Fragment fragment_;

    FragmentPermissionImpl (Fragment fragment)
    {
      this.fragment_ = fragment;
    }

    @Override
    public Activity getActivity ()
    {
      return this.fragment_.getActivity ();
    }

    @Override
    public void requestPermission (PermissionRequest request)
    {
      String [] permissions = request.getRequestedPermissions ();
      int id = request.getId ();

      FragmentCompat.requestPermissions (this.fragment_, permissions, id);
    }
  }
}

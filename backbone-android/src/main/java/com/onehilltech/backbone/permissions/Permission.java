package com.onehilltech.backbone.permissions;

/**
 * Created by hilljh on 11/29/17.
 */
public class Permission
{
  public String name;

  public String reason;

  public Permission (String name)
  {
    this (name, null);
  }

  public Permission (String name, String reason)
  {
    this.name = name;
    this.reason = reason;
  }

  @Override
  public boolean equals (Object obj)
  {
    if (!(obj instanceof Permission))
      return false;

    Permission permission = (Permission) obj;
    return permission.name.equals (this.name);
  }

  @Override
  public int hashCode ()
  {
    return this.name.hashCode ();
  }
}

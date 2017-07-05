package com.onehilltech.backbone.data;

public class Pluralize
{
  public static String singular (String s)
  {
    if (s.endsWith ("es"))
      return s.substring (0, s.length () - 2);
    else if (s.endsWith ("s"))
      return s.substring (0, s.length () - 1);
    else
      return s;
  }
}

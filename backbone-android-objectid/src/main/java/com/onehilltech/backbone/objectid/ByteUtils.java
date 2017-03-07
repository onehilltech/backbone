package com.onehilltech.backbone.objectid;

public class ByteUtils
{
  public static byte int3(final int x)
  {
    return (byte) (x >> 24);
  }

  public static byte int2(final int x)
  {
    return (byte) (x >> 16);
  }

  public static byte int1(final int x)
  {
    return (byte) (x >> 8);
  }

  public static byte int0(final int x)
  {
    return (byte) (x);
  }

  public static byte short1(final short x)
  {
    return (byte) (x >> 8);
  }

  public static byte short0(final short x)
  {
    return (byte) (x);
  }
}

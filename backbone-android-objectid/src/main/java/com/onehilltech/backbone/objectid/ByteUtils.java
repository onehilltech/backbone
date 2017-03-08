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

  public static byte[] fromHexStringToByteArray (String hexString)
  {
    int len = hexString.length ();
    byte[] data = new byte[len / 2];

    for (int i = 0; i < data.length; i++)
      data[i] = (byte) Integer.parseInt(hexString.substring (i * 2, i * 2 + 2), 16);

    return data;
  }

  public static int toInt (final byte b3, final byte b2, final byte b1, final byte b0)
  {
    return
        (((b3) << 24) |
        ((b2 & 0xff) << 16) |
        ((b1 & 0xff) << 8) |
        ((b0 & 0xff)));
  }
}

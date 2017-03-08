package com.onehilltech.backbone.objectid;

import org.joda.time.DateTime;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @class ObjectId
 * <p>
 * The 12-byte value consists of
 * <p>
 * = 4-byte value representing the seconds since the Unix epoch,
 * = 3-byte machine identifier,
 * = 2-byte process id, and
 * = 3-byte counter, starting with a random value.
 */
public final class ObjectId
{
  /// Current date/timeof the generated id.
  private int seconds_;

  /// The machine part of the id.
  private int machinePart_;

  /// The process part of the id.
  private short processPart_;

  /// Random integer for the id.
  private int counter_;

  /// Pre-computed hash code for the object id.
  private int hashCode_;

  private static final char [] HEX_CHARS = "0123456789abcdef".toCharArray ();

  /**
   * Initializing constructor.
   *
   * @param seconds
   * @param machinePart
   * @param process
   * @param counter
   */
  ObjectId (int seconds, int machinePart, short process, int counter)
  {
    this.seconds_ = seconds;
    this.machinePart_ = machinePart & 0x00ffffff;
    this.processPart_ = process;
    this.counter_ = counter & 0x00ffffff;

    this.computeHashCode ();
  }

  /**
   * Initializing constructor.
   *
   * @param hexString
   */
  public ObjectId (String hexString)
  {
    this (ByteUtils.fromHexStringToByteArray (hexString));
  }

  /**
   * Initializing constructor.
   *
   * @param bytes
   */
  public ObjectId (byte [] bytes)
  {
    ByteBuffer buffer = ByteBuffer.wrap (bytes);
    this.seconds_ = ByteUtils.toInt (buffer.get (), buffer.get (), buffer.get (), buffer.get ());
    this.machinePart_ = ByteUtils.toInt ((byte)0, buffer.get (), buffer.get (), buffer.get ());
    this.processPart_ = (short)ByteUtils.toInt ((byte)0, (byte)0, buffer.get (), buffer.get ());
    this.counter_ =  ByteUtils.toInt ((byte)0, buffer.get (), buffer.get (), buffer.get ());

    this.computeHashCode ();
  }

  /**
   * Get the machine part.
   *
   * @return
   */
  public int getMachinePart ()
  {
    return this.machinePart_;
  }

  /**
   * Get the process part of the id.
   *
   * @return
   */
  public int getProcessPart ()
  {
    return this.processPart_;
  }

  /**
   * Get the random integer for the id.
   *
   * @return
   */
  public int getCounter ()
  {
    return this.counter_;
  }

  /**
   * Get the date the id was generated.
   *
   * @return
   */
  public int getSeconds ()
  {
    return this.seconds_;
  }

  /**
   * Get the date the object id was generated.
   *
   * @return
   */
  public DateTime getDate ()
  {
    return new DateTime (this.seconds_ * 1000);
  }

  @Override
  public boolean equals (Object obj)
  {
    if (!(obj instanceof ObjectId))
      return false;

    ObjectId objId = (ObjectId) obj;

    return this.seconds_ == objId.seconds_ &&
            this.machinePart_ == objId.machinePart_ &&
            this.processPart_ == objId.processPart_ &&
            this.counter_ == objId.counter_;
  }

  /**
   * Compute the hash code for the object.
   */
  private void computeHashCode ()
  {
    Object[] objs = {
        this.seconds_,
        this.machinePart_,
        this.processPart_,
        this.counter_
    };

    this.hashCode_ = Arrays.hashCode (objs);
  }

  @Override
  public String toString ()
  {
    char [] hexString = new char[24];
    byte [] bytes = this.toByteArray ();

    int i = 0;

    for (byte b: bytes)
    {
      hexString[i ++] = HEX_CHARS[b >> 4 & 0xF];
      hexString[i ++] = HEX_CHARS[b & 0x0F];
    }

    return new String (hexString);
  }

  @Override
  public int hashCode ()
  {
    return this.hashCode_;
  }

  /**
   * Convert the ObjectId to a byte array.
   *
   * @return
   */
  public byte[] toByteArray ()
  {
    ByteBuffer buffer = ByteBuffer.allocate (12);

    // Write the individual bytes to prevent byte swapping.
    buffer.put (ByteUtils.int3 (this.seconds_));
    buffer.put (ByteUtils.int2 (this.seconds_));
    buffer.put (ByteUtils.int1 (this.seconds_));
    buffer.put (ByteUtils.int0 (this.seconds_));
    buffer.put (ByteUtils.int2 (this.machinePart_));
    buffer.put (ByteUtils.int1 (this.machinePart_));
    buffer.put (ByteUtils.int0 (this.machinePart_));
    buffer.put (ByteUtils.short1 (this.processPart_));
    buffer.put (ByteUtils.short0 (this.processPart_));
    buffer.put (ByteUtils.int2 (this.counter_));
    buffer.put (ByteUtils.int1 (this.counter_));
    buffer.put (ByteUtils.int0 (this.counter_));

    return buffer.array ();
  }
}

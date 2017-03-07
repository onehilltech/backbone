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
  private DateTime date_;

  /// The machine part of the id.
  private int machinePart_;

  /// The process part of the id.
  private short processPart_;

  /// Random integer for the id.
  private int counter_;

  /// Pre-computed hash code for the object id.
  private int hashCode_;

  private static final char [] HEX_CHARS = "1234567890abcdef".toCharArray ();

  /**
   * Convert a String to an ObjectId.
   *
   * @param objectId
   * @return
   */
  public static ObjectId fromString (String objectId)
  {
    return null;
  }

  /**
   * Initializing constructor.
   *
   * @param date
   * @param machinePart
   * @param process
   * @param counter
   */
  ObjectId (DateTime date, int machinePart, short process, int counter)
  {
    this.date_ = date;
    this.machinePart_ = machinePart;
    this.processPart_ = process;
    this.counter_ = counter;

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
  public DateTime getDate ()
  {
    return this.date_;
  }

  @Override
  public boolean equals (Object obj)
  {
    if (!(obj instanceof ObjectId))
      return false;

    ObjectId objId = (ObjectId) obj;

    return
        this.date_.equals (objId.date_) &&
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
        this.date_,
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

    // Convert the date to seconds since epoch.
    int seconds = (int) (this.date_.getMillis () / 1000);

    // Write the individual bytes to prevent byte swapping.
    buffer.put (ByteUtils.int3 (seconds));
    buffer.put (ByteUtils.int2 (seconds));
    buffer.put (ByteUtils.int1 (seconds));
    buffer.put (ByteUtils.int0 (seconds));
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

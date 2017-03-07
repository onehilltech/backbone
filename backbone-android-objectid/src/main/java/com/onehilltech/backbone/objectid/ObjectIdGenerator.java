package com.onehilltech.backbone.objectid;

import org.joda.time.DateTime;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @class ObjectIdGenerator
 *
 * Generator class for creating an ObjectId.
 */
public final class ObjectIdGenerator
{
  /// The singleton instance.
  private static ObjectIdGenerator instance_;

  /// Machine part to use for all generated ObjectId
  private int machinePart_;

  /// Process part to use for all generated ObjectId
  private short processPart_;

  /// Next counter value for the generated ObjectId
  private final AtomicInteger counter_ = new AtomicInteger (new SecureRandom ().nextInt ());

  /**
   * Get the singleton instance.
   *
   * @return
   */
  public static ObjectIdGenerator getInstance ()
  {
    if (instance_ != null)
      return instance_;

    int machinePart = computeMachinePart ();
    short processPart = computeProcessPart ();

    instance_ = new ObjectIdGenerator (machinePart, processPart);
    return instance_;
  }

  /**
   * Compute the machine part for all ObjectId objects.
   *
   * @return
   */
  private static int computeMachinePart ()
  {
    // Ideally, we should use the network interfaces to compute the machine
    // part. On Android 6.0+, it is not possible get the machine information,
    // such as Bluetooth mac address. We could use the InstanceId from Google
    // Services, but this requires us to integrate Google Services. So, we are
    // are just going to use the hash of a random UUID.
    return UUID.randomUUID ().hashCode ();
  }

  /**
   * Compute the process part for all ObjectId objects.
   *
   * @return
   */
  private static short computeProcessPart ()
  {
    return (short)android.os.Process.myPid ();
  }

  /**
   * Initializing constructor.
   *
   * @param machinePart
   * @param processPart
   */
  private ObjectIdGenerator (int machinePart, short processPart)
  {
    this.machinePart_ = machinePart;
    this.processPart_ = processPart;
  }

  /**
   * Get the next ObjectId for the generator.
   *
   * @return
   */
  public ObjectId nextObjectId ()
  {
    DateTime date = DateTime.now ();
    int counter = this.counter_.getAndIncrement ();

    return new ObjectId (date, this.machinePart_, this.processPart_, counter);
  }

  /**
   * Get the current counter value.
   *
   * @return
   */
  public int getCounter ()
  {
    return this.counter_.get ();
  }

  public int getMachinePart ()
  {
    return this.machinePart_;
  }

  public short getProcessPart ()
  {
    return this.processPart_;
  }
}

package com.onehilltech.backbone.app;

public interface PromiseFulfillment <T>
{
  void resolve (T value);

  void reject (Throwable reason);
}

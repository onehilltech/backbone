package com.onehilltech.backbone.app;

public interface PromiseFulfill <T>
{
  void execute (PromiseFulfillment<T> completion);
}

package com.onehilltech.backbone.app;

public interface ContinuationExecutor <T>
{
  void with (Promise <T> promise);
}

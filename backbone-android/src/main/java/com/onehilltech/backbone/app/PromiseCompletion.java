package com.onehilltech.backbone.app;

public interface PromiseCompletion <T>
{
  void resolve (T value);

  void reject (Throwable reason);
}

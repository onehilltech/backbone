package com.onehilltech.backbone.app;

public interface PromiseExecutor <T>
{
  void execute (Promise.Settlement<T> settlement);
}

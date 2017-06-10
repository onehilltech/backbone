package com.onehilltech.backbone.app;

public interface PromiseExecutor <T>
{
  void execute (PromiseCompletion <T> completion);
}

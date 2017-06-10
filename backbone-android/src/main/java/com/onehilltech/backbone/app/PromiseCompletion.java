package com.onehilltech.backbone.app;

/**
 * Created by hilljh on 6/9/17.
 */

public interface PromiseCompletion
{
  void resolve (Object value);

  void reject (Throwable reason);
}

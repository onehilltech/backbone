package com.onehilltech.backbone.http.retrofit.tasks;

import android.support.annotation.Nullable;

import com.onehilltech.backbone.http.Resource;
import com.onehilltech.concurrent.CompletionCallback;
import com.onehilltech.concurrent.Task;
import com.onehilltech.backbone.http.retrofit.ResourceEndpoint;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GetResourcesTask extends Task
{
  public static class Builder
  {
    private String name_;

    private ResourceEndpoint<?> endpoint_;

    private Map <String, Object> criteria_;

    private int currentCount_ = -1;

    public Builder (ResourceEndpoint<?> endpoint)
    {
      this (null, endpoint);
    }

    public Builder (String name, ResourceEndpoint<?> endpoint)
    {
      this.name_ = name;
      this.endpoint_ = endpoint;
    }

    public Builder setCriteria (Map <String, Object> criteria)
    {
      this.criteria_ = criteria;
      return this;
    }

    public Builder setCurrentCount (int count)
    {
      this.currentCount_ = count;
      return this;
    }

    public GetResourcesTask build ()
    {
      GetResourcesTask task = new GetResourcesTask (this.name_, this.endpoint_);

      if (this.criteria_ != null)
        task.criteria_.putAll (this.criteria_);

      if (this.currentCount_ != -1)
        task.cacheCount_ = this.currentCount_;

      return task;
    }
  }

  private int cacheCount_ = -1;

  private final ResourceEndpoint<?> endpoint_;

  private final Map<String, Object> criteria_ = new HashMap<> ();

  private GetResourcesTask (String name, ResourceEndpoint<?> endpoint)
  {
    super (name);

    this.endpoint_ = endpoint;
  }

  @Override
  public void run (@Nullable Object o, final CompletionCallback completionCallback)
  {
    Integer remoteCount = (int)o;

    if (o == null || remoteCount != this.cacheCount_)
    {
      this.endpoint_.get (this.criteria_).enqueue (new Callback<Resource> ()
      {
        @Override
        public void onResponse (Call<Resource> call, Response<Resource> response)
        {
          if (response.isSuccessful ())
            completionCallback.done (response.body ());
          else
            completionCallback.fail (new IllegalStateException ("Failed to get resources"));
        }

        @Override
        public void onFailure (Call<Resource> call, Throwable t)
        {
          completionCallback.fail (t);
        }
      });
    }
    else
    {
      completionCallback.done ();
    }
  }

}

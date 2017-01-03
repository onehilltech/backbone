package com.onehilltech.backbone.http.retrofit.tasks;

import com.onehilltech.backbone.http.Resource;
import com.onehilltech.backbone.http.retrofit.ResourceEndpoint;
import com.onehilltech.concurrent.CompletionCallback;
import com.onehilltech.concurrent.Task;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GetResourceCountTask extends Task
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

    public GetResourceCountTask build ()
    {
      GetResourceCountTask task = new GetResourceCountTask (this.name_, this.endpoint_);

      if (this.criteria_ != null)
        task.criteria_.putAll (this.criteria_);

      return task;
    }
  }


  private final ResourceEndpoint<?> endpoint_;
  private final Map<String, Object> criteria_ = new HashMap<> ();

  GetResourceCountTask (String name, ResourceEndpoint<?> endpoint)
  {
    super (name);

    this.endpoint_ = endpoint;
  }

  @Override
  public void run (Object o, final CompletionCallback completionCallback)
  {
    this.endpoint_.count (this.criteria_).enqueue (new Callback<Resource> ()
    {
      @Override
      public void onResponse (Call<Resource> call, Response<Resource> response)
      {
        if (response.isSuccessful ())
        {
          int count = response.body ().get ("count");
          completionCallback.done (count);
        }
        else
        {
          completionCallback.fail (new IllegalStateException ("Failed to get count"));
        }
      }

      @Override
      public void onFailure (Call<Resource> call, Throwable t)
      {
        completionCallback.fail (t);
      }
    });
  }
}



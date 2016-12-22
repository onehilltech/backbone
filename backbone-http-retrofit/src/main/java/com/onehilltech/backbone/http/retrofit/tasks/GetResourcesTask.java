package com.onehilltech.backbone.http.retrofit.tasks;

import com.onehilltech.concurrent.CompletionCallback;
import com.onehilltech.concurrent.Task;
import com.onehilltech.gatekeeper.android.RefreshTokenCallback;
import com.onehilltech.gatekeeper.android.SingleUserSessionClient;
import com.onehilltech.backbone.http.retrofit.Resource;
import com.onehilltech.backbone.http.retrofit.ResourceEndpoint;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

public class GetResourcesTask extends Task
{
  public static class Builder
  {
    private String name_;

    private ResourceEndpoint<?> endpoint_;

    private SingleUserSessionClient session_;

    private Map <String, Object> criteria_;

    private int currentCount_ = -1;

    public Builder (ResourceEndpoint<?> endpoint, SingleUserSessionClient session)
    {
      this (null, endpoint, session);
    }

    public Builder (String name, ResourceEndpoint<?> endpoint, SingleUserSessionClient session)
    {
      this.name_ = name;
      this.endpoint_ = endpoint;
      this.session_ = session;
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
      GetResourcesTask task = new GetResourcesTask (this.name_, this.endpoint_, this.session_);

      if (this.criteria_ != null)
        task.criteria_.putAll (this.criteria_);

      if (this.currentCount_ != -1)
        task.cacheCount_ = this.currentCount_;

      return task;
    }
  }

  private int cacheCount_ = -1;

  private final ResourceEndpoint<?> endpoint_;

  private final SingleUserSessionClient session_;

  private final Map<String, Object> criteria_ = new HashMap<> ();

  GetResourcesTask (String name, ResourceEndpoint<?> endpoint, SingleUserSessionClient session)
  {
    super (name);

    this.endpoint_ = endpoint;
    this.session_ = session;
  }

  @Override
  public void run (Object o, final CompletionCallback completionCallback)
  {
    final Long remoteCount = (Long)o;

    if (remoteCount == null || remoteCount != this.cacheCount_)
    {
      this.endpoint_.get (this.criteria_).enqueue (new RefreshTokenCallback<Resource> (this.session_)
      {
        @Override
        public void onHandleResponse (Call<Resource> call, Response<Resource> response)
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

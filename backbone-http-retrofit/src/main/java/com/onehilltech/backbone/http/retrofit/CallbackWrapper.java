package com.onehilltech.backbone.http.retrofit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CallbackWrapper <T> implements Callback <T>
{
  private final Callback <T> delegate_;

  public CallbackWrapper (Callback <T> delegate)
  {
    this.delegate_ = delegate;
  }

  @Override
  public void onFailure (Call<T> call, Throwable t)
  {
    this.delegate_.onFailure (call, t);
  }

  @Override
  public void onResponse (Call<T> call, Response<T> response)
  {
    this.delegate_.onResponse (call, response);
  }
}

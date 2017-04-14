package com.onehilltech.backbone.http;

/**
 * @class HttpError
 *
 * The default representation of an http error in the response body.
 */
public class HttpError
{
  private String code;

  private String message;

  public HttpError ()
  {

  }

  public String getCode ()
  {
    return this.code;
  }

  public String getMessage ()
  {
    return this.message;
  }
}

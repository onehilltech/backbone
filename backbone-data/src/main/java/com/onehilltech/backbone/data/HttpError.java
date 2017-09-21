package com.onehilltech.backbone.data;

import java.util.HashMap;

/**
 * @class HttpError
 *
 * The default representation of an http error in the response body.
 */
public class HttpError extends RuntimeException
{
  private int statusCode;

  private String code;

  private String message;

  private HashMap <String, Object> details;

  public HttpError ()
  {

  }

  public HttpError (String code, String message)
  {
    this.code = code;
    this.message = message;
  }

  public int getStatusCode ()
  {
    return this.statusCode;
  }

  public void setStatusCode (int statusCode)
  {
    this.statusCode = statusCode;
  }

  public String getCode ()
  {
    return this.code;
  }

  public String getMessage ()
  {
    return this.message;
  }

  public HashMap <String, Object> getDetails ()
  {
    return this.details;
  }
}

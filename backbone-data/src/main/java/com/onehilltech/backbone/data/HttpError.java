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

  private String detail;

  private HashMap <String, Object> meta;

  public HttpError ()
  {

  }

  public HttpError (String code, String detail)
  {
    this.code = code;
    this.detail = detail;
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

  @Override
  public String getMessage ()
  {
    return this.detail;
  }

  @Override
  public String getLocalizedMessage ()
  {
    return this.detail;
  }

  public HashMap <String, Object> getMeta ()
  {
    return this.meta;
  }
}

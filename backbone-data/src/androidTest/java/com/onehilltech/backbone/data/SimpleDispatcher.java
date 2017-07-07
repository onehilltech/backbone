package com.onehilltech.backbone.data;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.squareup.okhttp.mockwebserver.Dispatcher;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import java.util.Collection;
import java.util.Iterator;

public class SimpleDispatcher extends Dispatcher
{
  private final Multimap <String, MockResponse> responses_ = ArrayListMultimap.create ();

  public void add (String path, MockResponse response)
  {
    this.responses_.put (path, response);
  }

  @Override
  public MockResponse dispatch (RecordedRequest request) throws InterruptedException
  {
    Collection <MockResponse> responses = this.responses_.get (request.getPath ());

    if (responses == null)
      return new MockResponse ().setResponseCode (404);

    Iterator <MockResponse> iter = responses.iterator ();

    if (!iter.hasNext ())
      return new MockResponse ().setResponseCode (404);

    MockResponse nextResponse = iter.next ();
    iter.remove ();

    return nextResponse;
  }
}

/*
 * Copyright © 2023 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.idstore.server.http;

import org.eclipse.jetty.ee10.servlet.Dispatcher;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Optional;

/**
 * A simple error handler.
 */

public final class IdPlainErrorHandler implements Request.Handler
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdPlainErrorHandler.class);

  /**
   * A simple error handler.
   */

  public IdPlainErrorHandler()
  {

  }

  private static boolean exceptionResponse(
    final Response response,
    final Throwable exception,
    final Callback callback)
    throws IOException
  {
    LOG.error("Exception: ", exception);

    response.getHeaders()
        .put("Content-Type", "text/plain");

    try (var writer = new StringWriter()) {
      writer.append("Internal server error.");
      writer.append('\r');
      writer.append('\n');
      writer.flush();
      Content.Sink.write(response, true, writer.toString(), callback);
    }
    return true;
  }

  @Override
  public boolean handle(
    final Request request,
    final Response response,
    final Callback callback)
    throws Exception
  {
    final var exception =
      (Throwable) request.getAttribute(Dispatcher.ERROR_EXCEPTION);
    final var message =
      (String) request.getAttribute(Dispatcher.ERROR_MESSAGE);
    final var errorCode =
      request.getAttribute(Dispatcher.ERROR_STATUS_CODE);

    if (exception != null) {
      return exceptionResponse(response, exception, callback);
    }

    response.getHeaders()
      .put("Content-Type", "text/plain");

    try (var writer = new StringWriter()) {
      writer.append(errorCode.toString());
      writer.append(' ');
      writer.write(Optional.ofNullable(message).orElse(""));
      writer.append('\r');
      writer.append('\n');
      writer.flush();
      Content.Sink.write(response, true, writer.toString(), callback);
    }

    return true;
  }
}

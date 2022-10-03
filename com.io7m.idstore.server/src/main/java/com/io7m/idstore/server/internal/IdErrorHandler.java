/*
 * Copyright © 2022 Mark Raynsford <code@io7m.com> https://www.io7m.com
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


package com.io7m.idstore.server.internal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Dispatcher;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A simple error handler.
 */

public final class IdErrorHandler extends ErrorHandler
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdErrorHandler.class);

  /**
   * A simple error handler.
   */

  public IdErrorHandler()
  {

  }

  private static void exceptionResponse(
    final HttpServletResponse response,
    final Throwable exception)
    throws IOException
  {
    LOG.error("exception: ", exception);
    response.setContentType("text/plain");

    try (var out = response.getOutputStream()) {
      try (var w = new BufferedWriter(new OutputStreamWriter(out, UTF_8))) {
        w.write(Optional.ofNullable(exception.getMessage()).orElse(""));
        w.write('\r');
        w.write('\n');
      }
    }
  }

  @Override
  public void handle(
    final String target,
    final Request baseRequest,
    final HttpServletRequest request,
    final HttpServletResponse response)
    throws IOException
  {
    final var exception =
      (Throwable) baseRequest.getAttribute(Dispatcher.ERROR_EXCEPTION);
    final var message =
      (String) baseRequest.getAttribute(Dispatcher.ERROR_MESSAGE);
    final var errorCode =
      baseRequest.getAttribute(Dispatcher.ERROR_STATUS_CODE);

    if (exception != null) {
      exceptionResponse(response, exception);
      return;
    }

    response.setContentType("text/plain");

    try (var out = response.getOutputStream()) {
      try (var w = new BufferedWriter(new OutputStreamWriter(out, UTF_8))) {
        w.write(errorCode.toString());
        w.write(" ");
        w.write(Optional.ofNullable(message).orElse(""));
        w.write("\r");
        w.write("\n");
        w.flush();
      }
    }
  }
}

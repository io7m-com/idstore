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

package com.io7m.idstore.server.logging;

import com.io7m.idstore.server.internal.IdServerClock;
import com.io7m.idstore.server.internal.IdServerRequestDecoration;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.slf4j.MDC;

import java.time.Duration;
import java.util.Objects;

import static com.io7m.idstore.server.logging.IdLoggingHTTPRequestLayout.MDC_RESPONSE_CONTENT_LENGTH;
import static com.io7m.idstore.server.logging.IdLoggingHTTPRequestLayout.MDC_RESPONSE_CONTENT_TYPE;
import static com.io7m.idstore.server.logging.IdLoggingHTTPRequestLayout.MDC_RESPONSE_DURATION;
import static com.io7m.idstore.server.logging.IdLoggingHTTPRequestLayout.MDC_RESPONSE_HTTP_STATUS;

/**
 * An MDC processor that adds MDC properties for an HTTP response.
 */

public final class IdServerMDCResponseProcessor
  implements IdServerMDCProcessorType
{
  private final IdServerClock clock;
  private final Request request;
  private final Response response;

  private IdServerMDCResponseProcessor(
    final IdServerClock inClock,
    final Request inRequest,
    final Response inResponse)
  {
    this.clock =
      Objects.requireNonNull(inClock, "inClock");
    this.request =
      Objects.requireNonNull(inRequest, "inRequest");
    this.response =
      Objects.requireNonNull(inResponse, "response");
  }

  /**
   * An MDC processor that adds MDC properties for an HTTP response.
   *
   * @param inRequest  The request
   * @param inResponse The response
   * @param inClock    A clock
   *
   * @return A processor
   */

  public static IdServerMDCProcessorType open(
    final IdServerClock inClock,
    final Request inRequest,
    final Response inResponse)
  {
    final var p =
      new IdServerMDCResponseProcessor(inClock, inRequest, inResponse);
    p.process();
    return p;
  }

  private void process()
  {
    final var timeThen =
      IdServerRequestDecoration.requestStartTimeFor(this.request);
    final var timeNow =
      this.clock.nowPrecise();
    final var timeDuration =
      Duration.between(timeThen, timeNow);

    MDC.put(
      MDC_RESPONSE_CONTENT_LENGTH,
      Long.toString(this.response.getContentLength()));
    MDC.put(
      MDC_RESPONSE_CONTENT_TYPE,
      this.response.getContentType());
    MDC.put(
      MDC_RESPONSE_DURATION,
      timeDuration.toString());
    MDC.put(
      MDC_RESPONSE_HTTP_STATUS,
      String.valueOf(this.response.getStatus()));
  }

  @Override
  public void close()
    throws RuntimeException
  {
    MDC.remove(MDC_RESPONSE_CONTENT_LENGTH);
    MDC.remove(MDC_RESPONSE_CONTENT_TYPE);
    MDC.remove(MDC_RESPONSE_DURATION);
    MDC.remove(MDC_RESPONSE_HTTP_STATUS);
  }
}

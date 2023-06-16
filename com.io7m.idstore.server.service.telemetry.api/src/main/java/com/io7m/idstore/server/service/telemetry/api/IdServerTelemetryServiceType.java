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

package com.io7m.idstore.server.service.telemetry.api;

import com.io7m.idstore.error_codes.IdErrorCode;
import com.io7m.idstore.error_codes.IdException;
import com.io7m.repetoir.core.RPServiceType;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;

/**
 * The type of server telemetry services.
 */

public interface IdServerTelemetryServiceType extends RPServiceType
{
  /**
   * @return The main tracer
   */

  Tracer tracer();

  /**
   * @return The main meter
   */

  Meter meter();

  /**
   * @return The main logger
   */

  Logger logger();

  /**
   * Set the error code for the current span.
   *
   * @param errorCode The error code
   */

  static void setSpanErrorCode(
    final IdErrorCode errorCode)
  {
    final var span = Span.current();
    span.setAttribute("idstore.errorCode", errorCode.id());
    span.setStatus(StatusCode.ERROR);
  }

  /**
   * Set the error code for the current span.
   *
   * @param e The error code
   */

  static void recordSpanException(
    final Throwable e)
  {
    final var span = Span.current();
    if (e instanceof final IdException ex) {
      setSpanErrorCode(ex.errorCode());
    }
    span.recordException(e);
    span.setStatus(StatusCode.ERROR);
  }
}

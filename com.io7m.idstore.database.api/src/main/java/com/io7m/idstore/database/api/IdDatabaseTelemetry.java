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


package com.io7m.idstore.database.api;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;

import java.util.Objects;

/**
 * Interfaces used for database telemetry.
 *
 * @param isNoOp {@code true} if telemetry is in no-op mode
 * @param meter  The meter
 * @param tracer The tracer
 */

public record IdDatabaseTelemetry(
  boolean isNoOp,
  Meter meter,
  Tracer tracer)
{
  /**
   * Interfaces used for database telemetry.
   *
   * @param isNoOp {@code true} if telemetry is in no-op mode
   * @param meter  The meter
   * @param tracer The tracer
   */

  public IdDatabaseTelemetry
  {
    Objects.requireNonNull(meter, "meter");
    Objects.requireNonNull(tracer, "tracer");
  }
}

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


package com.io7m.idstore.server.service.telemetry.otp.internal;

import com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

import java.util.Objects;

/**
 * An OpenTelemetry service.
 */

public final class IdServerTelemetryService
  implements IdServerTelemetryServiceType
{
  private final OpenTelemetry openTelemetry;
  private final Tracer tracer;

  /**
   * An OpenTelemetry service.
   *
   * @param inOpenTelemetry The OT instance
   * @param inTracer        The tracer instance
   */

  public IdServerTelemetryService(
    final OpenTelemetry inOpenTelemetry,
    final Tracer inTracer)
  {
    this.openTelemetry =
      Objects.requireNonNull(inOpenTelemetry, "openTelemetry");
    this.tracer =
      Objects.requireNonNull(inTracer, "tracer");
  }

  @Override
  public String toString()
  {
    return "[IdServerTelemetryService 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode(), 16));
  }

  @Override
  public String description()
  {
    return "Server OpenTelemetry service.";
  }

  @Override
  public Tracer tracer()
  {
    return this.tracer;
  }

  @Override
  public OpenTelemetry openTelemetry()
  {
    return this.openTelemetry;
  }
}

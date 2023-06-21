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

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.TextMapPropagator;

import java.util.Objects;

/**
 * A no-op telemetry service.
 */

public final class IdServerTelemetryNoOp
  implements IdServerTelemetryServiceType
{
  private final Tracer tracer;
  private final Meter meter;
  private final Logger logger;
  private final TextMapPropagator textMapPropagator;
  private final boolean isNoOp;

  private IdServerTelemetryNoOp(
    final Tracer inTracer,
    final Meter inMeter,
    final Logger inLogger,
    final TextMapPropagator inTextMapPropagator,
    final boolean noOp)
  {
    this.tracer =
      Objects.requireNonNull(inTracer, "tracer");
    this.meter =
      Objects.requireNonNull(inMeter, "inMeter");
    this.logger =
      Objects.requireNonNull(inLogger, "inLogger");
    this.textMapPropagator =
      Objects.requireNonNull(inTextMapPropagator, "inTextMapPropagator");
    this.isNoOp = noOp;
  }

  /**
   * @return A completely no-op service
   */

  public static IdServerTelemetryNoOp noop()
  {
    final var noop = OpenTelemetry.noop();
    return new IdServerTelemetryNoOp(
      noop.getTracer("noop"),
      noop.getMeter("noop"),
      noop.getLogsBridge().get("noop"),
      noop.getPropagators()
        .getTextMapPropagator(),
      true
    );
  }

  @Override
  public TextMapPropagator textMapPropagator()
  {
    return this.textMapPropagator;
  }

  @Override
  public Tracer tracer()
  {
    return this.tracer;
  }

  @Override
  public Meter meter()
  {
    return this.meter;
  }

  @Override
  public Logger logger()
  {
    return this.logger;
  }

  @Override
  public boolean isNoOp()
  {
    return this.isNoOp;
  }

  @Override
  public String toString()
  {
    return "[IdServerTelemetryNoOp 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode(), 16));
  }

  @Override
  public String description()
  {
    return "Server no-op telemetry service.";
  }
}

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

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.time.Instant;
import java.util.Objects;

/**
 * The event service.
 */

public final class IdEventService implements IdEventServiceType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdEventService.class);

  private final io.opentelemetry.api.logs.Logger logger;
  private final IdMetricsServiceType metrics;

  private IdEventService(
    final IdServerTelemetryServiceType inTelemetry,
    final IdMetricsServiceType inMetrics)
  {
    Objects.requireNonNull(inTelemetry, "inTelemetry");

    this.metrics =
      Objects.requireNonNull(inMetrics, "inMetrics");
    this.logger =
      inTelemetry.logger();
  }

  /**
   * Create a new event service.
   *
   * @param telemetry The telemetry service
   * @param metrics   The metrics service
   *
   * @return The event service
   */

  public static IdEventServiceType create(
    final IdServerTelemetryServiceType telemetry,
    final IdMetricsServiceType metrics)
  {
    return new IdEventService(telemetry, metrics);
  }

  @Override
  public void emit(
    final IdEventType event)
  {
    Objects.requireNonNull(event, "event");

    this.logToTelemetry(event);
    this.logToMetrics(event);
  }

  private void logToMetrics(
    final IdEventType event)
  {
    if (event instanceof final IdEventMailSent s) {
      this.metrics.onMailSent(s.to(), s.time());
    }
    if (event instanceof final IdEventMailFailed f) {
      this.metrics.onMailFailed(f.to(), f.time());
    }
  }

  private void logToTelemetry(
    final IdEventType event)
  {
    var builder =
      LOG.makeLoggingEventBuilder(
        switch (event.severity()) {
          case INFO -> Level.INFO;
          case ERROR -> Level.ERROR;
          case WARNING -> Level.WARN;
        }).setMessage(event.message());

    final var eventAttributeBuilder = Attributes.builder();
    for (final var entry : event.asAttributes().entrySet()) {
      eventAttributeBuilder.put(entry.getKey(), entry.getValue());
      builder = builder.addKeyValue(entry.getKey(), entry.getValue());
    }
    builder.log();

    final var attributes =
      eventAttributeBuilder.build();
    Span.current()
      .addEvent(event.name(), attributes, Instant.now());

    this.logger.logRecordBuilder()
      .setAllAttributes(attributes)
      .setBody(event.message())
      .setSeverity(
        switch (event.severity()) {
          case ERROR -> Severity.ERROR;
          case INFO -> Severity.INFO;
          case WARNING -> Severity.WARN;
        }).emit();
  }

  @Override
  public String description()
  {
    return "Event service.";
  }

  @Override
  public String toString()
  {
    return "[IdEventService 0x%s]"
      .formatted(Integer.toUnsignedString(this.hashCode(), 16));
  }
}

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

package com.io7m.idstore.server.service.events;

import com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import io.opentelemetry.api.common.Attributes;
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

  private IdEventService(
    final IdServerTelemetryServiceType inTelemetry)
  {
    Objects.requireNonNull(inTelemetry, "inTelemetry");
  }

  /**
   * Create a new event service.
   *
   * @param services The service directory
   *
   * @return The event service
   */

  public static IdEventServiceType create(
    final RPServiceDirectoryType services)
  {
    return new IdEventService(
      services.requireService(IdServerTelemetryServiceType.class)
    );
  }

  @Override
  public void emit(
    final IdEventType event)
  {
    Objects.requireNonNull(event, "event");

    var builder =
      LOG.makeLoggingEventBuilder(Level.INFO)
        .setMessage(event.message());

    final var eventAttributes = Attributes.builder();
    for (final var entry : event.asAttributes().entrySet()) {
      eventAttributes.put(entry.getKey(), entry.getValue());
      builder = builder.addKeyValue(entry.getKey(), entry.getValue());
    }

    Span.current()
      .addEvent(event.name(), eventAttributes.build(), Instant.now());
    builder.log();
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

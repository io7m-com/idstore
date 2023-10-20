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

import com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import io.helidon.webserver.http.ServerRequest;

import java.time.Duration;
import java.util.Objects;

/**
 * A handler core that executes with a fixed delay.
 */

public final class IdHTTPHandlerCoreFixedDelay
  implements IdHTTPHandlerFunctionalCoreType
{
  private final IdHTTPHandlerFunctionalCoreType core;
  private final Duration delay;
  private final IdServerTelemetryServiceType telemetry;

  private IdHTTPHandlerCoreFixedDelay(
    final IdServerTelemetryServiceType inTelemetry,
    final IdHTTPHandlerFunctionalCoreType inCore,
    final Duration inDelay)
  {
    this.telemetry =
      Objects.requireNonNull(inTelemetry, "inTelemetry");
    this.core =
      Objects.requireNonNull(inCore, "core");
    this.delay =
      Objects.requireNonNull(inDelay, "inDelay");
  }

  /**
   * @param services The services
   * @param inCore   The core
   * @param delay    The fixed delay to apply to each execution
   *
   * @return A handler core that executes with a fixed delay
   */

  public static IdHTTPHandlerFunctionalCoreType withFixedDelay(
    final RPServiceDirectoryType services,
    final Duration delay,
    final IdHTTPHandlerFunctionalCoreType inCore)
  {
    return new IdHTTPHandlerCoreFixedDelay(
      services.requireService(IdServerTelemetryServiceType.class),
      inCore,
      delay
    );
  }

  @Override
  public IdHTTPResponseType execute(
    final ServerRequest request,
    final IdHTTPRequestInformation information)
  {
    this.applyFixedDelay();
    return this.core.execute(request, information);
  }

  /**
   * Apply a fixed delay for all requests.
   */

  private void applyFixedDelay()
  {
    try {
      final var childSpan =
        this.telemetry.tracer()
          .spanBuilder("FixedDelay")
          .startSpan();

      try (var ignored = childSpan.makeCurrent()) {
        Thread.sleep(this.delay.toMillis());
      } finally {
        childSpan.end();
      }
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}

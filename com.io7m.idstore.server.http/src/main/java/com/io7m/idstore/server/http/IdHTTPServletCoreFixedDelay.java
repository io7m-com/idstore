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
import jakarta.servlet.http.HttpServletRequest;

import java.time.Duration;
import java.util.Objects;

/**
 * A servlet core that executes with a fixed delay.
 */

public final class IdHTTPServletCoreFixedDelay
  implements IdHTTPServletFunctionalCoreType
{
  private final IdHTTPServletFunctionalCoreType core;
  private final Duration delay;
  private final IdServerTelemetryServiceType telemetry;

  private IdHTTPServletCoreFixedDelay(
    final IdServerTelemetryServiceType inTelemetry,
    final IdHTTPServletFunctionalCoreType inCore,
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
   * @return A servlet core that executes with a fixed delay
   */

  public static IdHTTPServletFunctionalCoreType withFixedDelay(
    final RPServiceDirectoryType services,
    final Duration delay,
    final IdHTTPServletFunctionalCoreType inCore)
  {
    return new IdHTTPServletCoreFixedDelay(
      services.requireService(IdServerTelemetryServiceType.class),
      inCore,
      delay
    );
  }

  @Override
  public IdHTTPServletResponseType execute(
    final HttpServletRequest request,
    final IdHTTPServletRequestInformation information)
  {
    this.applyFixedDelay();
    return this.core.execute(request, information);
  }

  /**
   * Apply a fixed delay for all login requests.
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

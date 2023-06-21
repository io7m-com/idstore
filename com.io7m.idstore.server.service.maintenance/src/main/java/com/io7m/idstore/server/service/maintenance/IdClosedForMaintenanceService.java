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


package com.io7m.idstore.server.service.maintenance;

import com.io7m.idstore.server.service.telemetry.api.IdMetricsServiceType;
import com.io7m.repetoir.core.RPServiceType;

import java.util.Objects;
import java.util.Optional;

/**
 * A service that allows toggling the server into maintenance mode.
 */

public final class IdClosedForMaintenanceService
  implements RPServiceType
{
  private final IdMetricsServiceType metrics;
  private volatile Optional<String> message;

  /**
   * A service that allows toggling the server into maintenance mode.
   *
   * @param inMetrics The metrics service
   */

  public IdClosedForMaintenanceService(
    final IdMetricsServiceType inMetrics)
  {
    this.metrics =
      Objects.requireNonNull(inMetrics, "inMetrics");
    this.message =
      Optional.empty();
  }

  /**
   * The server is now open for business.
   */

  public void openForBusiness()
  {
    this.message = Optional.empty();
    this.metrics.onClosedForMaintenance(false);
  }

  /**
   * The server is now closed for maintenance.
   *
   * @param messageText The message text
   */

  public void closeForMaintenance(
    final String messageText)
  {
    this.message = Optional.of(messageText);
    this.metrics.onClosedForMaintenance(true);
  }

  /**
   * @return A message indicating the server is closed, if it is
   */

  public Optional<String> isClosed()
  {
    return this.message;
  }

  @Override
  public String description()
  {
    return "Closed for maintenance service.";
  }

  @Override
  public String toString()
  {
    return "[IdClosedForMaintenanceService 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode(), 16));
  }
}

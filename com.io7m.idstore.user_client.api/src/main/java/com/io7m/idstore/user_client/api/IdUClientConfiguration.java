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

package com.io7m.idstore.user_client.api;

import com.io7m.hibiscus.api.HBConfigurationType;
import io.opentelemetry.api.OpenTelemetry;

import java.time.Clock;
import java.util.Locale;
import java.util.Objects;

/**
 * The user client configuration.
 *
 * @param openTelemetry      The OpenTelemetry API
 * @param clock              The clock used for timeouts
 * @param locale             The locale
 * @param receiveQueueBounds The receive queue maximum size
 */

public record IdUClientConfiguration(
  OpenTelemetry openTelemetry,
  Clock clock,
  Locale locale,
  int receiveQueueBounds)
  implements HBConfigurationType
{
  /**
   * The user client configuration.
   *
   * @param openTelemetry      The OpenTelemetry API
   * @param clock              The clock used for timeouts
   * @param locale             The locale
   * @param receiveQueueBounds The receive queue maximum size
   */

  public IdUClientConfiguration
  {
    Objects.requireNonNull(openTelemetry, "openTelemetry");
    Objects.requireNonNull(clock, "clock");
    Objects.requireNonNull(locale, "locale");
  }
}

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

package com.io7m.idstore.server.api;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Configuration information for the server's maintenance service.
 *
 * @param tlsReloadInterval The interval at which to reload TLS contexts
 */

public record IdServerMaintenanceConfiguration(
  Optional<Duration> tlsReloadInterval)
{
  /**
   * Configuration information for the server's maintenance service.
   *
   * @param tlsReloadInterval The interval at which to reload TLS contexts
   */

  public IdServerMaintenanceConfiguration
  {
    Objects.requireNonNull(
      tlsReloadInterval, "tlsReloadInterval");
  }
}

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

package com.io7m.idstore.server.service.configuration;

import com.io7m.idstore.server.api.IdServerConfiguration;
import com.io7m.repetoir.core.RPServiceType;

import java.util.Objects;

/**
 * A service that exposes configuration information.
 */

public final class IdServerConfigurationService implements RPServiceType
{
  private final IdServerConfiguration configuration;

  /**
   * A service that exposes configuration information.
   *
   * @param inConfiguration The configuration
   */

  public IdServerConfigurationService(
    final IdServerConfiguration inConfiguration)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
  }

  @Override
  public String description()
  {
    return "Server configurations.";
  }

  /**
   * @return The current configuration
   */

  public IdServerConfiguration configuration()
  {
    return this.configuration;
  }

  @Override
  public String toString()
  {
    return "[IdServerConfigurationService 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode(), 16));
  }
}

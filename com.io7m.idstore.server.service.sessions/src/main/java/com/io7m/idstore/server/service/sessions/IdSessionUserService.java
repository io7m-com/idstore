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

package com.io7m.idstore.server.service.sessions;

import io.opentelemetry.api.OpenTelemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * A service to create and manage user sessions.
 */

public final class IdSessionUserService extends IdSessionService<IdSessionUser>
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdSessionUserService.class);

  /**
   * A service to create and manage sessions.
   *
   * @param inTelemetry  The telemetry service
   * @param inExpiration The expiration time for sessions
   */

  public IdSessionUserService(
    final OpenTelemetry inTelemetry,
    final Duration inExpiration)
  {
    super(inTelemetry, inExpiration, "User", IdSessionUser::new);
  }

  @Override
  public String description()
  {
    return "User session service.";
  }

  @Override
  public String toString()
  {
    return "[IdSessionUserService 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode(), 16));
  }

  @Override
  protected Logger logger()
  {
    return LOG;
  }
}

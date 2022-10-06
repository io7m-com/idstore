/*
 * Copyright © 2022 Mark Raynsford <code@io7m.com> https://www.io7m.com
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


package com.io7m.idstore.server.internal;

import com.io7m.idstore.services.api.IdServiceType;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * A service that exposes a clock.
 */

public final class IdServerClock implements IdServiceType
{
  private final Clock clock;

  /**
   * A service that exposes a clock.
   *
   * @param inClock The clock
   */

  public IdServerClock(
    final Clock inClock)
  {
    this.clock = Objects.requireNonNull(inClock, "clock");
  }

  @Override
  public String description()
  {
    return "Server clock.";
  }

  /**
   * @return The current time to the nearest second
   */

  public OffsetDateTime now()
  {
    return this.nowPrecise().withNano(0);
  }

  /**
   * @return The current time
   */

  public OffsetDateTime nowPrecise()
  {
    return OffsetDateTime.now(this.clock);
  }

  @Override
  public String toString()
  {
    return "[IdServerClock 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode(), 16));
  }
}

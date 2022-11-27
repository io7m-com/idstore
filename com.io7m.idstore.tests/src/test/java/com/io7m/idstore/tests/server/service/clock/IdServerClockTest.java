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


package com.io7m.idstore.tests.server.service.clock;

import com.io7m.idstore.server.service.clock.IdServerClock;
import com.io7m.idstore.tests.IdFakeClock;
import com.io7m.idstore.tests.server.service.IdServiceContract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;

import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class IdServerClockTest
  extends IdServiceContract<IdServerClock>
{
  private IdFakeClock clock;
  private IdServerClock clockService;

  @BeforeEach
  public void setup()
  {
    this.clock =
      new IdFakeClock();
    this.clockService =
      new IdServerClock(this.clock);
  }

  /**
   * Fetching the current time works.
   */

  @Test
  public void testNow()
  {
    final var then =
      this.clock.instant().getEpochSecond();
    final var now =
      OffsetDateTime.ofInstant(Instant.ofEpochSecond(then + 1L), UTC);

    assertEquals(now, this.clockService.now());
  }

  /**
   * Fetching the current time works.
   */

  @Test
  public void testNowPrecise()
  {
    final var then =
      this.clock.instant().getEpochSecond();
    final var now =
      OffsetDateTime.ofInstant(Instant.ofEpochSecond(then + 1L), UTC);

    assertEquals(now, this.clockService.nowPrecise());
  }

  @Override
  protected IdServerClock createInstanceA()
  {
    return new IdServerClock(this.clock);
  }

  @Override
  protected IdServerClock createInstanceB()
  {
    return new IdServerClock(this.clock);
  }
}

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

package com.io7m.idstore.tests.model;

import com.io7m.idstore.model.IdTimeRange;
import com.io7m.idstore.model.IdValidityException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class IdTimeRangeTest
{
  /**
   * Ordering is correct.
   */

  @Test
  public void testOrdered()
  {
    assertThrows(IdValidityException.class, () -> {
      new IdTimeRange(
        OffsetDateTime.now(),
        OffsetDateTime.now().minusDays(1L)
      );
    });
  }

  /**
   * The largest time range always contains today.
   */

  @Test
  public void testToday()
  {
    final var today = OffsetDateTime.now();
    final var largest = IdTimeRange.largest();
    assertTrue(largest.timeLower().compareTo(today) < 0);
    assertTrue(largest.timeUpper().compareTo(today) > 0);
  }
}

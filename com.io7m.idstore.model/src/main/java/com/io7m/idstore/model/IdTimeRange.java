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

package com.io7m.idstore.model;

import java.time.OffsetDateTime;
import java.util.Objects;

import static java.time.ZoneOffset.UTC;

/**
 * An inclusive range of time.
 *
 * @param timeLower The lower time
 * @param timeUpper The upper time
 */

public record IdTimeRange(
  OffsetDateTime timeLower,
  OffsetDateTime timeUpper)
{
  private static final OffsetDateTime TIME_LOWER =
    OffsetDateTime.of(
      2000,
      1,
      1,
      0,
      0,
      0,
      0,
      UTC
    );

  private static final IdTimeRange LARGEST =
    new IdTimeRange(
      TIME_LOWER,
      TIME_LOWER.plusYears(100_000L)
    );

  /**
   * An inclusive range of time.
   *
   * @param timeLower The lower time
   * @param timeUpper The upper time
   */

  public IdTimeRange
  {
    Objects.requireNonNull(timeLower, "timeLower");
    Objects.requireNonNull(timeUpper, "timeUpper");

    if (timeLower.compareTo(timeUpper) > 0) {
      throw new IdValidityException(
        "Time upper %s must be >= lower time %s"
          .formatted(timeUpper, timeLower)
      );
    }
  }

  /**
   * @return The largest sensible time range
   */

  public static IdTimeRange largest()
  {
    return LARGEST;
  }
}

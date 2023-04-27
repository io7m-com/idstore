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

package com.io7m.idstore.tests.arbitraries;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.providers.TypeUsage;

import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;

/**
 * A provider of {@link OffsetDateTime} values.
 */

public final class IdArbOffsetDateTimeProvider extends IdArbAbstractProvider
{
  /**
   * A provider of values.
   */

  public IdArbOffsetDateTimeProvider()
  {

  }

  @Override
  public boolean canProvideFor(
    final TypeUsage targetType)
  {
    return targetType.isOfType(OffsetDateTime.class);
  }

  @Override
  public Set<Arbitrary<?>> provideFor(
    final TypeUsage targetType,
    final SubtypeProvider subtypeProvider)
  {
    final var years =
      Arbitraries.integers().between(0, 65534);
    final var months =
      Arbitraries.integers().between(1, 12);
    final var days =
      Arbitraries.integers().between(1, 31);
    final var hours =
      Arbitraries.integers().between(0, 23);
    final var minutes =
      Arbitraries.integers().between(0, 59);
    final var seconds =
      Arbitraries.integers().between(0, 59);

    final var a =
      Combinators.combine(years, months, days, hours, minutes, seconds)
        .as((y, m, d, hr, min, sec) -> {
          try {
            return OffsetDateTime.of(
              y.intValue(),
              m.intValue(),
              d.intValue(),
              hr.intValue(),
              min.intValue(),
              sec.intValue(),
              0,
              ZoneOffset.UTC
            );
          } catch (final DateTimeException e) {
            return OffsetDateTime.of(
              0,
              1,
              1,
              hr.intValue(),
              min.intValue(),
              sec.intValue(),
              0,
              ZoneOffset.UTC
            );
          }
        });

    return Set.of(a);
  }
}

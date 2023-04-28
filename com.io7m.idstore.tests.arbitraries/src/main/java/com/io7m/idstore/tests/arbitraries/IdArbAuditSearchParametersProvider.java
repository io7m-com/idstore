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

import com.io7m.idstore.model.IdAuditSearchParameters;
import com.io7m.idstore.model.IdTimeRange;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.providers.TypeUsage;

import java.util.Optional;
import java.util.Set;

/**
 * A provider of {@link IdAuditSearchParameters} values.
 */

public final class IdArbAuditSearchParametersProvider
  extends IdArbAbstractProvider
{
  /**
   * A provider of values.
   */

  public IdArbAuditSearchParametersProvider()
  {

  }

  @Override
  public boolean canProvideFor(
    final TypeUsage targetType)
  {
    return targetType.isOfType(IdAuditSearchParameters.class);
  }

  @Override
  public Set<Arbitrary<?>> provideFor(
    final TypeUsage targetType,
    final SubtypeProvider subtypeProvider)
  {
    final var t =
      Arbitraries.defaultFor(IdTimeRange.class);
    final var s =
      Arbitraries.strings();
    final var i =
      Arbitraries.integers()
        .between(1, 1000);

    final var a =
      Combinators.combine(t, s, s, s, i).as((t0, ss0, ss1, ss2, in) -> {
        final var size = in.intValue();
        assert size >= 0;
        assert size <= 65535;
        return new IdAuditSearchParameters(
          t0,
          Optional.of(ss0),
          Optional.of(ss1),
          Optional.of(ss2),
          size
        );
      });

    return Set.of(a);
  }
}

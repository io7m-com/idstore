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

package com.io7m.idstore.tests.arbitraries;

import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.protocol.admin_v1.IdA1AdminSummary;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.providers.TypeUsage;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * A provider of {@link IdA1AdminSummary} values.
 */

public final class IdArbA1AdminSummaryProvider extends IdArbAbstractProvider
{
  /**
   * A provider of values.
   */

  public IdArbA1AdminSummaryProvider()
  {

  }

  @Override
  public boolean canProvideFor(
    final TypeUsage targetType)
  {
    return targetType.isOfType(IdA1AdminSummary.class);
  }

  @Override
  public Set<Arbitrary<?>> provideFor(
    final TypeUsage targetType,
    final SubtypeProvider subtypeProvider)
  {
    final var u =
      Arbitraries.defaultFor(UUID.class);
    final var rn =
      Arbitraries.defaultFor(IdRealName.class);
    final var un =
      Arbitraries.defaultFor(IdName.class);
    final var e =
      Arbitraries.defaultFor(IdEmail.class)
        .map(IdEmail::value)
        .list()
        .ofMinSize(1);
    final var t =
      Arbitraries.defaultFor(OffsetDateTime.class);

    final Arbitrary<IdA1AdminSummary> a =
      Combinators.combine(u, un, rn, t, t)
        .as((id1, idName, realName, created, updated) -> {
          return new IdA1AdminSummary(
            id1,
            idName.value(),
            realName.value(),
            created,
            updated
          );
        });

    return Set.of(a);
  }
}

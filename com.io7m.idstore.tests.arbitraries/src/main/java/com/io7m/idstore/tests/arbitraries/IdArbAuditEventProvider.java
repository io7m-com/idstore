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

import com.io7m.idstore.model.IdAuditEvent;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.providers.TypeUsage;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * A provider of {@link IdAuditEvent} values.
 */

public final class IdArbAuditEventProvider extends IdArbAbstractProvider
{
  /**
   * A provider of values.
   */

  public IdArbAuditEventProvider()
  {

  }

  @Override
  public boolean canProvideFor(
    final TypeUsage targetType)
  {
    return targetType.isOfType(IdAuditEvent.class);
  }

  @Override
  public Set<Arbitrary<?>> provideFor(
    final TypeUsage targetType,
    final SubtypeProvider subtypeProvider)
  {
    final var l =
      Arbitraries.longs();
    final var u =
      Arbitraries.defaultFor(UUID.class);
    final var t =
      Arbitraries.defaultFor(OffsetDateTime.class);
    final var s0 =
      Arbitraries.strings();
    final var s1 =
      Arbitraries.strings();

    return Set.of(
      Combinators.combine(l, u, t, s0, s1)
        .as((ul, ui, ut, us0, us1) -> {
          return new IdAuditEvent(
            ul.longValue(),
            ui,
            ut,
            us0,
            us1
          );
        }));
  }
}

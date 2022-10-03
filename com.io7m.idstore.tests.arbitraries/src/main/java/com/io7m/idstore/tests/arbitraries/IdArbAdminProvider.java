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

import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdAdminPermission;
import com.io7m.idstore.model.IdAdminPermissionSet;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdNonEmptyList;
import com.io7m.idstore.model.IdPassword;
import com.io7m.idstore.model.IdRealName;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.providers.TypeUsage;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * A provider of {@link IdAdmin} values.
 */

public final class IdArbAdminProvider extends IdArbAbstractProvider
{
  /**
   * A provider of values.
   */

  public IdArbAdminProvider()
  {

  }

  @Override
  public boolean canProvideFor(
    final TypeUsage targetType)
  {
    return targetType.isOfType(IdAdmin.class);
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
        .list()
        .ofMinSize(1)
        .map(IdNonEmptyList::ofList);
    final var t =
      Arbitraries.defaultFor(OffsetDateTime.class);
    final var p =
      Arbitraries.defaultFor(IdPassword.class);
    final var pp =
      Arbitraries.defaultFor(IdAdminPermission.class)
        .set()
        .map(IdAdminPermissionSet::of);

    final Arbitrary<IdAdmin> a =
      Combinators.combine(u, un, rn, e, t, t, p, pp)
        .as(IdAdmin::new);

    return Set.of(a);
  }
}

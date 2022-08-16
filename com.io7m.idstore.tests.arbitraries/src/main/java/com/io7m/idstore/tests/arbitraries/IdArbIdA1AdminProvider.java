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
import com.io7m.idstore.protocol.admin_v1.IdA1Admin;
import com.io7m.idstore.protocol.admin_v1.IdA1AdminPermission;
import com.io7m.idstore.protocol.admin_v1.IdA1Password;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.providers.TypeUsage;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * A provider of {@link IdA1Admin} values.
 */

public final class IdArbIdA1AdminProvider extends IdArbAbstractProvider
{
  /**
   * A provider of values.
   */

  public IdArbIdA1AdminProvider()
  {

  }

  @Override
  public boolean canProvideFor(
    final TypeUsage targetType)
  {
    return targetType.isOfType(IdA1Admin.class);
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
    final var p =
      Arbitraries.defaultFor(IdA1Password.class);
    final var pp =
      Arbitraries.defaultFor(IdA1AdminPermission.class)
        .set();

    final Arbitrary<IdA1Admin> a =
      Combinators.combine(u, un, rn, e, t, t, p, pp)
        .as((id1, idName, realName, email1, created, lastLoginTime, password, perms) -> {
          return new IdA1Admin(
            id1,
            idName.value(),
            realName.value(),
            email1,
            created,
            lastLoginTime,
            password,
            perms
          );
        });

    return Set.of(a);
  }
}

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

import com.io7m.idstore.protocol.admin_v1.IdA1Admin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSelf;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandLogin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserListParametersSet;
import com.io7m.idstore.protocol.admin_v1.IdA1MessageType;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminSelf;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseError;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseLogin;
import com.io7m.idstore.protocol.admin_v1.IdA1UserListParameters;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.providers.TypeUsage;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * A provider of {@link IdA1MessageType} values.
 */

public final class IdArbA1MessageProvider extends IdArbAbstractProvider
{
  /**
   * A provider of values.
   */

  public IdArbA1MessageProvider()
  {

  }

  @Override
  public boolean canProvideFor(
    final TypeUsage targetType)
  {
    return targetType.isOfType(IdA1MessageType.class);
  }

  @Override
  public Set<Arbitrary<?>> provideFor(
    final TypeUsage targetType,
    final SubtypeProvider subtypeProvider)
  {
    return Set.of(
      commandLogin(),
      commandAdminSelf(),
      commandUserListParametersSet(),
      responseLogin(),
      responseError(),
      responseAdminSelf()
    );
  }

  private static Arbitrary<IdA1ResponseAdminSelf> responseAdminSelf()
  {
    final var id =
      Arbitraries.defaultFor(UUID.class);
    final var a =
      Arbitraries.defaultFor(IdA1Admin.class);

    return Combinators.combine(id, a).as(IdA1ResponseAdminSelf::new);
  }

  private static Arbitrary<IdA1ResponseError> responseError()
  {
    final var id =
      Arbitraries.defaultFor(UUID.class);
    final var s0 =
      Arbitraries.strings();
    final var s1 =
      Arbitraries.strings();

    return Combinators.combine(id, s0, s1).as(IdA1ResponseError::new);
  }

  private static Arbitrary<IdA1ResponseLogin> responseLogin()
  {
    final var id =
      Arbitraries.defaultFor(UUID.class);
    final var t =
      Arbitraries.defaultFor(OffsetDateTime.class);

    return Combinators.combine(id, t).as(IdA1ResponseLogin::new);
  }

  private static Arbitrary<IdA1CommandAdminSelf> commandAdminSelf()
  {
    return Arbitraries.integers().map(i -> new IdA1CommandAdminSelf());
  }

  private static Arbitrary<IdA1CommandLogin> commandLogin()
  {
    final var s0 =
      Arbitraries.strings();
    final var s1 =
      Arbitraries.strings();

    return Combinators.combine(s0, s1).as(IdA1CommandLogin::new);
  }

  private static Arbitrary<IdA1CommandUserListParametersSet> commandUserListParametersSet()
  {
    return Arbitraries.defaultFor(IdA1UserListParameters.class)
      .map(IdA1CommandUserListParametersSet::new);
  }
}

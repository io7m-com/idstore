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

import com.io7m.idstore.protocol.user_v1.IdU1CommandLogin;
import com.io7m.idstore.protocol.user_v1.IdU1CommandUserSelf;
import com.io7m.idstore.protocol.user_v1.IdU1MessageType;
import com.io7m.idstore.protocol.user_v1.IdU1ResponseError;
import com.io7m.idstore.protocol.user_v1.IdU1ResponseLogin;
import com.io7m.idstore.protocol.user_v1.IdU1ResponseUserSelf;
import com.io7m.idstore.protocol.user_v1.IdU1User;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.providers.TypeUsage;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * A provider of {@link IdU1MessageType} values.
 */

public final class IdArbU1MessageProvider extends IdArbAbstractProvider
{
  /**
   * A provider of values.
   */

  public IdArbU1MessageProvider()
  {

  }

  @Override
  public boolean canProvideFor(
    final TypeUsage targetType)
  {
    return targetType.isOfType(IdU1MessageType.class);
  }

  @Override
  public Set<Arbitrary<?>> provideFor(
    final TypeUsage targetType,
    final SubtypeProvider subtypeProvider)
  {
    return Set.of(
      commandLogin(),
      commandUserSelf(),
      responseLogin(),
      responseError(),
      responseUserSelf()
    );
  }

  private static Arbitrary<IdU1ResponseUserSelf> responseUserSelf()
  {
    final var id =
      Arbitraries.defaultFor(UUID.class);
    final var a =
      Arbitraries.defaultFor(IdU1User.class);

    return Combinators.combine(id, a).as(IdU1ResponseUserSelf::new);
  }

  private static Arbitrary<IdU1ResponseError> responseError()
  {
    final var id =
      Arbitraries.defaultFor(UUID.class);
    final var s0 =
      Arbitraries.strings();
    final var s1 =
      Arbitraries.strings();

    return Combinators.combine(id, s0, s1).as(IdU1ResponseError::new);
  }

  private static Arbitrary<IdU1ResponseLogin> responseLogin()
  {
    final var id =
      Arbitraries.defaultFor(UUID.class);
    final var t =
      Arbitraries.defaultFor(OffsetDateTime.class);

    return Combinators.combine(id, t).as(IdU1ResponseLogin::new);
  }

  private static Arbitrary<IdU1CommandUserSelf> commandUserSelf()
  {
    return Arbitraries.integers().map(i -> new IdU1CommandUserSelf());
  }

  private static Arbitrary<IdU1CommandLogin> commandLogin()
  {
    final var s0 =
      Arbitraries.strings();
    final var s1 =
      Arbitraries.strings();

    return Combinators.combine(s0, s1).as(IdU1CommandLogin::new);
  }
}

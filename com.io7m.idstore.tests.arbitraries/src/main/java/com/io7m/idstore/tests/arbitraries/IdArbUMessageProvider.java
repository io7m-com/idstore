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
import com.io7m.idstore.model.IdToken;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.protocol.user.IdUCommandEmailAddBegin;
import com.io7m.idstore.protocol.user.IdUCommandEmailAddDeny;
import com.io7m.idstore.protocol.user.IdUCommandEmailAddPermit;
import com.io7m.idstore.protocol.user.IdUCommandEmailRemoveBegin;
import com.io7m.idstore.protocol.user.IdUCommandEmailRemoveDeny;
import com.io7m.idstore.protocol.user.IdUCommandEmailRemovePermit;
import com.io7m.idstore.protocol.user.IdUCommandLogin;
import com.io7m.idstore.protocol.user.IdUCommandPasswordUpdate;
import com.io7m.idstore.protocol.user.IdUCommandRealnameUpdate;
import com.io7m.idstore.protocol.user.IdUCommandUserSelf;
import com.io7m.idstore.protocol.user.IdUMessageType;
import com.io7m.idstore.protocol.user.IdUResponseEmailAddBegin;
import com.io7m.idstore.protocol.user.IdUResponseEmailAddDeny;
import com.io7m.idstore.protocol.user.IdUResponseEmailAddPermit;
import com.io7m.idstore.protocol.user.IdUResponseEmailRemoveBegin;
import com.io7m.idstore.protocol.user.IdUResponseEmailRemoveDeny;
import com.io7m.idstore.protocol.user.IdUResponseEmailRemovePermit;
import com.io7m.idstore.protocol.user.IdUResponseError;
import com.io7m.idstore.protocol.user.IdUResponseLogin;
import com.io7m.idstore.protocol.user.IdUResponseUserSelf;
import com.io7m.idstore.protocol.user.IdUResponseUserUpdate;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.providers.TypeUsage;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

/**
 * A provider of {@link IdUMessageType} values.
 */

public final class IdArbUMessageProvider extends IdArbAbstractProvider
{
  /**
   * A provider of values.
   */

  public IdArbUMessageProvider()
  {

  }

  @Override
  public boolean canProvideFor(
    final TypeUsage targetType)
  {
    return targetType.isOfType(IdUMessageType.class);
  }

  @Override
  public Set<Arbitrary<?>> provideFor(
    final TypeUsage targetType,
    final SubtypeProvider subtypeProvider)
  {
    return Set.of(
      commandEmailAddBegin(),
      commandEmailAddDeny(),
      commandEmailAddPermit(),
      commandEmailRemoveBegin(),
      commandEmailRemoveDeny(),
      commandEmailRemovePermit(),
      commandLogin(),
      commandUserSelf(),
      commandUserRealnameUpdate(),
      commandUserPasswordUpdate(),
      responseEmailAddBegin(),
      responseEmailAddDeny(),
      responseEmailAddPermit(),
      responseEmailRemoveBegin(),
      responseEmailRemoveDeny(),
      responseEmailRemovePermit(),
      responseError(),
      responseLogin(),
      responseUserSelf(),
      responseUserUpdate()
    );
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdUCommandEmailAddBegin> commandEmailAddBegin()
  {
    return Arbitraries.defaultFor(IdEmail.class)
      .map(IdUCommandEmailAddBegin::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdUCommandEmailRemoveBegin> commandEmailRemoveBegin()
  {
    return Arbitraries.defaultFor(IdEmail.class)
      .map(IdUCommandEmailRemoveBegin::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdUCommandEmailAddPermit> commandEmailAddPermit()
  {
    return Arbitraries.defaultFor(IdToken.class)
      .map(IdUCommandEmailAddPermit::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdUCommandEmailRemovePermit> commandEmailRemovePermit()
  {
    return Arbitraries.defaultFor(IdToken.class)
      .map(IdUCommandEmailRemovePermit::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdUCommandEmailAddDeny> commandEmailAddDeny()
  {
    return Arbitraries.defaultFor(IdToken.class)
      .map(IdUCommandEmailAddDeny::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdUCommandEmailRemoveDeny> commandEmailRemoveDeny()
  {
    return Arbitraries.defaultFor(IdToken.class)
      .map(IdUCommandEmailRemoveDeny::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdUCommandRealnameUpdate> commandUserRealnameUpdate()
  {
    return Arbitraries.defaultFor(IdRealName.class)
      .map(IdUCommandRealnameUpdate::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdUCommandPasswordUpdate> commandUserPasswordUpdate()
  {
    return Combinators.combine(Arbitraries.strings(), Arbitraries.strings())
      .as(IdUCommandPasswordUpdate::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdUResponseEmailAddBegin> responseEmailAddBegin()
  {
    return Arbitraries.defaultFor(UUID.class)
      .map(IdUResponseEmailAddBegin::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdUResponseEmailAddPermit> responseEmailAddPermit()
  {
    return Arbitraries.defaultFor(UUID.class)
      .map(IdUResponseEmailAddPermit::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdUResponseEmailAddDeny> responseEmailAddDeny()
  {
    return Arbitraries.defaultFor(UUID.class)
      .map(IdUResponseEmailAddDeny::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdUResponseEmailRemoveBegin> responseEmailRemoveBegin()
  {
    return Arbitraries.defaultFor(UUID.class)
      .map(IdUResponseEmailRemoveBegin::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdUResponseEmailRemovePermit> responseEmailRemovePermit()
  {
    return Arbitraries.defaultFor(UUID.class)
      .map(IdUResponseEmailRemovePermit::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdUResponseEmailRemoveDeny> responseEmailRemoveDeny()
  {
    return Arbitraries.defaultFor(UUID.class)
      .map(IdUResponseEmailRemoveDeny::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdUResponseUserSelf> responseUserSelf()
  {
    final var id =
      Arbitraries.defaultFor(UUID.class);
    final var a =
      Arbitraries.defaultFor(IdUser.class);

    return Combinators.combine(id, a).as(IdUResponseUserSelf::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdUResponseUserUpdate> responseUserUpdate()
  {
    final var id =
      Arbitraries.defaultFor(UUID.class);
    final var a =
      Arbitraries.defaultFor(IdUser.class);

    return Combinators.combine(id, a).as(IdUResponseUserUpdate::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdUResponseError> responseError()
  {
    final var id =
      Arbitraries.defaultFor(UUID.class);
    final var s0 =
      Arbitraries.strings();
    final var s1 =
      Arbitraries.strings();

    final var s2 =
      Arbitraries.strings();
    final var s3 =
      Arbitraries.strings();
    final var ms =
      Arbitraries.maps(s2, s3);

    final var os =
      Arbitraries.strings()
        .optional();

    return Combinators.combine(id, s0, s1, ms, os).as(IdUResponseError::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdUResponseLogin> responseLogin()
  {
    final var id =
      Arbitraries.defaultFor(UUID.class);
    final var users =
      Arbitraries.defaultFor(IdUser.class);

    return Combinators.combine(id, users).as(IdUResponseLogin::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdUCommandUserSelf> commandUserSelf()
  {
    return Arbitraries.integers().map(i -> new IdUCommandUserSelf());
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdUCommandLogin> commandLogin()
  {
    final var s0 =
      Arbitraries.defaultFor(IdName.class);
    final var s1 =
      Arbitraries.strings();
    final var s2 =
      Arbitraries.defaultFor(String.class)
        .tuple2()
        .list()
        .map(tuple2s -> {
          final var map = new HashMap<String, String>();
          for (final var t : tuple2s) {
            map.put(t.get1(), t.get2());
          }
          return map;
        });

    return Combinators.combine(s0, s1, s2).as(IdUCommandLogin::new);
  }
}

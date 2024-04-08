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

import com.io7m.idstore.error_codes.IdErrorCode;
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdAdminPermission;
import com.io7m.idstore.model.IdAdminSearchByEmailParameters;
import com.io7m.idstore.model.IdAdminSearchParameters;
import com.io7m.idstore.model.IdAdminSummary;
import com.io7m.idstore.model.IdAuditEvent;
import com.io7m.idstore.model.IdAuditSearchParameters;
import com.io7m.idstore.model.IdBan;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdLogin;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPage;
import com.io7m.idstore.model.IdShortHumanToken;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.model.IdUserSearchByEmailParameters;
import com.io7m.idstore.model.IdUserSearchParameters;
import com.io7m.idstore.model.IdUserSummary;
import com.io7m.idstore.protocol.admin.IdACommandAdminBanCreate;
import com.io7m.idstore.protocol.admin.IdACommandAdminBanDelete;
import com.io7m.idstore.protocol.admin.IdACommandAdminBanGet;
import com.io7m.idstore.protocol.admin.IdACommandAdminCreate;
import com.io7m.idstore.protocol.admin.IdACommandAdminDelete;
import com.io7m.idstore.protocol.admin.IdACommandAdminEmailAdd;
import com.io7m.idstore.protocol.admin.IdACommandAdminEmailRemove;
import com.io7m.idstore.protocol.admin.IdACommandAdminGet;
import com.io7m.idstore.protocol.admin.IdACommandAdminGetByEmail;
import com.io7m.idstore.protocol.admin.IdACommandAdminPermissionGrant;
import com.io7m.idstore.protocol.admin.IdACommandAdminPermissionRevoke;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchBegin;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchByEmailBegin;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchByEmailNext;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchNext;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchPrevious;
import com.io7m.idstore.protocol.admin.IdACommandAdminSelf;
import com.io7m.idstore.protocol.admin.IdACommandAdminUpdateCredentials;
import com.io7m.idstore.protocol.admin.IdACommandAdminUpdatePasswordExpiration;
import com.io7m.idstore.protocol.admin.IdACommandAuditSearchBegin;
import com.io7m.idstore.protocol.admin.IdACommandAuditSearchNext;
import com.io7m.idstore.protocol.admin.IdACommandAuditSearchPrevious;
import com.io7m.idstore.protocol.admin.IdACommandLogin;
import com.io7m.idstore.protocol.admin.IdACommandMailTest;
import com.io7m.idstore.protocol.admin.IdACommandMaintenanceModeSet;
import com.io7m.idstore.protocol.admin.IdACommandUserBanCreate;
import com.io7m.idstore.protocol.admin.IdACommandUserBanDelete;
import com.io7m.idstore.protocol.admin.IdACommandUserBanGet;
import com.io7m.idstore.protocol.admin.IdACommandUserCreate;
import com.io7m.idstore.protocol.admin.IdACommandUserDelete;
import com.io7m.idstore.protocol.admin.IdACommandUserEmailAdd;
import com.io7m.idstore.protocol.admin.IdACommandUserEmailRemove;
import com.io7m.idstore.protocol.admin.IdACommandUserGet;
import com.io7m.idstore.protocol.admin.IdACommandUserGetByEmail;
import com.io7m.idstore.protocol.admin.IdACommandUserLoginHistory;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchBegin;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchByEmailBegin;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchByEmailNext;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchNext;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchPrevious;
import com.io7m.idstore.protocol.admin.IdACommandUserUpdateCredentials;
import com.io7m.idstore.protocol.admin.IdACommandUserUpdatePasswordExpiration;
import com.io7m.idstore.protocol.admin.IdAMessageType;
import com.io7m.idstore.protocol.admin.IdAPasswordExpirationSetNever;
import com.io7m.idstore.protocol.admin.IdAPasswordExpirationSetRefresh;
import com.io7m.idstore.protocol.admin.IdAPasswordExpirationSetSpecific;
import com.io7m.idstore.protocol.admin.IdAPasswordExpirationSetType;
import com.io7m.idstore.protocol.admin.IdAResponseAdminBanCreate;
import com.io7m.idstore.protocol.admin.IdAResponseAdminBanDelete;
import com.io7m.idstore.protocol.admin.IdAResponseAdminBanGet;
import com.io7m.idstore.protocol.admin.IdAResponseAdminCreate;
import com.io7m.idstore.protocol.admin.IdAResponseAdminDelete;
import com.io7m.idstore.protocol.admin.IdAResponseAdminGet;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSearchBegin;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSearchByEmailBegin;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSearchByEmailNext;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSearchNext;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSearchPrevious;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSelf;
import com.io7m.idstore.protocol.admin.IdAResponseAdminUpdate;
import com.io7m.idstore.protocol.admin.IdAResponseAuditSearchBegin;
import com.io7m.idstore.protocol.admin.IdAResponseAuditSearchNext;
import com.io7m.idstore.protocol.admin.IdAResponseAuditSearchPrevious;
import com.io7m.idstore.protocol.admin.IdAResponseBlame;
import com.io7m.idstore.protocol.admin.IdAResponseError;
import com.io7m.idstore.protocol.admin.IdAResponseLogin;
import com.io7m.idstore.protocol.admin.IdAResponseMailTest;
import com.io7m.idstore.protocol.admin.IdAResponseMaintenanceModeSet;
import com.io7m.idstore.protocol.admin.IdAResponseUserBanCreate;
import com.io7m.idstore.protocol.admin.IdAResponseUserBanDelete;
import com.io7m.idstore.protocol.admin.IdAResponseUserBanGet;
import com.io7m.idstore.protocol.admin.IdAResponseUserCreate;
import com.io7m.idstore.protocol.admin.IdAResponseUserDelete;
import com.io7m.idstore.protocol.admin.IdAResponseUserGet;
import com.io7m.idstore.protocol.admin.IdAResponseUserLoginHistory;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchBegin;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchByEmailBegin;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchByEmailNext;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchNext;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchPrevious;
import com.io7m.idstore.protocol.admin.IdAResponseUserUpdate;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.providers.TypeUsage;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A provider of {@link IdAMessageType} values.
 */

public final class IdArbAMessageProvider extends IdArbAbstractProvider
{
  /**
   * A provider of values.
   */

  public IdArbAMessageProvider()
  {

  }

  @Override
  public boolean canProvideFor(
    final TypeUsage targetType)
  {
    return targetType.isOfType(IdAMessageType.class);
  }

  @Override
  public Set<Arbitrary<?>> provideFor(
    final TypeUsage targetType,
    final SubtypeProvider subtypeProvider)
  {
    return Set.of(
      commandAdminBanCreate(),
      commandAdminBanDelete(),
      commandAdminBanGet(),
      commandAdminCreate(),
      commandAdminDelete(),
      commandAdminEmailAdd(),
      commandAdminEmailRemove(),
      commandAdminGet(),
      commandAdminGetByEmail(),
      commandAdminPermissionGrant(),
      commandAdminPermissionRevoke(),
      commandAdminSearchBegin(),
      commandAdminSearchByEmailBegin(),
      commandAdminSearchByEmailNext(),
      commandAdminSearchByEmailPrevious(),
      commandAdminSearchNext(),
      commandAdminSearchPrevious(),
      commandAdminSelf(),
      commandAdminUpdate(),
      commandAdminUpdatePasswordExpiration(),
      commandAuditSearchBegin(),
      commandAuditSearchNext(),
      commandAuditSearchPrevious(),
      commandLogin(),
      commandMailTest(),
      commandMaintenanceModeSet(),
      commandUserBanCreate(),
      commandUserBanDelete(),
      commandUserBanGet(),
      commandUserCreate(),
      commandUserDelete(),
      commandUserEmailAdd(),
      commandUserEmailRemove(),
      commandUserGet(),
      commandUserGetByEmail(),
      commandUserLoginHistory(),
      commandUserSearchBegin(),
      commandUserSearchByEmailBegin(),
      commandUserSearchByEmailNext(),
      commandUserSearchByEmailPrevious(),
      commandUserSearchNext(),
      commandUserSearchPrevious(),
      commandUserUpdate(),
      commandUserUpdatePasswordExpiration(),
      responseAdminBanCreate(),
      responseAdminBanDelete(),
      responseAdminBanGet(),
      responseAdminCreate(),
      responseAdminDelete(),
      responseAdminGet(),
      responseAdminSearchBegin(),
      responseAdminSearchByEmailBegin(),
      responseAdminSearchByEmailNext(),
      responseAdminSearchByEmailPrevious(),
      responseAdminSearchNext(),
      responseAdminSearchPrevious(),
      responseAdminSelf(),
      responseAdminUpdate(),
      responseAuditSearchBegin(),
      responseAuditSearchNext(),
      responseAuditSearchPrevious(),
      responseError(),
      responseLogin(),
      responseMailTest(),
      responseMaintenanceModeSet(),
      responseUserBanCreate(),
      responseUserBanDelete(),
      responseUserBanGet(),
      responseUserCreate(),
      responseUserDelete(),
      responseUserGet(),
      responseUserLoginHistory(),
      responseUserSearchBegin(),
      responseUserSearchByEmailBegin(),
      responseUserSearchByEmailNext(),
      responseUserSearchByEmailPrevious(),
      responseUserSearchNext(),
      responseUserSearchPrevious(),
      responseUserUpdate()
    );
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseMaintenanceModeSet> responseMaintenanceModeSet()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);
    final var a_s =
      Arbitraries.strings();

    return Combinators.combine(msgId, corId, a_s)
      .as(IdAResponseMaintenanceModeSet::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandMaintenanceModeSet> commandMaintenanceModeSet()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var msg =
      Arbitraries.strings()
        .optional();

    return Combinators.combine(msgId, msg)
      .as(IdACommandMaintenanceModeSet::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAPasswordExpirationSetType> passwordExpirationSet()
  {
    final var a_id =
      Arbitraries.defaultFor(UUID.class);
    final var a_i =
      Arbitraries.integers()
        .between(0, 2);

    return Combinators.combine(a_id, a_i)
      .as((uuid, integer) -> {
        return switch (integer.intValue()) {
          case 0 -> new IdAPasswordExpirationSetNever();
          case 1 -> new IdAPasswordExpirationSetRefresh();
          case 2 -> new IdAPasswordExpirationSetSpecific(OffsetDateTime.now());
          default -> throw new IllegalStateException();
        };
      });
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandUserUpdatePasswordExpiration> commandUserUpdatePasswordExpiration()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var a_id =
      Arbitraries.defaultFor(UUID.class);

    return Combinators.combine(msgId, a_id, passwordExpirationSet())
      .as(IdACommandUserUpdatePasswordExpiration::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandAdminUpdatePasswordExpiration> commandAdminUpdatePasswordExpiration()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var a_id =
      Arbitraries.defaultFor(UUID.class);

    return Combinators.combine(msgId, a_id, passwordExpirationSet())
      .as(IdACommandAdminUpdatePasswordExpiration::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseUserSearchBegin> responseUserSearchBegin()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);
    final var a_s =
      Arbitraries.defaultFor(IdUserSummary.class)
        .list();
    final var a_i =
      Arbitraries.integers();

    return Combinators.combine(msgId, corId, a_s, a_i, a_i, a_i)
      .as((m0, c0, summaries, x0, x1, x2) -> {
        return new IdAResponseUserSearchBegin(
          m0,
          c0,
          new IdPage<>(
            summaries,
            x0.intValue(),
            x1.intValue(),
            x2.intValue()
          )
        );
      });
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseUserSearchPrevious> responseUserSearchPrevious()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);
    final var a_s =
      Arbitraries.defaultFor(IdUserSummary.class)
        .list();
    final var a_i =
      Arbitraries.integers();

    return Combinators.combine(msgId, corId, a_s, a_i, a_i, a_i)
      .as((m0, c0, summaries, x0, x1, x2) -> {
        return new IdAResponseUserSearchPrevious(
          m0,
          c0,
          new IdPage<>(
            summaries,
            x0.intValue(),
            x1.intValue(),
            x2.intValue()
          )
        );
      });
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseUserSearchNext> responseUserSearchNext()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);
    final var a_s =
      Arbitraries.defaultFor(IdUserSummary.class)
        .list();
    final var a_i =
      Arbitraries.integers();

    return Combinators.combine(msgId, corId, a_s, a_i, a_i, a_i)
      .as((m0, c0, summaries, x0, x1, x2) -> {
        return new IdAResponseUserSearchNext(
          m0,
          c0,
          new IdPage<>(
            summaries,
            x0.intValue(),
            x1.intValue(),
            x2.intValue()
          )
        );
      });
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseAdminSelf> responseAdminSelf()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);
    final var a =
      Arbitraries.defaultFor(IdAdmin.class);

    return Combinators.combine(msgId, corId, a)
      .as(IdAResponseAdminSelf::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseError> responseError()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);

    final var s0 =
      Arbitraries.strings();
    final var s1 =
      Arbitraries.defaultFor(IdErrorCode.class);
    final var b =
      Arbitraries.defaultFor(IdAResponseBlame.class);

    final var s2 =
      Arbitraries.strings();
    final var s3 =
      Arbitraries.strings();
    final var ms =
      Arbitraries.maps(s2, s3);

    final var os =
      Arbitraries.strings()
        .optional();

    return Combinators.combine(msgId, corId, s0, s1, ms, os, b)
      .as(IdAResponseError::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseLogin> responseLogin()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);
    final var admins =
      Arbitraries.defaultFor(IdAdmin.class);

    return Combinators.combine(msgId, corId, admins)
      .as(IdAResponseLogin::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseUserGet> responseUserGet()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);
    final var users =
      Arbitraries.defaultFor(IdUser.class);

    return Combinators.combine(msgId, corId, users)
      .as((m0, c0, user) -> {
        return new IdAResponseUserGet(
          m0,
          c0,
          Optional.of(user)
        );
      });
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseUserCreate> responseUserCreate()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);
    final var users =
      Arbitraries.defaultFor(IdUser.class);

    return Combinators.combine(msgId, corId, users)
      .as(IdAResponseUserCreate::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseUserUpdate> responseUserUpdate()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);
    final var users =
      Arbitraries.defaultFor(IdUser.class);

    return Combinators.combine(msgId, corId, users)
      .as(IdAResponseUserUpdate::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandAdminSelf> commandAdminSelf()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);

    return msgId.map(IdACommandAdminSelf::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandUserGet> commandUserGet()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var id =
      Arbitraries.defaultFor(UUID.class);

    return Combinators.combine(msgId, id)
      .as(IdACommandUserGet::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandUserGetByEmail> commandUserGetByEmail()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var id =
      Arbitraries.defaultFor(IdEmail.class);

    return Combinators.combine(msgId, id)
      .as(IdACommandUserGetByEmail::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandLogin> commandLogin()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
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

    return Combinators.combine(msgId, s0, s1, s2)
      .as(IdACommandLogin::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandUserSearchBegin> commandUserSearchBegin()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var p =
      Arbitraries.defaultFor(IdUserSearchParameters.class);

    return Combinators.combine(msgId, p)
      .as(IdACommandUserSearchBegin::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandUserSearchNext> commandUserSearchNext()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);

    return msgId.map(IdACommandUserSearchNext::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandUserSearchPrevious> commandUserSearchPrevious()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);

    return msgId.map(IdACommandUserSearchPrevious::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandUserSearchByEmailBegin> commandUserSearchByEmailBegin()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var p =
      Arbitraries.defaultFor(IdUserSearchByEmailParameters.class);

    return Combinators.combine(msgId, p)
      .as(IdACommandUserSearchByEmailBegin::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandUserSearchByEmailNext> commandUserSearchByEmailNext()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);

    return msgId.map(IdACommandUserSearchByEmailNext::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandUserSearchByEmailPrevious> commandUserSearchByEmailPrevious()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);

    return msgId.map(IdACommandUserSearchByEmailPrevious::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandUserCreate> commandUserCreate()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var users =
      Arbitraries.defaultFor(IdUser.class);

    return Combinators.combine(msgId, users)
      .as((id, user) -> {
        return new IdACommandUserCreate(
          id,
          Optional.of(user.id()),
          user.idName(),
          user.realName(),
          user.emails().first(),
          user.password()
        );
      });
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandUserUpdateCredentials> commandUserUpdate()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var users =
      Arbitraries.defaultFor(IdUser.class);

    return Combinators.combine(msgId, users)
      .as((id, user) -> {
      return new IdACommandUserUpdateCredentials(
        id,
        user.id(),
        Optional.of(user.idName()),
        Optional.of(user.realName()),
        Optional.of(user.password())
      );
    });
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandAuditSearchBegin> commandAuditSearchBegin()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var p =
      Arbitraries.defaultFor(IdAuditSearchParameters.class);

    return Combinators.combine(msgId, p)
      .as(IdACommandAuditSearchBegin::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandAuditSearchNext> commandAuditSearchNext()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);

    return msgId.map(IdACommandAuditSearchNext::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandAuditSearchPrevious> commandAuditSearchPrevious()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);

    return msgId.map(IdACommandAuditSearchPrevious::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandMailTest> commandMailTest()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var e =
      Arbitraries.defaultFor(IdEmail.class);
    final var t =
      Arbitraries.defaultFor(IdShortHumanToken.class);

    return Combinators.combine(msgId, e, t).as(IdACommandMailTest::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseMailTest> responseMailTest()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);

    final var t =
      Arbitraries.defaultFor(IdShortHumanToken.class);

    return Combinators.combine(msgId, corId, t)
      .as(IdAResponseMailTest::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseAuditSearchBegin> responseAuditSearchBegin()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);

    final var a_s =
      Arbitraries.defaultFor(IdAuditEvent.class)
        .list();
    final var a_i =
      Arbitraries.integers();

    return Combinators.combine(msgId, corId, a_s, a_i, a_i, a_i)
      .as((m0, c0, summaries, x0, x1, x2) -> {
        return new IdAResponseAuditSearchBegin(
          m0,
          c0,
          new IdPage<>(
            summaries,
            x0.intValue(),
            x1.intValue(),
            x2.intValue()
          )
        );
      });
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseAuditSearchPrevious> responseAuditSearchPrevious()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);

    final var a_s =
      Arbitraries.defaultFor(IdAuditEvent.class)
        .list();
    final var a_i =
      Arbitraries.integers();

    return Combinators.combine(msgId, corId, a_s, a_i, a_i, a_i)
      .as((m0, c0, summaries, x0, x1, x2) -> {
        return new IdAResponseAuditSearchPrevious(
          m0,
          c0,
          new IdPage<>(
            summaries,
            x0.intValue(),
            x1.intValue(),
            x2.intValue()
          )
        );
      });
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseAuditSearchNext> responseAuditSearchNext()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);

    final var a_s =
      Arbitraries.defaultFor(IdAuditEvent.class)
        .list();
    final var a_i =
      Arbitraries.integers();

    return Combinators.combine(msgId, corId, a_s, a_i, a_i, a_i)
      .as((m0, c0, summaries, x0, x1, x2) -> {
        return new IdAResponseAuditSearchNext(
          m0,
          c0,
          new IdPage<>(
            summaries,
            x0.intValue(),
            x1.intValue(),
            x2.intValue()
          )
        );
      });
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseUserSearchByEmailBegin> responseUserSearchByEmailBegin()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);

    final var a_s =
      Arbitraries.defaultFor(IdUserSummary.class)
        .list();
    final var a_i =
      Arbitraries.integers();

    return Combinators.combine(msgId, corId, a_s, a_i, a_i, a_i)
      .as((m0, c0, summaries, x0, x1, x2) -> {
        return new IdAResponseUserSearchByEmailBegin(
          m0,
          c0,
          new IdPage<>(
            summaries,
            x0.intValue(),
            x1.intValue(),
            x2.intValue()
          )
        );
      });
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseUserSearchByEmailPrevious> responseUserSearchByEmailPrevious()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);

    final var a_s =
      Arbitraries.defaultFor(IdUserSummary.class)
        .list();
    final var a_i =
      Arbitraries.integers();

    return Combinators.combine(msgId, corId, a_s, a_i, a_i, a_i)
      .as((m0, c0, summaries, x0, x1, x2) -> {
        return new IdAResponseUserSearchByEmailPrevious(
          m0,
          c0,
          new IdPage<>(
            summaries,
            x0.intValue(),
            x1.intValue(),
            x2.intValue()
          )
        );
      });
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseUserSearchByEmailNext> responseUserSearchByEmailNext()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);

    final var a_s =
      Arbitraries.defaultFor(IdUserSummary.class)
        .list();
    final var a_i =
      Arbitraries.integers();

    return Combinators.combine(msgId, corId, a_s, a_i, a_i, a_i)
      .as((m0, c0, summaries, x0, x1, x2) -> {
        return new IdAResponseUserSearchByEmailNext(
          m0,
          c0,
          new IdPage<>(
            summaries,
            x0.intValue(),
            x1.intValue(),
            x2.intValue()
          )
        );
      });
  }


  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandAdminSearchBegin> commandAdminSearchBegin()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var p =
      Arbitraries.defaultFor(IdAdminSearchParameters.class);

    return Combinators.combine(msgId, p)
      .as(IdACommandAdminSearchBegin::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandAdminSearchNext> commandAdminSearchNext()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);

    return msgId.map(IdACommandAdminSearchNext::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandAdminSearchPrevious> commandAdminSearchPrevious()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);

    return msgId.map(IdACommandAdminSearchPrevious::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandAdminSearchByEmailBegin> commandAdminSearchByEmailBegin()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var p =
      Arbitraries.defaultFor(IdAdminSearchByEmailParameters.class);

    return Combinators.combine(msgId, p)
      .as(IdACommandAdminSearchByEmailBegin::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandAdminSearchByEmailNext> commandAdminSearchByEmailNext()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);

    return msgId.map(IdACommandAdminSearchByEmailNext::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandAdminSearchByEmailPrevious> commandAdminSearchByEmailPrevious()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);

    return msgId.map(IdACommandAdminSearchByEmailPrevious::new);
  }


  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseAdminSearchByEmailBegin> responseAdminSearchByEmailBegin()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);

    final var a_s =
      Arbitraries.defaultFor(IdAdminSummary.class)
        .list();
    final var a_i =
      Arbitraries.integers();

    return Combinators.combine(msgId, corId, a_s, a_i, a_i, a_i)
      .as((m0, c0, summaries, x0, x1, x2) -> {
        return new IdAResponseAdminSearchByEmailBegin(
          m0,
          c0,
          new IdPage<>(
            summaries,
            x0.intValue(),
            x1.intValue(),
            x2.intValue()
          )
        );
      });
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseAdminSearchByEmailPrevious> responseAdminSearchByEmailPrevious()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);

    final var a_s =
      Arbitraries.defaultFor(IdAdminSummary.class)
        .list();
    final var a_i =
      Arbitraries.integers();

    return Combinators.combine(msgId, corId, a_s, a_i, a_i, a_i)
      .as((m0, c0, summaries, x0, x1, x2) -> {
        return new IdAResponseAdminSearchByEmailPrevious(
          m0,
          c0,
          new IdPage<>(
            summaries,
            x0.intValue(),
            x1.intValue(),
            x2.intValue()
          )
        );
      });
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseAdminSearchByEmailNext> responseAdminSearchByEmailNext()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);

    final var a_s =
      Arbitraries.defaultFor(IdAdminSummary.class)
        .list();
    final var a_i =
      Arbitraries.integers();

    return Combinators.combine(msgId, corId, a_s, a_i, a_i, a_i)
      .as((m0, c0, summaries, x0, x1, x2) -> {
        return new IdAResponseAdminSearchByEmailNext(
          m0,
          c0,
          new IdPage<>(
            summaries,
            x0.intValue(),
            x1.intValue(),
            x2.intValue()
          )
        );
      });
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseAdminSearchBegin> responseAdminSearchBegin()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);

    final var a_s =
      Arbitraries.defaultFor(IdAdminSummary.class)
        .list();
    final var a_i =
      Arbitraries.integers();

    return Combinators.combine(msgId, corId, a_s, a_i, a_i, a_i)
      .as((m0, c0, summaries, x0, x1, x2) -> {
        return new IdAResponseAdminSearchBegin(
          m0,
          c0,
          new IdPage<>(
            summaries,
            x0.intValue(),
            x1.intValue(),
            x2.intValue()
          )
        );
      });
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseAdminSearchPrevious> responseAdminSearchPrevious()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);
    final var a_s =
      Arbitraries.defaultFor(IdAdminSummary.class)
        .list();
    final var a_i =
      Arbitraries.integers();

    return Combinators.combine(msgId, corId, a_s, a_i, a_i, a_i)
      .as((m0, c0, summaries, x0, x1, x2) -> {
        return new IdAResponseAdminSearchPrevious(
          m0,
          c0,
          new IdPage<>(
            summaries,
            x0.intValue(),
            x1.intValue(),
            x2.intValue()
          )
        );
      });
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseAdminSearchNext> responseAdminSearchNext()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);

    final var a_s =
      Arbitraries.defaultFor(IdAdminSummary.class)
        .list();
    final var a_i =
      Arbitraries.integers();

    return Combinators.combine(msgId, corId, a_s, a_i, a_i, a_i)
      .as((m0, c0, summaries, x0, x1, x2) -> {
        return new IdAResponseAdminSearchNext(
          m0,
          c0,
          new IdPage<>(
            summaries,
            x0.intValue(),
            x1.intValue(),
            x2.intValue()
          )
        );
      });
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandAdminCreate> commandAdminCreate()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var users =
      Arbitraries.defaultFor(IdAdmin.class);

    return Combinators.combine(msgId, users)
      .as((id, user) -> {
        return new IdACommandAdminCreate(
          id,
          Optional.of(user.id()),
          user.idName(),
          user.realName(),
          user.emails().first(),
          user.password(),
          user.permissions()
            .impliedPermissions()
            .stream()
            .collect(Collectors.toUnmodifiableSet())
        );
      });
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseAdminCreate> responseAdminCreate()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);
    final var users =
      Arbitraries.defaultFor(IdAdmin.class);

    return Combinators.combine(msgId, corId, users).as(IdAResponseAdminCreate::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseAdminGet> responseAdminGet()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);
    final var users =
      Arbitraries.defaultFor(IdAdmin.class);

    return Combinators.combine(msgId, corId, users).as((m0, c0, user) -> {
      return new IdAResponseAdminGet(
        m0,
        c0,
        Optional.of(user)
      );
    });
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandAdminGet> commandAdminGet()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var id =
      Arbitraries.defaultFor(UUID.class);

    return Combinators.combine(msgId, id)
      .as(IdACommandAdminGet::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandAdminGetByEmail> commandAdminGetByEmail()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var id =
      Arbitraries.defaultFor(IdEmail.class);

    return Combinators.combine(msgId, id)
      .as(IdACommandAdminGetByEmail::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandAdminUpdateCredentials> commandAdminUpdate()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var users =
      Arbitraries.defaultFor(IdAdmin.class);

    return Combinators.combine(msgId, users)
      .as((m0, admin) -> {
        return new IdACommandAdminUpdateCredentials(
          m0,
          admin.id(),
          Optional.of(admin.idName()),
          Optional.of(admin.realName()),
          Optional.of(admin.password())
        );
      });
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandAdminPermissionGrant> commandAdminPermissionGrant()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var id =
      Arbitraries.defaultFor(UUID.class);
    final var permissions =
      Arbitraries.defaultFor(IdAdminPermission.class);

    return Combinators.combine(msgId, id, permissions)
      .as(IdACommandAdminPermissionGrant::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandAdminPermissionRevoke> commandAdminPermissionRevoke()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var id =
      Arbitraries.defaultFor(UUID.class);
    final var permissions =
      Arbitraries.defaultFor(IdAdminPermission.class);

    return Combinators.combine(msgId, id, permissions)
      .as(IdACommandAdminPermissionRevoke::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandAdminEmailAdd> commandAdminEmailAdd()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var id =
      Arbitraries.defaultFor(UUID.class);
    final var mails =
      Arbitraries.defaultFor(IdEmail.class);

    return Combinators.combine(msgId, id, mails)
      .as(IdACommandAdminEmailAdd::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandAdminEmailRemove> commandAdminEmailRemove()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var id =
      Arbitraries.defaultFor(UUID.class);
    final var mails =
      Arbitraries.defaultFor(IdEmail.class);

    return Combinators.combine(msgId, id, mails)
      .as(IdACommandAdminEmailRemove::new);
  }


  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandUserEmailAdd> commandUserEmailAdd()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var id =
      Arbitraries.defaultFor(UUID.class);
    final var mails =
      Arbitraries.defaultFor(IdEmail.class);

    return Combinators.combine(msgId, id, mails)
      .as(IdACommandUserEmailAdd::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandUserEmailRemove> commandUserEmailRemove()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var id =
      Arbitraries.defaultFor(UUID.class);
    final var mails =
      Arbitraries.defaultFor(IdEmail.class);

    return Combinators.combine(msgId, id, mails)
      .as(IdACommandUserEmailRemove::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseAdminUpdate> responseAdminUpdate()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);
    final var users =
      Arbitraries.defaultFor(IdAdmin.class);

    return Combinators.combine(msgId, corId, users)
      .as(IdAResponseAdminUpdate::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandUserDelete> commandUserDelete()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var id =
      Arbitraries.defaultFor(UUID.class);

    return Combinators.combine(msgId, id)
      .as(IdACommandUserDelete::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandAdminDelete> commandAdminDelete()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var id =
      Arbitraries.defaultFor(UUID.class);

    return Combinators.combine(msgId, id)
      .as(IdACommandAdminDelete::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseUserDelete> responseUserDelete()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);

    return Combinators.combine(msgId, corId)
      .as(IdAResponseUserDelete::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseAdminDelete> responseAdminDelete()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);

    return Combinators.combine(msgId, corId)
      .as(IdAResponseAdminDelete::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandAdminBanCreate> commandAdminBanCreate()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var id =
      Arbitraries.defaultFor(IdBan.class);

    return Combinators.combine(msgId, id)
      .as(IdACommandAdminBanCreate::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandAdminBanDelete> commandAdminBanDelete()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var id =
      Arbitraries.defaultFor(UUID.class);

    return Combinators.combine(msgId, id)
      .as(IdACommandAdminBanDelete::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandAdminBanGet> commandAdminBanGet()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var id =
      Arbitraries.defaultFor(UUID.class);

    return Combinators.combine(msgId, id)
      .as(IdACommandAdminBanGet::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandUserBanCreate> commandUserBanCreate()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var id =
      Arbitraries.defaultFor(IdBan.class);

    return Combinators.combine(msgId, id)
      .as(IdACommandUserBanCreate::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandUserBanDelete> commandUserBanDelete()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var id =
      Arbitraries.defaultFor(UUID.class);

    return Combinators.combine(msgId, id)
      .as(IdACommandUserBanDelete::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandUserBanGet> commandUserBanGet()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var id =
      Arbitraries.defaultFor(UUID.class);

    return Combinators.combine(msgId, id)
      .as(IdACommandUserBanGet::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseAdminBanCreate> responseAdminBanCreate()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);
    final var b =
      Arbitraries.defaultFor(IdBan.class);

    return Combinators.combine(msgId, corId, b)
      .as(IdAResponseAdminBanCreate::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseAdminBanDelete> responseAdminBanDelete()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);

    return Combinators.combine(msgId, corId)
      .as(IdAResponseAdminBanDelete::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseAdminBanGet> responseAdminBanGet()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);
    final var b =
      Arbitraries.defaultFor(IdBan.class)
        .optional();

    return Combinators.combine(msgId, corId, b)
      .as(IdAResponseAdminBanGet::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseUserBanCreate> responseUserBanCreate()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);
    final var b =
      Arbitraries.defaultFor(IdBan.class);

    return Combinators.combine(msgId, corId, b)
      .as(IdAResponseUserBanCreate::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseUserBanDelete> responseUserBanDelete()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);

    return Combinators.combine(msgId, corId)
      .as(IdAResponseUserBanDelete::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseUserBanGet> responseUserBanGet()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);
    final var b =
      Arbitraries.defaultFor(IdBan.class)
        .optional();

    return Combinators.combine(msgId, corId, b)
      .as(IdAResponseUserBanGet::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdACommandUserLoginHistory> commandUserLoginHistory()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var id =
      Arbitraries.defaultFor(UUID.class);

    return Combinators.combine(msgId, id)
      .as(IdACommandUserLoginHistory::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdAResponseUserLoginHistory> responseUserLoginHistory()
  {
    final var msgId =
      Arbitraries.defaultFor(UUID.class);
    final var corId =
      Arbitraries.defaultFor(UUID.class);
    final var hist =
      Arbitraries.defaultFor(IdLogin.class)
        .list();

    return Combinators.combine(msgId, corId, hist)
      .as(IdAResponseUserLoginHistory::new);
  }
}

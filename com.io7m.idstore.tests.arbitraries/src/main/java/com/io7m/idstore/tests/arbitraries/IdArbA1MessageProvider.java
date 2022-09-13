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
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.protocol.admin_v1.IdA1Admin;
import com.io7m.idstore.protocol.admin_v1.IdA1AdminPermission;
import com.io7m.idstore.protocol.admin_v1.IdA1AdminSearchByEmailParameters;
import com.io7m.idstore.protocol.admin_v1.IdA1AdminSearchParameters;
import com.io7m.idstore.protocol.admin_v1.IdA1AdminSummary;
import com.io7m.idstore.protocol.admin_v1.IdA1AuditEvent;
import com.io7m.idstore.protocol.admin_v1.IdA1AuditListParameters;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminCreate;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminDelete;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminEmailAdd;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminEmailRemove;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminGet;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminGetByEmail;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminPermissionGrant;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminPermissionRevoke;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSearchBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSearchByEmailBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSearchByEmailNext;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSearchNext;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSearchPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSelf;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminUpdate;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAuditSearchBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAuditSearchNext;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAuditSearchPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandLogin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserCreate;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserDelete;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserEmailAdd;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserEmailRemove;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserGet;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserGetByEmail;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserSearchBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserSearchByEmailBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserSearchByEmailNext;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserSearchNext;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserSearchPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserUpdate;
import com.io7m.idstore.protocol.admin_v1.IdA1MessageType;
import com.io7m.idstore.protocol.admin_v1.IdA1Page;
import com.io7m.idstore.protocol.admin_v1.IdA1Password;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminCreate;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminDelete;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminGet;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminSearchBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminSearchByEmailBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminSearchByEmailNext;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminSearchNext;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminSearchPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminSelf;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminUpdate;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAuditSearchBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAuditSearchNext;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAuditSearchPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseError;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseLogin;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserCreate;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserDelete;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserGet;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserSearchBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserSearchByEmailBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserSearchByEmailNext;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserSearchNext;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserSearchPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserUpdate;
import com.io7m.idstore.protocol.admin_v1.IdA1User;
import com.io7m.idstore.protocol.admin_v1.IdA1UserSearchByEmailParameters;
import com.io7m.idstore.protocol.admin_v1.IdA1UserSearchParameters;
import com.io7m.idstore.protocol.admin_v1.IdA1UserSummary;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.providers.TypeUsage;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
      commandAdminCreate(),
      commandAdminDelete(),
      commandAdminEmailAdd(),
      commandAdminEmailRemove(),
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
      commandLogin(),
      commandUserCreate(),
      commandUserDelete(),
      commandUserEmailAdd(),
      commandUserEmailRemove(),
      commandUserGet(),
      commandUserSearchBegin(),
      commandUserSearchByEmailBegin(),
      commandUserSearchByEmailNext(),
      commandUserSearchByEmailPrevious(),
      commandUserSearchNext(),
      commandUserSearchPrevious(),
      commandUserUpdate(),
      responseAdminCreate(),
      responseAdminDelete(),
      responseAdminSearchBegin(),
      responseAdminSearchByEmailBegin(),
      responseAdminSearchByEmailNext(),
      responseAdminSearchByEmailPrevious(),
      responseAdminSearchNext(),
      responseAdminSearchPrevious(),
      responseAdminSelf(),
      responseError(),
      responseLogin(),
      responseUserCreate(),
      responseUserDelete(),
      responseUserGet(),
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

  public static Arbitrary<IdA1ResponseUserSearchBegin> responseUserSearchBegin()
  {
    final var a_id =
      Arbitraries.defaultFor(UUID.class);
    final var a_s =
      Arbitraries.defaultFor(IdA1UserSummary.class)
        .list();
    final var a_i =
      Arbitraries.integers();

    return Combinators.combine(a_id, a_s, a_i, a_i, a_i)
      .as((id, summaries, x0, x1, x2) -> {
        return new IdA1ResponseUserSearchBegin(
          id,
          new IdA1Page<>(
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

  public static Arbitrary<IdA1ResponseUserSearchPrevious> responseUserSearchPrevious()
  {
    final var a_id =
      Arbitraries.defaultFor(UUID.class);
    final var a_s =
      Arbitraries.defaultFor(IdA1UserSummary.class)
        .list();
    final var a_i =
      Arbitraries.integers();

    return Combinators.combine(a_id, a_s, a_i, a_i, a_i)
      .as((id, summaries, x0, x1, x2) -> {
        return new IdA1ResponseUserSearchPrevious(
          id,
          new IdA1Page<>(
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

  public static Arbitrary<IdA1ResponseUserSearchNext> responseUserSearchNext()
  {
    final var a_id =
      Arbitraries.defaultFor(UUID.class);
    final var a_s =
      Arbitraries.defaultFor(IdA1UserSummary.class)
        .list();
    final var a_i =
      Arbitraries.integers();

    return Combinators.combine(a_id, a_s, a_i, a_i, a_i)
      .as((id, summaries, x0, x1, x2) -> {
        return new IdA1ResponseUserSearchNext(
          id,
          new IdA1Page<>(
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

  public static Arbitrary<IdA1ResponseAdminSelf> responseAdminSelf()
  {
    final var id =
      Arbitraries.defaultFor(UUID.class);
    final var a =
      Arbitraries.defaultFor(IdA1Admin.class);

    return Combinators.combine(id, a).as(IdA1ResponseAdminSelf::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1ResponseError> responseError()
  {
    final var id =
      Arbitraries.defaultFor(UUID.class);
    final var s0 =
      Arbitraries.strings();
    final var s1 =
      Arbitraries.strings();

    return Combinators.combine(id, s0, s1).as(IdA1ResponseError::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1ResponseLogin> responseLogin()
  {
    final var id =
      Arbitraries.defaultFor(UUID.class);
    final var t =
      Arbitraries.defaultFor(OffsetDateTime.class);

    return Combinators.combine(id, t).as(IdA1ResponseLogin::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1ResponseUserGet> responseUserGet()
  {
    final var id =
      Arbitraries.defaultFor(UUID.class);
    final var users =
      Arbitraries.defaultFor(IdA1User.class);

    return Combinators.combine(id, users).as((req, user) -> {
      return new IdA1ResponseUserGet(
        req,
        Optional.of(user)
      );
    });
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1ResponseUserCreate> responseUserCreate()
  {
    final var id =
      Arbitraries.defaultFor(UUID.class);
    final var users =
      Arbitraries.defaultFor(IdA1User.class);

    return Combinators.combine(id, users).as(IdA1ResponseUserCreate::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1ResponseUserUpdate> responseUserUpdate()
  {
    final var id =
      Arbitraries.defaultFor(UUID.class);
    final var users =
      Arbitraries.defaultFor(IdA1User.class);

    return Combinators.combine(id, users).as(IdA1ResponseUserUpdate::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1CommandAdminSelf> commandAdminSelf()
  {
    return Arbitraries.integers().map(i -> new IdA1CommandAdminSelf());
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1CommandUserGet> commandUserGet()
  {
    final var id = Arbitraries.defaultFor(UUID.class);
    return id.map(IdA1CommandUserGet::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1CommandUserGetByEmail> commandUserGetByEmail()
  {
    final var id = Arbitraries.defaultFor(IdEmail.class).map(IdEmail::toString);
    return id.map(IdA1CommandUserGetByEmail::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1CommandLogin> commandLogin()
  {
    final var s0 =
      Arbitraries.strings();
    final var s1 =
      Arbitraries.strings();

    return Combinators.combine(s0, s1).as(IdA1CommandLogin::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1CommandUserSearchBegin> commandUserSearchBegin()
  {
    return Arbitraries.defaultFor(IdA1UserSearchParameters.class)
      .map(IdA1CommandUserSearchBegin::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1CommandUserSearchNext> commandUserSearchNext()
  {
    return Arbitraries.of(new IdA1CommandUserSearchNext());
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1CommandUserSearchPrevious> commandUserSearchPrevious()
  {
    return Arbitraries.of(new IdA1CommandUserSearchPrevious());
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1CommandUserSearchByEmailBegin> commandUserSearchByEmailBegin()
  {
    return Arbitraries.defaultFor(IdA1UserSearchByEmailParameters.class)
      .map(IdA1CommandUserSearchByEmailBegin::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1CommandUserSearchByEmailNext> commandUserSearchByEmailNext()
  {
    return Arbitraries.of(new IdA1CommandUserSearchByEmailNext());
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1CommandUserSearchByEmailPrevious> commandUserSearchByEmailPrevious()
  {
    return Arbitraries.of(new IdA1CommandUserSearchByEmailPrevious());
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1CommandUserCreate> commandUserCreate()
  {
    final var users =
      Arbitraries.defaultFor(IdUser.class);

    return users.map(user -> {
      return new IdA1CommandUserCreate(
        Optional.of(user.id()),
        user.idName().value(),
        user.realName().value(),
        user.emails().first().value(),
        new IdA1Password(
          user.password().algorithm().identifier(),
          user.password().hash(),
          user.password().salt()
        )
      );
    });
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1CommandUserUpdate> commandUserUpdate()
  {
    final var users =
      Arbitraries.defaultFor(IdA1User.class);

    return users.map((user) -> {
      return new IdA1CommandUserUpdate(
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

  public static Arbitrary<IdA1CommandAuditSearchBegin> commandAuditSearchBegin()
  {
    return Arbitraries.defaultFor(IdA1AuditListParameters.class)
      .map(IdA1CommandAuditSearchBegin::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1CommandAuditSearchNext> commandAuditSearchNext()
  {
    return Arbitraries.of(new IdA1CommandAuditSearchNext());
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1CommandAuditSearchPrevious> commandAuditSearchPrevious()
  {
    return Arbitraries.of(new IdA1CommandAuditSearchPrevious());
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1ResponseAuditSearchBegin> responseAuditSearchBegin()
  {
    final var a_id =
      Arbitraries.defaultFor(UUID.class);
    final var a_s =
      Arbitraries.defaultFor(IdA1AuditEvent.class)
        .list();
    final var a_i =
      Arbitraries.integers();

    return Combinators.combine(a_id, a_s, a_i, a_i, a_i)
      .as((id, summaries, x0, x1, x2) -> {
        return new IdA1ResponseAuditSearchBegin(
          id,
          new IdA1Page<>(
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

  public static Arbitrary<IdA1ResponseAuditSearchPrevious> responseAuditSearchPrevious()
  {
    final var a_id =
      Arbitraries.defaultFor(UUID.class);
    final var a_s =
      Arbitraries.defaultFor(IdA1AuditEvent.class)
        .list();
    final var a_i =
      Arbitraries.integers();

    return Combinators.combine(a_id, a_s, a_i, a_i, a_i)
      .as((id, summaries, x0, x1, x2) -> {
        return new IdA1ResponseAuditSearchPrevious(
          id,
          new IdA1Page<>(
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

  public static Arbitrary<IdA1ResponseAuditSearchNext> responseAuditSearchNext()
  {
    final var a_id =
      Arbitraries.defaultFor(UUID.class);
    final var a_s =
      Arbitraries.defaultFor(IdA1AuditEvent.class)
        .list();
    final var a_i =
      Arbitraries.integers();

    return Combinators.combine(a_id, a_s, a_i, a_i, a_i)
      .as((id, summaries, x0, x1, x2) -> {
        return new IdA1ResponseAuditSearchNext(
          id,
          new IdA1Page<>(
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

  public static Arbitrary<IdA1ResponseUserSearchByEmailBegin> responseUserSearchByEmailBegin()
  {
    final var a_id =
      Arbitraries.defaultFor(UUID.class);
    final var a_s =
      Arbitraries.defaultFor(IdA1UserSummary.class)
        .list();
    final var a_i =
      Arbitraries.integers();

    return Combinators.combine(a_id, a_s, a_i, a_i, a_i)
      .as((id, summaries, x0, x1, x2) -> {
        return new IdA1ResponseUserSearchByEmailBegin(
          id,
          new IdA1Page<>(
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

  public static Arbitrary<IdA1ResponseUserSearchByEmailPrevious> responseUserSearchByEmailPrevious()
  {
    final var a_id =
      Arbitraries.defaultFor(UUID.class);
    final var a_s =
      Arbitraries.defaultFor(IdA1UserSummary.class)
        .list();
    final var a_i =
      Arbitraries.integers();

    return Combinators.combine(a_id, a_s, a_i, a_i, a_i)
      .as((id, summaries, x0, x1, x2) -> {
        return new IdA1ResponseUserSearchByEmailPrevious(
          id,
          new IdA1Page<>(
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

  public static Arbitrary<IdA1ResponseUserSearchByEmailNext> responseUserSearchByEmailNext()
  {
    final var a_id =
      Arbitraries.defaultFor(UUID.class);
    final var a_s =
      Arbitraries.defaultFor(IdA1UserSummary.class)
        .list();
    final var a_i =
      Arbitraries.integers();

    return Combinators.combine(a_id, a_s, a_i, a_i, a_i)
      .as((id, summaries, x0, x1, x2) -> {
        return new IdA1ResponseUserSearchByEmailNext(
          id,
          new IdA1Page<>(
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

  public static Arbitrary<IdA1CommandAdminSearchBegin> commandAdminSearchBegin()
  {
    return Arbitraries.defaultFor(IdA1AdminSearchParameters.class)
      .map(IdA1CommandAdminSearchBegin::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1CommandAdminSearchNext> commandAdminSearchNext()
  {
    return Arbitraries.of(new IdA1CommandAdminSearchNext());
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1CommandAdminSearchPrevious> commandAdminSearchPrevious()
  {
    return Arbitraries.of(new IdA1CommandAdminSearchPrevious());
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1CommandAdminSearchByEmailBegin> commandAdminSearchByEmailBegin()
  {
    return Arbitraries.defaultFor(IdA1AdminSearchByEmailParameters.class)
      .map(IdA1CommandAdminSearchByEmailBegin::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1CommandAdminSearchByEmailNext> commandAdminSearchByEmailNext()
  {
    return Arbitraries.of(new IdA1CommandAdminSearchByEmailNext());
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1CommandAdminSearchByEmailPrevious> commandAdminSearchByEmailPrevious()
  {
    return Arbitraries.of(new IdA1CommandAdminSearchByEmailPrevious());
  }


  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1ResponseAdminSearchByEmailBegin> responseAdminSearchByEmailBegin()
  {
    final var a_id =
      Arbitraries.defaultFor(UUID.class);
    final var a_s =
      Arbitraries.defaultFor(IdA1AdminSummary.class)
        .list();
    final var a_i =
      Arbitraries.integers();

    return Combinators.combine(a_id, a_s, a_i, a_i, a_i)
      .as((id, summaries, x0, x1, x2) -> {
        return new IdA1ResponseAdminSearchByEmailBegin(
          id,
          new IdA1Page<>(
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

  public static Arbitrary<IdA1ResponseAdminSearchByEmailPrevious> responseAdminSearchByEmailPrevious()
  {
    final var a_id =
      Arbitraries.defaultFor(UUID.class);
    final var a_s =
      Arbitraries.defaultFor(IdA1AdminSummary.class)
        .list();
    final var a_i =
      Arbitraries.integers();

    return Combinators.combine(a_id, a_s, a_i, a_i, a_i)
      .as((id, summaries, x0, x1, x2) -> {
        return new IdA1ResponseAdminSearchByEmailPrevious(
          id,
          new IdA1Page<>(
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

  public static Arbitrary<IdA1ResponseAdminSearchByEmailNext> responseAdminSearchByEmailNext()
  {
    final var a_id =
      Arbitraries.defaultFor(UUID.class);
    final var a_s =
      Arbitraries.defaultFor(IdA1AdminSummary.class)
        .list();
    final var a_i =
      Arbitraries.integers();

    return Combinators.combine(a_id, a_s, a_i, a_i, a_i)
      .as((id, summaries, x0, x1, x2) -> {
        return new IdA1ResponseAdminSearchByEmailNext(
          id,
          new IdA1Page<>(
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

  public static Arbitrary<IdA1ResponseAdminSearchBegin> responseAdminSearchBegin()
  {
    final var a_id =
      Arbitraries.defaultFor(UUID.class);
    final var a_s =
      Arbitraries.defaultFor(IdA1AdminSummary.class)
        .list();
    final var a_i =
      Arbitraries.integers();

    return Combinators.combine(a_id, a_s, a_i, a_i, a_i)
      .as((id, summaries, x0, x1, x2) -> {
        return new IdA1ResponseAdminSearchBegin(
          id,
          new IdA1Page<>(
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

  public static Arbitrary<IdA1ResponseAdminSearchPrevious> responseAdminSearchPrevious()
  {
    final var a_id =
      Arbitraries.defaultFor(UUID.class);
    final var a_s =
      Arbitraries.defaultFor(IdA1AdminSummary.class)
        .list();
    final var a_i =
      Arbitraries.integers();

    return Combinators.combine(a_id, a_s, a_i, a_i, a_i)
      .as((id, summaries, x0, x1, x2) -> {
        return new IdA1ResponseAdminSearchPrevious(
          id,
          new IdA1Page<>(
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

  public static Arbitrary<IdA1ResponseAdminSearchNext> responseAdminSearchNext()
  {
    final var a_id =
      Arbitraries.defaultFor(UUID.class);
    final var a_s =
      Arbitraries.defaultFor(IdA1AdminSummary.class)
        .list();
    final var a_i =
      Arbitraries.integers();

    return Combinators.combine(a_id, a_s, a_i, a_i, a_i)
      .as((id, summaries, x0, x1, x2) -> {
        return new IdA1ResponseAdminSearchNext(
          id,
          new IdA1Page<>(
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

  public static Arbitrary<IdA1CommandAdminCreate> commandAdminCreate()
  {
    final var users =
      Arbitraries.defaultFor(IdAdmin.class);

    return users.map(user -> {
      return new IdA1CommandAdminCreate(
        Optional.of(user.id()),
        user.idName().value(),
        user.realName().value(),
        user.emails().first().value(),
        new IdA1Password(
          user.password().algorithm().identifier(),
          user.password().hash(),
          user.password().salt()
        ),
        user.permissions()
          .impliedPermissions()
          .stream()
          .map(IdA1AdminPermission::ofPermission)
          .collect(Collectors.toUnmodifiableSet())
      );
    });
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1ResponseAdminCreate> responseAdminCreate()
  {
    final var id =
      Arbitraries.defaultFor(UUID.class);
    final var users =
      Arbitraries.defaultFor(IdA1Admin.class);

    return Combinators.combine(id, users).as(IdA1ResponseAdminCreate::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1ResponseAdminGet> responseAdminGet()
  {
    final var id =
      Arbitraries.defaultFor(UUID.class);
    final var users =
      Arbitraries.defaultFor(IdA1Admin.class);

    return Combinators.combine(id, users).as((req, user) -> {
      return new IdA1ResponseAdminGet(
        req,
        Optional.of(user)
      );
    });
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1CommandAdminGet> commandAdminGet()
  {
    final var id = Arbitraries.defaultFor(UUID.class);
    return id.map(IdA1CommandAdminGet::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1CommandAdminGetByEmail> commandAdminGetByEmail()
  {
    final var id = Arbitraries.defaultFor(IdEmail.class).map(IdEmail::toString);
    return id.map(IdA1CommandAdminGetByEmail::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1CommandAdminUpdate> commandAdminUpdate()
  {
    final var users =
      Arbitraries.defaultFor(IdA1Admin.class);

    return users.map((admin) -> {
      return new IdA1CommandAdminUpdate(
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

  public static Arbitrary<IdA1CommandAdminPermissionGrant> commandAdminPermissionGrant()
  {
    final var id =
      Arbitraries.defaultFor(UUID.class);
    final var permissions =
      Arbitraries.defaultFor(IdA1AdminPermission.class);

    return Combinators.combine(id, permissions)
      .as(IdA1CommandAdminPermissionGrant::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1CommandAdminPermissionRevoke> commandAdminPermissionRevoke()
  {
    final var id =
      Arbitraries.defaultFor(UUID.class);
    final var permissions =
      Arbitraries.defaultFor(IdA1AdminPermission.class);

    return Combinators.combine(id, permissions)
      .as(IdA1CommandAdminPermissionRevoke::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1CommandAdminEmailAdd> commandAdminEmailAdd()
  {
    final var id =
      Arbitraries.defaultFor(UUID.class);
    final var mails =
      Arbitraries.defaultFor(IdEmail.class);

    return Combinators.combine(id, mails)
      .as((uuid, idEmail) -> {
        return new IdA1CommandAdminEmailAdd(uuid, idEmail.value());
      });
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1CommandAdminEmailRemove> commandAdminEmailRemove()
  {
    final var id =
      Arbitraries.defaultFor(UUID.class);
    final var mails =
      Arbitraries.defaultFor(IdEmail.class);

    return Combinators.combine(id, mails)
      .as((uuid, idEmail) -> {
        return new IdA1CommandAdminEmailRemove(uuid, idEmail.value());
      });
  }


  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1CommandUserEmailAdd> commandUserEmailAdd()
  {
    final var id =
      Arbitraries.defaultFor(UUID.class);
    final var mails =
      Arbitraries.defaultFor(IdEmail.class);

    return Combinators.combine(id, mails)
      .as((uuid, idEmail) -> {
        return new IdA1CommandUserEmailAdd(uuid, idEmail.value());
      });
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1CommandUserEmailRemove> commandUserEmailRemove()
  {
    final var id =
      Arbitraries.defaultFor(UUID.class);
    final var mails =
      Arbitraries.defaultFor(IdEmail.class);

    return Combinators.combine(id, mails)
      .as((uuid, idEmail) -> {
        return new IdA1CommandUserEmailRemove(uuid, idEmail.value());
      });
  }


  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1ResponseAdminUpdate> responseAdminUpdate()
  {
    final var id =
      Arbitraries.defaultFor(UUID.class);
    final var users =
      Arbitraries.defaultFor(IdA1Admin.class);

    return Combinators.combine(id, users).as(IdA1ResponseAdminUpdate::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1CommandUserDelete> commandUserDelete()
  {
    final var id = Arbitraries.defaultFor(UUID.class);
    return id.map(IdA1CommandUserDelete::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1CommandAdminDelete> commandAdminDelete()
  {
    final var id = Arbitraries.defaultFor(UUID.class);
    return id.map(IdA1CommandAdminDelete::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1ResponseUserDelete> responseUserDelete()
  {
    final var id = Arbitraries.defaultFor(UUID.class);
    return id.map(IdA1ResponseUserDelete::new);
  }

  /**
   * @return A message arbitrary
   */

  public static Arbitrary<IdA1ResponseAdminDelete> responseAdminDelete()
  {
    final var id = Arbitraries.defaultFor(UUID.class);
    return id.map(IdA1ResponseAdminDelete::new);
  }
}

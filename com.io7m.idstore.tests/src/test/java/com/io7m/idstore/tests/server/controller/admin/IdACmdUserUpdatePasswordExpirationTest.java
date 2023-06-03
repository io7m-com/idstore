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

package com.io7m.idstore.tests.server.controller.admin;

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.model.IdAdminPermissionSet;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.protocol.admin.IdACommandUserUpdatePasswordExpiration;
import com.io7m.idstore.protocol.admin.IdAPasswordExpirationSetNever;
import com.io7m.idstore.protocol.admin.IdAPasswordExpirationSetRefresh;
import com.io7m.idstore.protocol.admin.IdAPasswordExpirationSetSpecific;
import com.io7m.idstore.protocol.admin.IdAResponseUserUpdate;
import com.io7m.idstore.server.controller.admin.IdACmdUserUpdatePasswordExpiration;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SECURITY_POLICY_DENIED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SQL_ERROR;
import static com.io7m.idstore.model.IdAdminPermission.USER_WRITE_CREDENTIALS;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public final class IdACmdUserUpdatePasswordExpirationTest
  extends IdACmdAbstractContract
{
  /**
   * Editing users requires the USER_WRITE_CREDENTIALS permission.
   *
   * @throws Exception On errors
   */

  @Test
  public void testNotAllowed0()
    throws Exception
  {
    /* Arrange. */

    final var admin =
      this.createAdmin("admin", IdAdminPermissionSet.empty());
    final var context =
      this.createContextAndSession(admin);

    /* Act. */

    final var handler = new IdACmdUserUpdatePasswordExpiration();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(context, new IdACommandUserUpdatePasswordExpiration(
          randomUUID(),
          new IdAPasswordExpirationSetNever()
        ));
      });

    /* Assert. */

    assertEquals(SECURITY_POLICY_DENIED, ex.errorCode());
  }

  /**
   * Database errors are fatal.
   *
   * @throws Exception On errors
   */

  @Test
  public void testDatabaseError()
    throws Exception
  {
    /* Arrange. */

    final var admin0 =
      this.createAdmin(
        "admin0",
        IdAdminPermissionSet.of(USER_WRITE_CREDENTIALS));
    final var admin1 =
      this.createUser("user1");

    final var context =
      this.createContextAndSession(admin0);

    final var users =
      mock(IdDatabaseUsersQueriesType.class);

    when(users.userGetRequire(admin1.id()))
      .thenReturn(admin1);

    Mockito.doThrow(new IdDatabaseException("", SQL_ERROR, Map.of(), empty()))
      .when(users)
      .userUpdateAsAdmin(any(), any(), any(), any());

    final var transaction = this.transaction();
    when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);

    /* Act. */

    final var handler = new IdACmdUserUpdatePasswordExpiration();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(
          context,
          new IdACommandUserUpdatePasswordExpiration(
            admin1.id(),
            new IdAPasswordExpirationSetNever()
          ));
      });

    /* Assert. */

    assertEquals(SQL_ERROR, ex.errorCode());

    verify(transaction)
      .queries(IdDatabaseUsersQueriesType.class);
    verify(transaction)
      .adminIdSet(admin0.id());
    verify(users, this.once())
      .userGetRequire(any());
    verify(users, this.once())
      .userUpdateAsAdmin(
        any(),
        any(),
        any(),
        any()
      );

    verifyNoMoreInteractions(users);
    verifyNoMoreInteractions(transaction);
  }

  /**
   * Users can be updated.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUpdateSetNever()
    throws Exception
  {
    /* Arrange. */

    final var admin0 =
      this.createAdmin(
        "admin0",
        IdAdminPermissionSet.of(USER_WRITE_CREDENTIALS)
      );

    final var user =
      this.createUser("user");

    final var userWithoutExpiring =
      new IdUser(
        user.id(),
        user.idName(),
        user.realName(),
        user.emails(),
        user.timeCreated(),
        user.timeUpdated(),
        user.password().withoutExpirationDate()
      );

    final var context =
      this.createContextAndSession(admin0);

    final var users =
      mock(IdDatabaseUsersQueriesType.class);

    when(users.userGetRequire(user.id()))
      .thenReturn(user, userWithoutExpiring);

    final var transaction = this.transaction();
    when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);

    /* Act. */

    final var handler = new IdACmdUserUpdatePasswordExpiration();
    final var response =
      handler.execute(
        context,
        new IdACommandUserUpdatePasswordExpiration(
          user.id(),
          new IdAPasswordExpirationSetNever()
        )
      );

    /* Assert. */

    /*
     * The returned user has a redacted password without an expiration date.
     */

    assertEquals(
      new IdAResponseUserUpdate(
        context.requestId(),
        userWithoutExpiring
      ),
      response
    );

    verify(transaction)
      .queries(IdDatabaseUsersQueriesType.class);
    verify(transaction)
      .adminIdSet(admin0.id());
    verify(users, this.twice())
      .userGetRequire(user.id());
    verify(users, this.once())
      .userUpdateAsAdmin(
        eq(user.id()),
        eq(empty()),
        eq(empty()),
        argThat(argument -> {
          return Objects.equals(
            argument.get().expires(),
            Optional.empty()
          );
        })
      );

    verifyNoMoreInteractions(users);
    verifyNoMoreInteractions(transaction);
  }

  /**
   * Users can be updated.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUpdateSetRefresh()
    throws Exception
  {
    /* Arrange. */

    final var admin0 =
      this.createAdmin(
        "admin0",
        IdAdminPermissionSet.of(USER_WRITE_CREDENTIALS)
      );

    final var user =
      this.createUser("user");

    final var context =
      this.createContextAndSession(admin0);

    final var users =
      mock(IdDatabaseUsersQueriesType.class);

    when(users.userGetRequire(user.id()))
      .thenReturn(user);

    final var transaction = this.transaction();
    when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);

    /* Act. */

    final var handler = new IdACmdUserUpdatePasswordExpiration();
    final var response =
      handler.execute(
        context,
        new IdACommandUserUpdatePasswordExpiration(
          user.id(),
          new IdAPasswordExpirationSetRefresh()
        )
      );

    /* Assert. */

    /*
     * The returned user has a redacted password with an expiration date.
     */

    assertEquals(
      new IdAResponseUserUpdate(
        context.requestId(),
        user
      ),
      response
    );

    verify(transaction)
      .queries(IdDatabaseUsersQueriesType.class);
    verify(transaction)
      .adminIdSet(admin0.id());
    verify(users, this.twice())
      .userGetRequire(user.id());
    verify(users, this.once())
      .userUpdateAsAdmin(
        eq(user.id()),
        eq(empty()),
        eq(empty()),
        argThat(argument -> {
          final Optional<OffsetDateTime> received = argument.get().expires();
          final Optional<OffsetDateTime> expected = user.password().expires();
          return Objects.equals(received, expected);
        })
      );

    verifyNoMoreInteractions(users);
    verifyNoMoreInteractions(transaction);
  }

  /**
   * Users can be updated.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUpdateSetSpecific()
    throws Exception
  {
    /* Arrange. */

    final var admin0 =
      this.createAdmin(
        "admin0",
        IdAdminPermissionSet.of(USER_WRITE_CREDENTIALS)
      );

    final var expiration =
      OffsetDateTime.parse("1980-01-01T00:30:02Z");

    final var user =
      this.createUser("user");

    final var userWithExpiring =
      new IdUser(
        user.id(),
        user.idName(),
        user.realName(),
        user.emails(),
        user.timeCreated(),
        user.timeUpdated(),
        user.password().withExpirationDate(expiration)
      );

    final var context =
      this.createContextAndSession(admin0);

    final var users =
      mock(IdDatabaseUsersQueriesType.class);

    when(users.userGetRequire(user.id()))
      .thenReturn(user, userWithExpiring);

    final var transaction = this.transaction();
    when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);

    /* Act. */

    final var handler = new IdACmdUserUpdatePasswordExpiration();
    final var response =
      handler.execute(
        context,
        new IdACommandUserUpdatePasswordExpiration(
          user.id(),
          new IdAPasswordExpirationSetSpecific(expiration)
        )
      );

    /* Assert. */

    /*
     * The returned user has a redacted password with an expiration date.
     */

    assertEquals(
      new IdAResponseUserUpdate(
        context.requestId(),
        userWithExpiring
      ),
      response
    );

    verify(transaction)
      .queries(IdDatabaseUsersQueriesType.class);
    verify(transaction)
      .adminIdSet(admin0.id());
    verify(users, this.twice())
      .userGetRequire(user.id());
    verify(users, this.once())
      .userUpdateAsAdmin(
        eq(user.id()),
        eq(empty()),
        eq(empty()),
        argThat(argument -> {
          return Objects.equals(
            argument.get().expires(),
            Optional.of(expiration)
          );
        })
      );

    verifyNoMoreInteractions(users);
    verifyNoMoreInteractions(transaction);
  }
}

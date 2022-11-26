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

package com.io7m.idstore.tests.server.controller.admin;

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.model.IdAdminPermissionSet;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.protocol.admin.IdACommandUserUpdate;
import com.io7m.idstore.protocol.admin.IdAResponseUserUpdate;
import com.io7m.idstore.server.controller.admin.IdACmdUserUpdate;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SECURITY_POLICY_DENIED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SQL_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SQL_ERROR_UNIQUE;
import static com.io7m.idstore.model.IdAdminPermission.USER_WRITE;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public final class IdACmdUserUpdateTest
  extends IdACmdAbstractContract
{
  /**
   * Editing users requires the USER_WRITE permission.
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

    final var handler = new IdACmdUserUpdate();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(context, new IdACommandUserUpdate(
          randomUUID(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty()
        ));
      });

    /* Assert. */

    assertEquals(SECURITY_POLICY_DENIED, ex.errorCode());
  }

  /**
   * Editing yourself requires the USER_WRITE_SELF permission.
   *
   * @throws Exception On errors
   */

  @Test
  public void testNotAllowed1()
    throws Exception
  {
    /* Arrange. */

    final var admin =
      this.createAdmin("admin", IdAdminPermissionSet.empty());
    final var context =
      this.createContextAndSession(admin);

    /* Act. */

    final var handler = new IdACmdUserUpdate();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(context, new IdACommandUserUpdate(
          admin.id(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty()
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
      this.createAdmin("admin0", IdAdminPermissionSet.of(USER_WRITE));
    final var user0 =
      this.createUser("user0");

    final var context =
      this.createContextAndSession(admin0);

    final var users =
      mock(IdDatabaseUsersQueriesType.class);

    when(users.userGetRequire(user0.id()))
      .thenReturn(user0);

    Mockito.doThrow(new IdDatabaseException("", SQL_ERROR))
      .when(users)
      .userUpdateAsAdmin(any(), any(), any(), any());

    final var transaction = this.transaction();
    when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);

    /* Act. */

    final var handler = new IdACmdUserUpdate();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(
          context,
          new IdACommandUserUpdate(
            user0.id(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()));
      });

    /* Assert. */

    assertEquals(SQL_ERROR, ex.errorCode());

    verify(transaction)
      .queries(IdDatabaseUsersQueriesType.class);
    verify(transaction)
      .adminIdSet(admin0.id());
    verify(users, this.once())
      .userGetRequire(user0.id());
    verify(users, this.once())
      .userUpdateAsAdmin(
        user0.id(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
      );

    verifyNoMoreInteractions(users);
    verifyNoMoreInteractions(transaction);
  }

  /**
   * Admins can be updated.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUpdate()
    throws Exception
  {
    /* Arrange. */

    final var admin0 =
      this.createAdmin("admin0", IdAdminPermissionSet.of(USER_WRITE));
    final var user0 =
      this.createUser("user0");

    final var context =
      this.createContextAndSession(admin0);

    final var users =
      mock(IdDatabaseUsersQueriesType.class);

    final var password2 =
      IdPasswordAlgorithmPBKDF2HmacSHA256.create()
        .createHashed("y");

    final var expectedUser =
      new IdUser(
        user0.id(),
        new IdName("user1"),
        new IdRealName("Real Name"),
        user0.emails(),
        user0.timeCreated(),
        user0.timeUpdated(),
        password2
      );

    when(users.userGetRequire(user0.id()))
      .thenReturn(expectedUser);

    final var transaction = this.transaction();
    when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);

    /* Act. */

    final var handler = new IdACmdUserUpdate();
    final var response =
      handler.execute(
        context,
        new IdACommandUserUpdate(
          user0.id(),
          Optional.of(new IdName("user1")),
          Optional.of(new IdRealName("Real Name")),
          Optional.of(password2)
        )
      );

    /* Assert. */

    assertEquals(
      new IdAResponseUserUpdate(context.requestId(), expectedUser),
      response
    );

    verify(transaction)
      .queries(IdDatabaseUsersQueriesType.class);
    verify(transaction)
      .adminIdSet(admin0.id());
    verify(users, this.once())
      .userUpdateAsAdmin(
        user0.id(),
        Optional.of(new IdName("user1")),
        Optional.of(new IdRealName("Real Name")),
        Optional.of(password2)
      );
    verify(users, this.twice())
      .userGetRequire(user0.id());

    verifyNoMoreInteractions(users);
    verifyNoMoreInteractions(transaction);
  }

  /**
   * Names must be unique.
   *
   * @throws Exception On errors
   */

  @Test
  public void testNameUnique()
    throws Exception
  {
    /* Arrange. */

    final var admin0 =
      this.createAdmin("admin0", IdAdminPermissionSet.of(USER_WRITE));
    final var user0 =
      this.createUser("user0");

    final var context =
      this.createContextAndSession(admin0);

    final var users =
      mock(IdDatabaseUsersQueriesType.class);

    Mockito.doThrow(new IdDatabaseException("", SQL_ERROR_UNIQUE))
      .when(users)
      .userUpdateAsAdmin(any(), any(), any(), any());

    when(users.userGetRequire(user0.id()))
      .thenReturn(user0);

    final var transaction = this.transaction();
    when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);

    /* Act. */

    final var handler = new IdACmdUserUpdate();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(
          context,
          new IdACommandUserUpdate(
            user0.id(),
            Optional.of(admin0.idName()),
            Optional.empty(),
            Optional.empty()));
      });

    /* Assert. */

    assertEquals(SQL_ERROR_UNIQUE, ex.errorCode());

    verify(transaction)
      .queries(IdDatabaseUsersQueriesType.class);
    verify(transaction)
      .adminIdSet(admin0.id());
    verify(users, this.once())
      .userUpdateAsAdmin(
        user0.id(),
        Optional.of(admin0.idName()),
        Optional.empty(),
        Optional.empty()
      );
    verify(users, this.once())
      .userGetRequire(user0.id());

    verifyNoMoreInteractions(users);
    verifyNoMoreInteractions(transaction);
  }
}

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
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdNonEmptyList;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.protocol.admin.IdACommandUserEmailAdd;
import com.io7m.idstore.protocol.admin.IdAResponseUserUpdate;
import com.io7m.idstore.server.controller.admin.IdACmdUserEmailAdd;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SECURITY_POLICY_DENIED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SQL_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SQL_ERROR_UNIQUE;
import static com.io7m.idstore.model.IdAdminPermission.USER_WRITE;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public final class IdACmdUserEmailAddTest
  extends IdACmdAbstractContract
{
  /**
   * Editing admins requires the USER_WRITE permission.
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

    final var handler = new IdACmdUserEmailAdd();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(context, new IdACommandUserEmailAdd(
          randomUUID(),
          new IdEmail("someone-else@example.com")
        ));
      });

    /* Assert. */

    assertEquals(SECURITY_POLICY_DENIED, ex.errorCode());
  }
  
  /**
   * Email addresses must be unique.
   *
   * @throws Exception On errors
   */

  @Test
  public void testEmailUnique()
    throws Exception
  {
    /* Arrange. */

    final var admin0 =
      this.createAdmin("admin0", IdAdminPermissionSet.of(USER_WRITE));
    final var context =
      this.createContextAndSession(admin0);

    final var admins =
      mock(IdDatabaseUsersQueriesType.class);

    Mockito.doThrow(new IdDatabaseException("", SQL_ERROR_UNIQUE, Map.of(), empty()))
      .when(admins)
      .userEmailAdd(any(), any());

    final var transaction = this.transaction();
    when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(admins);

    final var userId =
      randomUUID();
    final var email =
      new IdEmail("someone-else@example.com");

    /* Act. */

    final var handler = new IdACmdUserEmailAdd();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(context, new IdACommandUserEmailAdd(userId, email));
      });

    /* Assert. */

    assertEquals(SQL_ERROR_UNIQUE, ex.errorCode());

    verify(transaction).queries(IdDatabaseUsersQueriesType.class);
    verify(transaction).adminIdSet(admin0.id());
    verify(admins, this.once()).userEmailAdd(userId, email);
    verifyNoMoreInteractions(admins);
    verifyNoMoreInteractions(transaction);
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

    final var context =
      this.createContextAndSession(admin0);

    final var admins =
      mock(IdDatabaseUsersQueriesType.class);

    Mockito.doThrow(new IdDatabaseException("", SQL_ERROR, Map.of(), empty()))
      .when(admins)
      .userEmailAdd(any(), any());

    final var transaction = this.transaction();
    when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(admins);

    final var userId =
      randomUUID();
    final var email =
      new IdEmail("someone-else@example.com");

    /* Act. */

    final var handler = new IdACmdUserEmailAdd();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(context, new IdACommandUserEmailAdd(userId, email));
      });

    /* Assert. */

    assertEquals(SQL_ERROR, ex.errorCode());

    verify(transaction).queries(IdDatabaseUsersQueriesType.class);
    verify(transaction).adminIdSet(admin0.id());
    verify(admins, this.once()).userEmailAdd(userId, email);
    verifyNoMoreInteractions(admins);
    verifyNoMoreInteractions(transaction);
  }

  /**
   * Email addresses can be added.
   *
   * @throws Exception On errors
   */

  @Test
  public void testEmailUpdate()
    throws Exception
  {
    /* Arrange. */

    final var admin0 =
      this.createAdmin("admin0", IdAdminPermissionSet.of(USER_WRITE));
    final var user0 =
      this.createUser("user0");

    final var context =
      this.createContextAndSession(admin0);

    final var admins =
      mock(IdDatabaseUsersQueriesType.class);

    final var email =
      new IdEmail("someone-else@example.com");

    final var expectedUser =
      new IdUser(
        user0.id(),
        user0.idName(),
        user0.realName(),
        IdNonEmptyList.ofList(List.of(
          user0.emails().first(),
          email
        )),
        user0.timeCreated(),
        user0.timeUpdated(),
        user0.password()
      );

    when(admins.userGetRequire(user0.id()))
      .thenReturn(expectedUser);

    final var transaction = this.transaction();
    when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(admins);

    /* Act. */

    final var handler = new IdACmdUserEmailAdd();
    final var response =
      handler.execute(context, new IdACommandUserEmailAdd(user0.id(), email));

    /* Assert. */

    assertEquals(
      new IdAResponseUserUpdate(context.requestId(), expectedUser),
      response
    );

    verify(transaction).queries(IdDatabaseUsersQueriesType.class);
    verify(transaction).adminIdSet(admin0.id());
    verify(admins, this.once()).userEmailAdd(user0.id(), email);
    verify(admins, this.once()).userGetRequire(user0.id());
    verifyNoMoreInteractions(admins);
    verifyNoMoreInteractions(transaction);
  }
}

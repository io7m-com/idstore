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
import com.io7m.idstore.protocol.admin.IdACommandUserDelete;
import com.io7m.idstore.protocol.admin.IdAResponseUserDelete;
import com.io7m.idstore.server.controller.admin.IdACmdUserDelete;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SECURITY_POLICY_DENIED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.USER_NONEXISTENT;
import static com.io7m.idstore.model.IdAdminPermission.USER_DELETE;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public final class IdACmdUserDeleteTest
  extends IdACmdAbstractContract
{
  /**
   * Creating users requires the USER_DELETE permission.
   *
   * @throws Exception On errors
   */

  @Test
  public void testNotAllowed()
    throws Exception
  {
    /* Arrange. */

    final var admin =
      this.createAdmin("admin", IdAdminPermissionSet.empty());
    final var context =
      this.createContextAndSession(admin);

    /* Act. */

    final var handler = new IdACmdUserDelete();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(context, new IdACommandUserDelete(randomUUID()));
      });

    /* Assert. */

    assertEquals(SECURITY_POLICY_DENIED, ex.errorCode());
  }

  /**
   * Deleting users requires the user to exist.
   *
   * @throws Exception On errors
   */

  @Test
  public void testMustExist()
    throws Exception
  {
    /* Arrange. */

    final var admin0 =
      this.createAdmin("admin0", IdAdminPermissionSet.of(USER_DELETE));
    final var context =
      this.createContextAndSession(admin0);

    final var users =
      mock(IdDatabaseUsersQueriesType.class);

    final var adminId = randomUUID();
    doThrow(new IdDatabaseException("", USER_NONEXISTENT, Map.of(), empty()))
      .when(users)
      .userDelete(adminId);

    final var transaction = this.transaction();
    when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);

    /* Act. */

    final var handler = new IdACmdUserDelete();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(context, new IdACommandUserDelete(adminId));
      });

    /* Assert. */

    assertEquals(USER_NONEXISTENT, ex.errorCode());

    verify(transaction).queries(IdDatabaseUsersQueriesType.class);
    verify(transaction).adminIdSet(admin0.id());
    verify(users, this.once()).userDelete(adminId);
    verifyNoMoreInteractions(users);
    verifyNoMoreInteractions(transaction);
  }

  /**
   * Deleting users works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCreateOK()
    throws Exception
  {
    /* Arrange. */

    final var admin0 =
      this.createAdmin("admin0", IdAdminPermissionSet.of(USER_DELETE));
    final var context =
      this.createContextAndSession(admin0);

    final var users =
      mock(IdDatabaseUsersQueriesType.class);

    final var transaction = this.transaction();
    when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);

    /* Act. */

    final var handler =
      new IdACmdUserDelete();
    final var adminId =
      randomUUID();
    final var response =
      handler.execute(context, new IdACommandUserDelete(adminId));

    /* Assert. */

    assertEquals(
      new IdAResponseUserDelete(context.requestId()),
      response
    );

    verify(transaction).queries(IdDatabaseUsersQueriesType.class);
    verify(transaction).adminIdSet(admin0.id());
    verify(users, this.once()).userDelete(adminId);
    verifyNoMoreInteractions(users);
    verifyNoMoreInteractions(transaction);
  }
}

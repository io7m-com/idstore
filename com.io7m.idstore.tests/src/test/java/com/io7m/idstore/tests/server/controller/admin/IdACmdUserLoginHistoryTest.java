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
import com.io7m.idstore.model.IdLogin;
import com.io7m.idstore.protocol.admin.IdACommandUserLoginHistory;
import com.io7m.idstore.protocol.admin.IdAResponseUserLoginHistory;
import com.io7m.idstore.server.controller.admin.IdACmdUserLoginHistory;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SECURITY_POLICY_DENIED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.USER_NONEXISTENT;
import static com.io7m.idstore.model.IdAdminPermission.USER_READ;
import static java.time.OffsetDateTime.now;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public final class IdACmdUserLoginHistoryTest
  extends IdACmdAbstractContract
{
  /**
   * Retrieving users requires the USER_READ permission.
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

    final var handler = new IdACmdUserLoginHistory();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(context, new IdACommandUserLoginHistory(randomUUID()));
      });

    /* Assert. */

    assertEquals(SECURITY_POLICY_DENIED, ex.errorCode());
  }

  /**
   * Retrieving users works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGetOK()
    throws Exception
  {
    /* Arrange. */

    final var admin0 =
      this.createAdmin("admin0", IdAdminPermissionSet.of(USER_READ));
    final var context =
      this.createContextAndSession(admin0);

    final var userId =
      randomUUID();

    final var users =
      mock(IdDatabaseUsersQueriesType.class);

    final var history = List.of(
      new IdLogin(userId, now(), "127.0.0.1", "NCSA 0.1"),
      new IdLogin(userId, now(), "127.0.0.2", "NCSA 0.2"),
      new IdLogin(userId, now(), "127.0.0.3", "NCSA 0.3"),
      new IdLogin(userId, now(), "127.0.0.4", "NCSA 0.4"),
      new IdLogin(userId, now(), "127.0.0.5", "NCSA 0.5")
    );

    when(users.userLoginHistory(eq(userId), anyInt()))
      .thenReturn(history);

    final var transaction = this.transaction();
    when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);

    /* Act. */

    final var handler =
      new IdACmdUserLoginHistory();
    final var response =
      handler.execute(context, new IdACommandUserLoginHistory(userId));

    /* Assert. */

    assertEquals(
      new IdAResponseUserLoginHistory(context.requestId(), history),
      response
    );

    verify(transaction)
      .queries(IdDatabaseUsersQueriesType.class);
    verify(users, this.once())
      .userLoginHistory(eq(userId), anyInt());

    verifyNoMoreInteractions(users);
    verifyNoMoreInteractions(transaction);
  }

  /**
   * Retrieving users works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGetNonexistent()
    throws Exception
  {
    /* Arrange. */

    final var admin0 =
      this.createAdmin("admin0", IdAdminPermissionSet.of(USER_READ));
    final var context =
      this.createContextAndSession(admin0);

    final var userId =
      randomUUID();

    final var users =
      mock(IdDatabaseUsersQueriesType.class);

    when(users.userLoginHistory(eq(userId), anyInt()))
      .thenThrow(new IdDatabaseException("", USER_NONEXISTENT, Map.of(), empty()));

    final var transaction = this.transaction();
    when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);

    /* Act. */

    final var handler = new IdACmdUserLoginHistory();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(context, new IdACommandUserLoginHistory(userId));
      });

    /* Assert. */

    assertEquals(USER_NONEXISTENT, ex.errorCode());

    verify(transaction)
      .queries(IdDatabaseUsersQueriesType.class);
    verify(users, this.once())
      .userLoginHistory(eq(userId), anyInt());

    verifyNoMoreInteractions(users);
    verifyNoMoreInteractions(transaction);
  }
}

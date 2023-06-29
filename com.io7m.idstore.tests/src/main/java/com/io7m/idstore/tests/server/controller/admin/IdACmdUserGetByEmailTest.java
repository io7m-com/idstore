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

import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.model.IdAdminPermissionSet;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdNonEmptyList;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.protocol.admin.IdACommandUserGetByEmail;
import com.io7m.idstore.protocol.admin.IdAResponseUserGet;
import com.io7m.idstore.server.controller.admin.IdACmdUserGetByEmail;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SECURITY_POLICY_DENIED;
import static com.io7m.idstore.model.IdAdminPermission.USER_READ;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public final class IdACmdUserGetByEmailTest
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

    final var handler = new IdACmdUserGetByEmail();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(
          context,
          new IdACommandUserGetByEmail(new IdEmail("someone-else@example.com"))
        );
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
    final var password =
      this.password();
    final var email =
      new IdEmail("someone-else@example.com");
    final var expectedUser =
      new IdUser(
        userId,
        new IdName("ad"),
        new IdRealName("Someone"),
        IdNonEmptyList.single(email),
        this.timeStart(),
        this.timeStart().plusSeconds(1L),
        password
      );

    final var users =
      mock(IdDatabaseUsersQueriesType.class);

    when(users.userGetForEmail(email))
      .thenReturn(Optional.of(expectedUser));

    final var transaction = this.transaction();
    when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);

    /* Act. */

    final var handler =
      new IdACmdUserGetByEmail();
    final var response =
      handler.execute(context, new IdACommandUserGetByEmail(email));

    /* Assert. */

    assertEquals(
      new IdAResponseUserGet(
        context.requestId(),
        Optional.of(expectedUser)),
      response
    );

    verify(transaction).queries(IdDatabaseUsersQueriesType.class);
    verify(users, this.once()).userGetForEmail(email);
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

    final var email =
      new IdEmail("someone-else@example.com");
    final var users =
      mock(IdDatabaseUsersQueriesType.class);

    when(users.userGetForEmail(email))
      .thenReturn(empty());

    final var transaction = this.transaction();
    when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);

    /* Act. */

    final var handler =
      new IdACmdUserGetByEmail();
    final var response =
      handler.execute(context, new IdACommandUserGetByEmail(email));

    /* Assert. */

    assertEquals(
      new IdAResponseUserGet(context.requestId(), empty()),
      response
    );

    verify(transaction).queries(IdDatabaseUsersQueriesType.class);
    verify(users, this.once()).userGetForEmail(email);
    verifyNoMoreInteractions(users);
    verifyNoMoreInteractions(transaction);
  }
}

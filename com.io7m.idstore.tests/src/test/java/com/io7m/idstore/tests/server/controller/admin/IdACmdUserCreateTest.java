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
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdNonEmptyList;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.protocol.admin.IdACommandUserCreate;
import com.io7m.idstore.protocol.admin.IdAResponseUserCreate;
import com.io7m.idstore.server.controller.admin.IdACmdUserCreate;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SECURITY_POLICY_DENIED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.USER_DUPLICATE;
import static com.io7m.idstore.model.IdAdminPermission.USER_CREATE;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public final class IdACmdUserCreateTest
  extends IdACmdAbstractContract
{
  /**
   * Creating users requires the USER_CREATE permission.
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

    final var handler = new IdACmdUserCreate();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(context, new IdACommandUserCreate(
          empty(),
          new IdName("ad"),
          new IdRealName("Someone"),
          new IdEmail("someone-else@example.com"),
          this.password()
        ));
      });

    /* Assert. */

    assertEquals(SECURITY_POLICY_DENIED, ex.errorCode());
  }

  /**
   * Creating users requires the user to not exist.
   *
   * @throws Exception On errors
   */

  @Test
  public void testMustNotExist()
    throws Exception
  {
    /* Arrange. */

    final var admin0 =
      this.createAdmin("admin0", IdAdminPermissionSet.of(USER_CREATE));
    final var context =
      this.createContextAndSession(admin0);

    final var users =
      Mockito.mock(IdDatabaseUsersQueriesType.class);

    Mockito.doThrow(new IdDatabaseException("", USER_DUPLICATE))
      .when(users)
      .userCreate(
        any(),
        any(),
        any(),
        any(),
        any(),
        any()
      );

    final var transaction = this.transaction();
    Mockito.when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);

    /* Act. */

    final var password = this.password();
    final var handler = new IdACmdUserCreate();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(context, new IdACommandUserCreate(
          empty(),
          new IdName("ad"),
          new IdRealName("Someone"),
          new IdEmail("someone-else@example.com"),
          password
        ));
      });

    /* Assert. */

    assertEquals(USER_DUPLICATE, ex.errorCode());

    verify(transaction).queries(IdDatabaseUsersQueriesType.class);
    verify(transaction).adminIdSet(admin0.id());
    verify(users, this.once())
      .userCreate(
        any(),
        eq(new IdName("ad")),
        eq(new IdRealName("Someone")),
        eq(new IdEmail("someone-else@example.com")),
        any(),
        eq(password)
      );
    verifyNoMoreInteractions(users);
    verifyNoMoreInteractions(transaction);
  }

  /**
   * Creating users works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCreateOK()
    throws Exception
  {
    /* Arrange. */

    final var admin0 =
      this.createAdmin("admin0", IdAdminPermissionSet.of(USER_CREATE));
    final var context =
      this.createContextAndSession(admin0);

    final var userId =
      randomUUID();
    final var password =
      this.password();
    final var expectedUser =
      new IdUser(
        userId,
        new IdName("ad"),
        new IdRealName("Someone"),
        IdNonEmptyList.single(new IdEmail("someone-else@example.com")),
        this.timeStart(),
        this.timeStart().plusSeconds(1L),
        password
      );

    final var users =
      Mockito.mock(IdDatabaseUsersQueriesType.class);

    Mockito.when(users.userCreate(
        any(),
        any(),
        any(),
        any(),
        any(),
        any()))
      .thenReturn(expectedUser);

    final var transaction = this.transaction();
    Mockito.when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);

    /* Act. */

    final var handler =
      new IdACmdUserCreate();
    final var response =
      handler.execute(context, new IdACommandUserCreate(
        Optional.of(userId),
        new IdName("ad"),
        new IdRealName("Someone"),
        new IdEmail("someone-else@example.com"),
        password
      ));

    /* Assert. */

    assertEquals(
      new IdAResponseUserCreate(context.requestId(), expectedUser),
      response
    );

    verify(transaction).queries(IdDatabaseUsersQueriesType.class);
    verify(transaction).adminIdSet(admin0.id());
    verify(users, this.once())
      .userCreate(
        any(),
        eq(new IdName("ad")),
        eq(new IdRealName("Someone")),
        eq(new IdEmail("someone-else@example.com")),
        any(),
        eq(password)
      );
    verifyNoMoreInteractions(users);
    verifyNoMoreInteractions(transaction);
  }
}

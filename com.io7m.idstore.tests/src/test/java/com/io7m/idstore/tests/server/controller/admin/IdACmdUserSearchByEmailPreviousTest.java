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

import com.io7m.idstore.database.api.IdDatabaseUserSearchByEmailType;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.model.IdAdminPermissionSet;
import com.io7m.idstore.model.IdPage;
import com.io7m.idstore.model.IdUserSummary;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchByEmailPrevious;
import com.io7m.idstore.server.controller.admin.IdACmdUserSearchByEmailPrevious;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SECURITY_POLICY_DENIED;
import static com.io7m.idstore.model.IdAdminPermission.USER_READ;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public final class IdACmdUserSearchByEmailPreviousTest
  extends IdACmdAbstractContract
{
  /**
   * Searching requires USER_READ.
   *
   * @throws Exception On errors
   */

  @Test
  public void testNotAllowed0()
    throws Exception
  {
    /* Arrange. */

    final var admin0 =
      this.createAdmin("admin0", IdAdminPermissionSet.empty());
    final var context =
      this.createContextAndSession(admin0);

    /* Act. */

    final var handler = new IdACmdUserSearchByEmailPrevious();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(
          context,
          new IdACommandUserSearchByEmailPrevious()
        );
      });

    /* Assert. */

    assertEquals(SECURITY_POLICY_DENIED, ex.errorCode());
  }

  /**
   * Searching works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testSearchingWorks()
    throws Exception
  {
    /* Arrange. */

    final var admin0 =
      this.createAdmin("admin0", IdAdminPermissionSet.of(USER_READ));
    final var context =
      this.createContextAndSession(admin0);

    final var users =
      mock(IdDatabaseUsersQueriesType.class);
    final var search =
      mock(IdDatabaseUserSearchByEmailType.class);

    final var page =
      new IdPage<IdUserSummary>(List.of(), 2, 3, 1L);

    when(search.pagePrevious(any()))
      .thenReturn(page);

    final var transaction = this.transaction();
    when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);

    context.session()
      .setUserSearchByEmail(search);

    /* Act. */

    final var handler =
      new IdACmdUserSearchByEmailPrevious();
    final var response =
      handler.execute(
        context,
        new IdACommandUserSearchByEmailPrevious()
      );

    /* Assert. */

    assertEquals(
      Optional.of(search),
      context.session().userSearchByEmail()
    );
    assertEquals(
      response,
      new IdAResponseUserSearchByEmailPrevious(context.requestId(), page)
    );

    verify(search)
      .pagePrevious(users);
    verify(transaction)
      .queries(IdDatabaseUsersQueriesType.class);

    verifyNoMoreInteractions(search);
    verifyNoMoreInteractions(users);
    verifyNoMoreInteractions(transaction);
  }

  /**
   * It's not possible to request the previous page when the first page hasn't been requested.
   *
   * @throws Exception On errors
   */

  @Test
  public void testSearchingRequiredStart()
    throws Exception
  {
    /* Arrange. */

    final var admin0 =
      this.createAdmin("admin0", IdAdminPermissionSet.of(USER_READ));
    final var context =
      this.createContextAndSession(admin0);

    final var users =
      mock(IdDatabaseUsersQueriesType.class);

    final var transaction = this.transaction();
    when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);

    /* Act. */

    final var handler =
      new IdACmdUserSearchByEmailPrevious();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(
          context,
          new IdACommandUserSearchByEmailPrevious()
        );
      });

    /* Assert. */

    assertEquals(PROTOCOL_ERROR, ex.errorCode());
  }
}

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

import com.io7m.idstore.database.api.IdDatabaseAdminSearchByEmailType;
import com.io7m.idstore.database.api.IdDatabaseAdminsQueriesType;
import com.io7m.idstore.model.IdAdminColumnOrdering;
import com.io7m.idstore.model.IdAdminPermissionSet;
import com.io7m.idstore.model.IdAdminSearchByEmailParameters;
import com.io7m.idstore.model.IdAdminSummary;
import com.io7m.idstore.model.IdPage;
import com.io7m.idstore.model.IdTimeRange;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchByEmailBegin;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSearchByEmailBegin;
import com.io7m.idstore.server.controller.admin.IdACmdAdminSearchByEmailBegin;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SECURITY_POLICY_DENIED;
import static com.io7m.idstore.model.IdAdminColumn.BY_IDNAME;
import static com.io7m.idstore.model.IdAdminPermission.ADMIN_READ;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public final class IdACmdAdminSearchByEmailBeginTest
  extends IdACmdAbstractContract
{
  /**
   * Searching requires ADMIN_READ.
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

    final var handler = new IdACmdAdminSearchByEmailBegin();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(
          context,
          new IdACommandAdminSearchByEmailBegin(
            new IdAdminSearchByEmailParameters(
              IdTimeRange.largest(),
              IdTimeRange.largest(),
              "x",
              new IdAdminColumnOrdering(BY_IDNAME, true),
              100
            )
          )
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
      this.createAdmin("admin0", IdAdminPermissionSet.of(ADMIN_READ));
    final var context =
      this.createContextAndSession(admin0);

    final var admins =
      mock(IdDatabaseAdminsQueriesType.class);
    final var search =
      mock(IdDatabaseAdminSearchByEmailType.class);

    final var page =
      new IdPage<IdAdminSummary>(List.of(), 1, 1, 0L);

    when(search.pageCurrent(any()))
      .thenReturn(page);
    when(admins.adminSearchByEmail(any()))
      .thenReturn(search);

    final var transaction = this.transaction();
    when(transaction.queries(IdDatabaseAdminsQueriesType.class))
      .thenReturn(admins);

    final var parameters =
      new IdAdminSearchByEmailParameters(
        IdTimeRange.largest(),
        IdTimeRange.largest(),
        "x",
        new IdAdminColumnOrdering(BY_IDNAME, true),
        100
      );

    /* Act. */

    final var handler =
      new IdACmdAdminSearchByEmailBegin();
    final var response =
      handler.execute(
        context,
        new IdACommandAdminSearchByEmailBegin(
          parameters
        )
      );

    /* Assert. */

    assertEquals(
      Optional.of(search),
      context.session().adminSearchByEmail()
    );
    assertEquals(
      response,
      new IdAResponseAdminSearchByEmailBegin(context.requestId(), page)
    );

    verify(admins)
      .adminSearchByEmail(parameters);
    verify(search)
      .pageCurrent(admins);
    verify(transaction)
      .queries(IdDatabaseAdminsQueriesType.class);

    verifyNoMoreInteractions(search);
    verifyNoMoreInteractions(admins);
    verifyNoMoreInteractions(transaction);
  }

  /**
   * Searching is limited to 1000 results.
   *
   * @throws Exception On errors
   */

  @Test
  public void testSearchingLimit1000()
    throws Exception
  {
    /* Arrange. */

    final var admin0 =
      this.createAdmin("admin0", IdAdminPermissionSet.of(ADMIN_READ));
    final var context =
      this.createContextAndSession(admin0);

    final var admins =
      mock(IdDatabaseAdminsQueriesType.class);
    final var search =
      mock(IdDatabaseAdminSearchByEmailType.class);

    final var page =
      new IdPage<IdAdminSummary>(List.of(), 1, 1, 0L);

    when(search.pageCurrent(any()))
      .thenReturn(page);
    when(admins.adminSearchByEmail(any()))
      .thenReturn(search);

    final var transaction = this.transaction();
    when(transaction.queries(IdDatabaseAdminsQueriesType.class))
      .thenReturn(admins);

    final var parameters =
      new IdAdminSearchByEmailParameters(
        IdTimeRange.largest(),
        IdTimeRange.largest(),
        "x",
        new IdAdminColumnOrdering(BY_IDNAME, true),
        2000
      );

    final var parametersLimited =
      new IdAdminSearchByEmailParameters(
        IdTimeRange.largest(),
        IdTimeRange.largest(),
        "x",
        new IdAdminColumnOrdering(BY_IDNAME, true),
        1000
      );

    /* Act. */

    final var handler = new IdACmdAdminSearchByEmailBegin();
    final var response =
      handler.execute(
        context,
        new IdACommandAdminSearchByEmailBegin(parameters)
      );

    /* Assert. */

    assertEquals(
      Optional.of(search),
      context.session().adminSearchByEmail()
    );
    assertEquals(
      response,
      new IdAResponseAdminSearchByEmailBegin(context.requestId(), page)
    );

    verify(admins)
      .adminSearchByEmail(parametersLimited);
    verify(search)
      .pageCurrent(admins);
    verify(transaction)
      .queries(IdDatabaseAdminsQueriesType.class);

    verifyNoMoreInteractions(search);
    verifyNoMoreInteractions(admins);
    verifyNoMoreInteractions(transaction);
  }
}

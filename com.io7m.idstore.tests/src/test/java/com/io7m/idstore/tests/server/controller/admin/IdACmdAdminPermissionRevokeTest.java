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

import com.io7m.idstore.database.api.IdDatabaseAdminsQueriesType;
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdAdminPermissionSet;
import com.io7m.idstore.protocol.admin.IdACommandAdminPermissionRevoke;
import com.io7m.idstore.server.controller.admin.IdACmdAdminPermissionRevoke;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SECURITY_POLICY_DENIED;
import static com.io7m.idstore.model.IdAdminPermission.ADMIN_WRITE;
import static com.io7m.idstore.model.IdAdminPermission.USER_READ;
import static com.io7m.idstore.model.IdAdminPermission.USER_WRITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public final class IdACmdAdminPermissionRevokeTest
  extends IdACmdAbstractContract
{
  /**
   * It's not possible to revoke a permission that you don't have.
   *
   * @throws Exception On errors
   */

  @Test
  public void testNotAllowed0()
    throws Exception
  {
    /* Arrange. */

    final var admin0 =
      this.createAdmin("admin0", IdAdminPermissionSet.of(ADMIN_WRITE));
    final var admin1 =
      this.createAdmin("admin1", IdAdminPermissionSet.of(USER_READ));

    final var context =
      this.createContextAndSession(admin0);

    /* Act. */

    final var handler = new IdACmdAdminPermissionRevoke();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(
          context,
          new IdACommandAdminPermissionRevoke(admin1.id(), USER_READ)
        );
      });

    /* Assert. */

    assertEquals(SECURITY_POLICY_DENIED, ex.errorCode());
  }

  /**
   * Editing admin permissions requires ADMIN_WRITE.
   *
   * @throws Exception On errors
   */

  @Test
  public void testNotAllowed1()
    throws Exception
  {
    /* Arrange. */

    final var admin0 =
      this.createAdmin("admin0", IdAdminPermissionSet.of(USER_READ));
    final var admin1 =
      this.createAdmin("admin1", IdAdminPermissionSet.empty());

    final var context =
      this.createContextAndSession(admin0);

    /* Act. */

    final var handler = new IdACmdAdminPermissionRevoke();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(
          context,
          new IdACommandAdminPermissionRevoke(admin1.id(), USER_READ)
        );
      });

    /* Assert. */

    assertEquals(SECURITY_POLICY_DENIED, ex.errorCode());
  }

  /**
   * A permission you own can be revoked.
   *
   * @throws Exception On errors
   */

  @Test
  public void testRevoke()
    throws Exception
  {
    /* Arrange. */

    final var admin0 =
      this.createAdmin("admin0", IdAdminPermissionSet.of(ADMIN_WRITE, USER_WRITE));
    final var admin1 =
      this.createAdmin("admin1", IdAdminPermissionSet.of(USER_WRITE));

    final var context =
      this.createContextAndSession(admin0);

    final var admins =
      mock(IdDatabaseAdminsQueriesType.class);

    final var adminAfter =
      new IdAdmin(
        admin1.id(),
        admin1.idName(),
        admin1.realName(),
        admin1.emails(),
        admin1.timeCreated(),
        admin1.timeUpdated(),
        admin1.password(),
        IdAdminPermissionSet.empty()
      ).withRedactedPassword();

    when(admins.adminGetRequire(admin1.id()))
      .thenReturn(admin1);

    when(admins.adminGetRequire(admin1.id()))
      .thenReturn(adminAfter);

    final var transaction = this.transaction();
    when(transaction.queries(IdDatabaseAdminsQueriesType.class))
      .thenReturn(admins);

    /* Act. */

    final var handler = new IdACmdAdminPermissionRevoke();
    handler.execute(
      context,
      new IdACommandAdminPermissionRevoke(admin1.id(), USER_WRITE)
    );

    /* Assert. */

    verify(transaction)
      .queries(IdDatabaseAdminsQueriesType.class);
    verify(transaction)
      .adminIdSet(admin0.id());
    verify(admins, this.twice())
      .adminGetRequire(admin1.id());
    verify(admins, this.once())
      .adminUpdate(
        admin1.id(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.of(Set.of())
      );

    verifyNoMoreInteractions(admins);
    verifyNoMoreInteractions(transaction);
  }
}

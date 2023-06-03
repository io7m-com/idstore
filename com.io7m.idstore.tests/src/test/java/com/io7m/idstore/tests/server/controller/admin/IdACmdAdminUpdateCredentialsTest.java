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

import com.io7m.idstore.database.api.IdDatabaseAdminsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdAdminPermissionSet;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.protocol.admin.IdACommandAdminUpdateCredentials;
import com.io7m.idstore.protocol.admin.IdAResponseAdminUpdate;
import com.io7m.idstore.server.controller.admin.IdACmdAdminUpdateCredentials;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SECURITY_POLICY_DENIED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SQL_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SQL_ERROR_UNIQUE;
import static com.io7m.idstore.model.IdAdminPermission.ADMIN_WRITE_CREDENTIALS;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public final class IdACmdAdminUpdateCredentialsTest
  extends IdACmdAbstractContract
{
  /**
   * Editing admins requires the ADMIN_WRITE permission.
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

    final var handler = new IdACmdAdminUpdateCredentials();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(context, new IdACommandAdminUpdateCredentials(
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
   * Editing yourself requires the ADMIN_WRITE_SELF permission.
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

    final var handler = new IdACmdAdminUpdateCredentials();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(context, new IdACommandAdminUpdateCredentials(
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
      this.createAdmin("admin0", IdAdminPermissionSet.of(ADMIN_WRITE_CREDENTIALS));
    final var admin1 =
      this.createAdmin("admin1", IdAdminPermissionSet.empty());

    final var context =
      this.createContextAndSession(admin0);

    final var admins =
      mock(IdDatabaseAdminsQueriesType.class);

    when(admins.adminGetRequire(admin1.id()))
      .thenReturn(admin1);

    Mockito.doThrow(new IdDatabaseException("", SQL_ERROR, Map.of(), empty()))
      .when(admins)
      .adminUpdate(any(), any(), any(), any(), any());

    final var transaction = this.transaction();
    when(transaction.queries(IdDatabaseAdminsQueriesType.class))
      .thenReturn(admins);

    /* Act. */

    final var handler = new IdACmdAdminUpdateCredentials();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(
          context,
          new IdACommandAdminUpdateCredentials(
            admin1.id(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()));
      });

    /* Assert. */

    assertEquals(SQL_ERROR, ex.errorCode());

    verify(transaction)
      .queries(IdDatabaseAdminsQueriesType.class);
    verify(transaction)
      .adminIdSet(admin0.id());
    verify(admins, this.once())
      .adminGetRequire(admin1.id());
    verify(admins, this.once())
      .adminUpdate(
        admin1.id(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
      );

    verifyNoMoreInteractions(admins);
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
      this.createAdmin("admin0", IdAdminPermissionSet.of(ADMIN_WRITE_CREDENTIALS));
    final var admin1 =
      this.createAdmin("admin1", IdAdminPermissionSet.empty());

    final var context =
      this.createContextAndSession(admin0);

    final var admins =
      mock(IdDatabaseAdminsQueriesType.class);

    final var password2 =
      IdPasswordAlgorithmPBKDF2HmacSHA256.create()
        .createHashed("y")
        .withExpirationDate(OffsetDateTime.parse("1970-01-01T00:30:02Z"));

    final var expectedAdmin =
      new IdAdmin(
        admin1.id(),
        new IdName("admin2"),
        new IdRealName("Real Name"),
        admin1.emails(),
        admin1.timeCreated(),
        admin1.timeUpdated(),
        password2,
        admin1.permissions()
      ).withRedactedPassword();

    when(admins.adminGetRequire(admin1.id()))
      .thenReturn(expectedAdmin);

    final var transaction = this.transaction();
    when(transaction.queries(IdDatabaseAdminsQueriesType.class))
      .thenReturn(admins);

    /* Act. */

    final var handler = new IdACmdAdminUpdateCredentials();
    final var response =
      handler.execute(
        context,
        new IdACommandAdminUpdateCredentials(
          admin1.id(),
          Optional.of(new IdName("admin2")),
          Optional.of(new IdRealName("Real Name")),
          Optional.of(password2)
        )
      );

    /* Assert. */

    assertEquals(
      new IdAResponseAdminUpdate(context.requestId(), expectedAdmin),
      response
    );

    verify(transaction)
      .queries(IdDatabaseAdminsQueriesType.class);
    verify(transaction)
      .adminIdSet(admin0.id());
    verify(admins, this.once())
      .adminUpdate(
        admin1.id(),
        Optional.of(new IdName("admin2")),
        Optional.of(new IdRealName("Real Name")),
        Optional.of(password2),
        Optional.empty()
      );
    verify(admins, this.twice())
      .adminGetRequire(admin1.id());

    verifyNoMoreInteractions(admins);
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
      this.createAdmin("admin0", IdAdminPermissionSet.of(ADMIN_WRITE_CREDENTIALS));
    final var admin1 =
      this.createAdmin("admin1", IdAdminPermissionSet.empty());

    final var context =
      this.createContextAndSession(admin0);

    final var admins =
      mock(IdDatabaseAdminsQueriesType.class);

    Mockito.doThrow(new IdDatabaseException("", SQL_ERROR_UNIQUE, Map.of(), empty()))
      .when(admins)
      .adminUpdate(any(), any(), any(), any(), any());

    when(admins.adminGetRequire(admin1.id()))
      .thenReturn(admin1);

    final var transaction = this.transaction();
    when(transaction.queries(IdDatabaseAdminsQueriesType.class))
      .thenReturn(admins);

    /* Act. */

    final var handler = new IdACmdAdminUpdateCredentials();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(
          context,
          new IdACommandAdminUpdateCredentials(
            admin1.id(),
            Optional.of(admin0.idName()),
            Optional.empty(),
            Optional.empty()));
      });

    /* Assert. */

    assertEquals(SQL_ERROR_UNIQUE, ex.errorCode());

    verify(transaction)
      .queries(IdDatabaseAdminsQueriesType.class);
    verify(transaction)
      .adminIdSet(admin0.id());
    verify(admins, this.once())
      .adminUpdate(
        admin1.id(),
        Optional.of(admin0.idName()),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
      );
    verify(admins, this.once())
      .adminGetRequire(admin1.id());

    verifyNoMoreInteractions(admins);
    verifyNoMoreInteractions(transaction);
  }
}

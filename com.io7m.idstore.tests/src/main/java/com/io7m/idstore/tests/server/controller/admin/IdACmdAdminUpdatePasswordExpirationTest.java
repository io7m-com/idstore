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
import com.io7m.idstore.model.IdPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.idstore.model.IdPasswordAlgorithmRedacted;
import com.io7m.idstore.protocol.admin.IdACommandAdminUpdatePasswordExpiration;
import com.io7m.idstore.protocol.admin.IdAPasswordExpirationSetNever;
import com.io7m.idstore.protocol.admin.IdAPasswordExpirationSetRefresh;
import com.io7m.idstore.protocol.admin.IdAPasswordExpirationSetSpecific;
import com.io7m.idstore.protocol.admin.IdAResponseAdminUpdate;
import com.io7m.idstore.server.controller.admin.IdACmdAdminUpdatePasswordExpiration;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SECURITY_POLICY_DENIED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SQL_ERROR;
import static com.io7m.idstore.model.IdAdminPermission.ADMIN_WRITE_CREDENTIALS;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public final class IdACmdAdminUpdatePasswordExpirationTest
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

    final var handler = new IdACmdAdminUpdatePasswordExpiration();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(context, new IdACommandAdminUpdatePasswordExpiration(
          randomUUID(),
          new IdAPasswordExpirationSetNever()
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

    final var handler = new IdACmdAdminUpdatePasswordExpiration();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(context, new IdACommandAdminUpdatePasswordExpiration(
          admin.id(),
          new IdAPasswordExpirationSetNever()
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
      this.createAdmin(
        "admin0",
        IdAdminPermissionSet.of(ADMIN_WRITE_CREDENTIALS));
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

    final var handler = new IdACmdAdminUpdatePasswordExpiration();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(
          context,
          new IdACommandAdminUpdatePasswordExpiration(
            admin1.id(),
            new IdAPasswordExpirationSetNever()
          ));
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
        eq(admin1.id()),
        any(),
        any(),
        any(),
        any()
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
  public void testUpdateSetNever()
    throws Exception
  {
    /* Arrange. */

    final var admin0 =
      this.createAdmin(
        "admin0",
        IdAdminPermissionSet.of(ADMIN_WRITE_CREDENTIALS)
      );

    final var password =
      IdPasswordAlgorithmPBKDF2HmacSHA256.create()
        .createHashed("y")
        .withExpirationDate(OffsetDateTime.parse("1970-01-01T00:30:02Z"));

    final var passwordWithoutExpiration =
      password.withoutExpirationDate();

    final var passwordRedacted =
      IdPasswordAlgorithmRedacted.create()
        .createHashed("")
        .withoutExpirationDate();

    var adminInput =
      this.createAdmin("admin1", IdAdminPermissionSet.empty());

    adminInput =
      new IdAdmin(
        adminInput.id(),
        adminInput.idName(),
        adminInput.realName(),
        adminInput.emails(),
        adminInput.timeCreated(),
        adminInput.timeUpdated(),
        password,
        adminInput.permissions()
      );

    final var adminExpected =
      new IdAdmin(
        adminInput.id(),
        adminInput.idName(),
        adminInput.realName(),
        adminInput.emails(),
        adminInput.timeCreated(),
        adminInput.timeUpdated(),
        passwordRedacted,
        adminInput.permissions()
      );

    final var context =
      this.createContextAndSession(admin0);

    final var admins =
      mock(IdDatabaseAdminsQueriesType.class);

    when(admins.adminGetRequire(adminInput.id()))
      .thenReturn(adminInput, adminExpected);

    final var transaction = this.transaction();
    when(transaction.queries(IdDatabaseAdminsQueriesType.class))
      .thenReturn(admins);

    /* Act. */

    final var handler = new IdACmdAdminUpdatePasswordExpiration();
    final var response =
      handler.execute(
        context,
        new IdACommandAdminUpdatePasswordExpiration(
          adminInput.id(),
          new IdAPasswordExpirationSetNever()
        )
      );

    /* Assert. */

    /*
     * The returned admin has a redacted password without an expiration date.
     */

    assertEquals(
      new IdAResponseAdminUpdate(
        context.requestId(),
        adminExpected
      ),
      response
    );

    verify(transaction)
      .queries(IdDatabaseAdminsQueriesType.class);
    verify(transaction)
      .adminIdSet(admin0.id());
    verify(admins, atLeast(1))
      .adminGetRequire(adminInput.id());
    verify(admins, this.once())
      .adminUpdate(
        eq(adminInput.id()),
        eq(empty()),
        eq(empty()),
        eq(Optional.of(passwordWithoutExpiration)),
        eq(empty())
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
  public void testUpdateSetRefresh()
    throws Exception
  {
    /* Arrange. */

    final var admin0 =
      this.createAdmin(
        "admin0",
        IdAdminPermissionSet.of(ADMIN_WRITE_CREDENTIALS)
      );

    final var password =
      IdPasswordAlgorithmPBKDF2HmacSHA256.create()
        .createHashed("y")
        .withoutExpirationDate();

    final var passwordWithExpiration =
      password.withExpirationDate(
        OffsetDateTime.parse("1970-01-01T00:30:02Z"));

    final var passwordRedacted =
      IdPasswordAlgorithmRedacted.create()
        .createHashed("")
        .withExpirationDate(passwordWithExpiration.expires());

    var adminInput =
      this.createAdmin("admin1", IdAdminPermissionSet.empty());

    adminInput =
      new IdAdmin(
        adminInput.id(),
        adminInput.idName(),
        adminInput.realName(),
        adminInput.emails(),
        adminInput.timeCreated(),
        adminInput.timeUpdated(),
        password,
        adminInput.permissions()
      );

    final var adminExpected =
      new IdAdmin(
        adminInput.id(),
        adminInput.idName(),
        adminInput.realName(),
        adminInput.emails(),
        adminInput.timeCreated(),
        adminInput.timeUpdated(),
        passwordRedacted,
        adminInput.permissions()
      );

    final var context =
      this.createContextAndSession(admin0);

    final var admins =
      mock(IdDatabaseAdminsQueriesType.class);

    when(admins.adminGetRequire(adminInput.id()))
      .thenReturn(adminInput, adminExpected);

    final var transaction = this.transaction();
    when(transaction.queries(IdDatabaseAdminsQueriesType.class))
      .thenReturn(admins);

    /* Act. */

    final var handler = new IdACmdAdminUpdatePasswordExpiration();
    final var response =
      handler.execute(
        context,
        new IdACommandAdminUpdatePasswordExpiration(
          adminInput.id(),
          new IdAPasswordExpirationSetRefresh()
        )
      );

    /* Assert. */

    /*
     * The returned admin has a redacted password with an expiration date.
     */

    assertEquals(
      new IdAResponseAdminUpdate(
        context.requestId(),
        adminExpected
      ),
      response
    );

    verify(transaction)
      .queries(IdDatabaseAdminsQueriesType.class);
    verify(transaction)
      .adminIdSet(admin0.id());
    verify(admins, atLeast(1))
      .adminGetRequire(adminInput.id());
    verify(admins, this.once())
      .adminUpdate(
        eq(adminInput.id()),
        eq(empty()),
        eq(empty()),
        eq(Optional.of(passwordWithExpiration)),
        eq(empty())
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
  public void testUpdateSetSpecific()
    throws Exception
  {
    /* Arrange. */

    final var admin0 =
      this.createAdmin(
        "admin0",
        IdAdminPermissionSet.of(ADMIN_WRITE_CREDENTIALS)
      );

    final var password =
      IdPasswordAlgorithmPBKDF2HmacSHA256.create()
        .createHashed("y")
        .withoutExpirationDate();

    final var expiration =
      OffsetDateTime.parse("1980-01-01T00:30:02Z");

    final var passwordWithExpiration =
      password.withExpirationDate(expiration);

    final var passwordRedacted =
      IdPasswordAlgorithmRedacted.create()
        .createHashed("")
        .withExpirationDate(passwordWithExpiration.expires());

    var adminInput =
      this.createAdmin("admin1", IdAdminPermissionSet.empty());

    adminInput =
      new IdAdmin(
        adminInput.id(),
        adminInput.idName(),
        adminInput.realName(),
        adminInput.emails(),
        adminInput.timeCreated(),
        adminInput.timeUpdated(),
        password,
        adminInput.permissions()
      );

    final var adminExpected =
      new IdAdmin(
        adminInput.id(),
        adminInput.idName(),
        adminInput.realName(),
        adminInput.emails(),
        adminInput.timeCreated(),
        adminInput.timeUpdated(),
        passwordRedacted,
        adminInput.permissions()
      );

    final var context =
      this.createContextAndSession(admin0);

    final var admins =
      mock(IdDatabaseAdminsQueriesType.class);

    when(admins.adminGetRequire(adminInput.id()))
      .thenReturn(adminInput, adminExpected);

    final var transaction = this.transaction();
    when(transaction.queries(IdDatabaseAdminsQueriesType.class))
      .thenReturn(admins);

    /* Act. */

    final var handler = new IdACmdAdminUpdatePasswordExpiration();
    final var response =
      handler.execute(
        context,
        new IdACommandAdminUpdatePasswordExpiration(
          adminInput.id(),
          new IdAPasswordExpirationSetSpecific(expiration)
        )
      );

    /* Assert. */

    /*
     * The returned admin has a redacted password with an expiration date.
     */

    assertEquals(
      new IdAResponseAdminUpdate(
        context.requestId(),
        adminExpected
      ),
      response
    );

    verify(transaction)
      .queries(IdDatabaseAdminsQueriesType.class);
    verify(transaction)
      .adminIdSet(admin0.id());
    verify(admins, atLeast(1))
      .adminGetRequire(adminInput.id());
    verify(admins, this.once())
      .adminUpdate(
        eq(adminInput.id()),
        eq(empty()),
        eq(empty()),
        eq(Optional.of(passwordWithExpiration)),
        eq(empty())
      );

    verifyNoMoreInteractions(admins);
    verifyNoMoreInteractions(transaction);
  }
}

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

package com.io7m.idstore.tests.server.controller.user;

import com.io7m.idstore.database.api.IdDatabaseEmailsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.model.IdEmailVerification;
import com.io7m.idstore.model.IdToken;
import com.io7m.idstore.protocol.user.IdUCommandEmailRemovePermit;
import com.io7m.idstore.protocol.user.IdUResponseEmailRemovePermit;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.controller.user.IdUCmdEmailRemovePermit;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.EMAIL_VERIFICATION_FAILED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.EMAIL_VERIFICATION_NONEXISTENT;
import static com.io7m.idstore.model.IdEmailVerificationOperation.EMAIL_ADD;
import static com.io7m.idstore.model.IdEmailVerificationOperation.EMAIL_REMOVE;
import static com.io7m.idstore.model.IdEmailVerificationResolution.PERMITTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public final class IdUCmdEmailRemovePermitTest
  extends IdUCmdAbstractContract
{
  /**
   * Nonexistent tokens fail.
   *
   * @throws Exception On errors
   */

  @Test
  public void testTokenNonexistent0()
    throws Exception
  {
    /* Arrange. */

    final var user0 =
      this.createUserAndSessionWithEmails("user0");
    final var context =
      this.createContextAndSession(user0);

    final var emails =
      mock(IdDatabaseEmailsQueriesType.class);
    final var transaction =
      this.transaction();
    final var token =
      IdToken.generate();

    when(transaction.queries(IdDatabaseEmailsQueriesType.class))
      .thenReturn(emails);
    when(emails.emailVerificationGetPermit(any()))
      .thenReturn(Optional.empty());

    /* Act. */

    final var handler = new IdUCmdEmailRemovePermit();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(context, new IdUCommandEmailRemovePermit(token));
      });

    /* Assert. */

    assertEquals(EMAIL_VERIFICATION_NONEXISTENT, ex.errorCode());
  }

  /**
   * Tokens for the wrong user fail.
   *
   * @throws Exception On errors
   */

  @Test
  public void testTokenWrongUser()
    throws Exception
  {
    /* Arrange. */

    final var user0 =
      this.createUserAndSessionWithEmails("user0");
    final var context =
      this.createContextAndSession(user0);

    final var emails =
      mock(IdDatabaseEmailsQueriesType.class);
    final var transaction =
      this.transaction();
    final var token0 =
      IdToken.generate();
    final var token1 =
      IdToken.generate();

    final var verification =
      new IdEmailVerification(
        UUID.randomUUID(),
        user0.emails().first(),
        token0,
        token1,
        EMAIL_REMOVE,
        this.timeStart().plusHours(6L)
      );

    when(transaction.queries(IdDatabaseEmailsQueriesType.class))
      .thenReturn(emails);
    when(emails.emailVerificationGetPermit(any()))
      .thenReturn(Optional.of(verification));

    /* Act. */

    final var handler = new IdUCmdEmailRemovePermit();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(context, new IdUCommandEmailRemovePermit(token0));
      });

    /* Assert. */

    assertEquals(EMAIL_VERIFICATION_FAILED, ex.errorCode());
  }

  /**
   * Expired tokens fail.
   *
   * @throws Exception On errors
   */

  @Test
  public void testTokenExpired()
    throws Exception
  {
    /* Arrange. */

    final var user0 =
      this.createUserAndSessionWithEmails("user0");
    final var context =
      this.createContextAndSession(user0);

    final var emails =
      mock(IdDatabaseEmailsQueriesType.class);
    final var transaction =
      this.transaction();
    final var token0 =
      IdToken.generate();
    final var token1 =
      IdToken.generate();

    final var verification =
      new IdEmailVerification(
        user0.id(),
        user0.emails().first(),
        token0,
        token1,
        EMAIL_REMOVE,
        this.timeStart().minusHours(6L)
      );

    when(transaction.queries(IdDatabaseEmailsQueriesType.class))
      .thenReturn(emails);
    when(emails.emailVerificationGetPermit(any()))
      .thenReturn(Optional.of(verification));

    /* Act. */

    final var handler = new IdUCmdEmailRemovePermit();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(context, new IdUCommandEmailRemovePermit(token0));
      });

    /* Assert. */

    assertEquals(EMAIL_VERIFICATION_FAILED, ex.errorCode());
  }

  /**
   * Tokens using the wrong operation fail.
   *
   * @throws Exception On errors
   */

  @Test
  public void testTokenWrongOperation()
    throws Exception
  {
    /* Arrange. */

    final var user0 =
      this.createUserAndSessionWithEmails("user0");
    final var context =
      this.createContextAndSession(user0);

    final var emails =
      mock(IdDatabaseEmailsQueriesType.class);
    final var transaction =
      this.transaction();
    final var token0 =
      IdToken.generate();
    final var token1 =
      IdToken.generate();

    final var verification =
      new IdEmailVerification(
        user0.id(),
        user0.emails().first(),
        token0,
        token1,
        EMAIL_ADD,
        this.timeStart().plusHours(6L)
      );

    when(transaction.queries(IdDatabaseEmailsQueriesType.class))
      .thenReturn(emails);
    when(emails.emailVerificationGetPermit(any()))
      .thenReturn(Optional.of(verification));

    /* Act. */

    final var handler = new IdUCmdEmailRemovePermit();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(context, new IdUCommandEmailRemovePermit(token0));
      });

    /* Assert. */

    assertEquals(EMAIL_VERIFICATION_FAILED, ex.errorCode());
  }

  /**
   * Valid tokens succeed.
   *
   * @throws Exception On errors
   */

  @Test
  public void testTokenOK()
    throws Exception
  {
    /* Arrange. */

    final var user0 =
      this.createUserAndSessionWithEmails("user0");
    final var context =
      this.createContextAndSession(user0);

    final var emails =
      mock(IdDatabaseEmailsQueriesType.class);
    final var users =
      mock(IdDatabaseUsersQueriesType.class);

    final var transaction =
      this.transaction();
    final var token0 =
      IdToken.generate();
    final var token1 =
      IdToken.generate();

    final var verification =
      new IdEmailVerification(
        user0.id(),
        user0.emails().first(),
        token0,
        token1,
        EMAIL_REMOVE,
        this.timeStart().plusHours(6L)
      );

    when(transaction.queries(IdDatabaseEmailsQueriesType.class))
      .thenReturn(emails);
    when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);
    when(emails.emailVerificationGetPermit(any()))
      .thenReturn(Optional.of(verification));

    /* Act. */

    final var handler =
      new IdUCmdEmailRemovePermit();
    final var response =
      handler.execute(context, new IdUCommandEmailRemovePermit(token0));

    /* Assert. */

    assertEquals(
      new IdUResponseEmailRemovePermit(context.requestId()),
      response
    );

    verify(transaction, this.once())
      .userIdSet(user0.id());
    verify(transaction, this.once())
      .queries(IdDatabaseEmailsQueriesType.class);
    verify(transaction, this.once())
      .queries(IdDatabaseUsersQueriesType.class);

    verify(users, this.once())
      .userEmailRemove(user0.id(), user0.emails().first());

    verify(emails, this.once())
      .emailVerificationGetPermit(token0);
    verify(emails, this.once())
      .emailVerificationDelete(token0, PERMITTED);

    verifyNoMoreInteractions(emails);
    verifyNoMoreInteractions(transaction);
    verifyNoMoreInteractions(users);
  }

  /**
   * The last email address on a user cannot be removed.
   *
   * @throws Exception On errors
   */

  @Test
  public void testEmailLast()
    throws Exception
  {
    /* Arrange. */

    final var user0 =
      this.createUser("user0");
    final var context =
      this.createContextAndSession(user0);

    final var emails =
      mock(IdDatabaseEmailsQueriesType.class);
    final var transaction =
      this.transaction();
    final var token0 =
      IdToken.generate();
    final var token1 =
      IdToken.generate();

    final var verification =
      new IdEmailVerification(
        user0.id(),
        user0.emails().first(),
        token0,
        token1,
        EMAIL_REMOVE,
        this.timeStart().plusHours(6L)
      );

    when(transaction.queries(IdDatabaseEmailsQueriesType.class))
      .thenReturn(emails);
    when(emails.emailVerificationGetPermit(any()))
      .thenReturn(Optional.of(verification));

    /* Act. */

    final var handler = new IdUCmdEmailRemovePermit();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(context, new IdUCommandEmailRemovePermit(token0));
      });

    /* Assert. */

    assertEquals(EMAIL_VERIFICATION_FAILED, ex.errorCode());
  }
}

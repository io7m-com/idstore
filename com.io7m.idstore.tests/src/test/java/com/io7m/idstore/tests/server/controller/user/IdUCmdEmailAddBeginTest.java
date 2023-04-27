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
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdEmailOwner;
import com.io7m.idstore.model.IdEmailVerification;
import com.io7m.idstore.protocol.user.IdUCommandEmailAddBegin;
import com.io7m.idstore.protocol.user.IdUResponseEmailAddBegin;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.controller.user.IdUCmdEmailAddBegin;
import com.io7m.idstore.server.service.templating.IdFMTemplateType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.EMAIL_DUPLICATE;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.IO_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.MAIL_SYSTEM_FAILURE;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.RATE_LIMIT_EXCEEDED;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public final class IdUCmdEmailAddBeginTest
  extends IdUCmdAbstractContract
{
  /**
   * Requests are rejected by rate limiting.
   *
   * @throws Exception On errors
   */

  @Test
  public void testRateLimited()
    throws Exception
  {
    /* Arrange. */

    final var user0 =
      this.createUser("user0");
    final var context =
      this.createContextAndSession(user0);

    when(this.rateLimit().isAllowedByRateLimit(any()))
      .thenReturn(FALSE);

    final var email =
      new IdEmail("someone-new@example.com");

    /* Act. */

    final var handler = new IdUCmdEmailAddBegin();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(context, new IdUCommandEmailAddBegin(email));
      });

    /* Assert. */

    assertEquals(RATE_LIMIT_EXCEEDED, ex.errorCode());
  }

  /**
   * An existing email address cannot be added.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAlreadyExists()
    throws Exception
  {
    /* Arrange. */

    final var user0 =
      this.createUser("user0");
    final var context =
      this.createContextAndSession(user0);

    when(this.rateLimit().isAllowedByRateLimit(any()))
      .thenReturn(TRUE);

    final var emails =
      mock(IdDatabaseEmailsQueriesType.class);

    final var email =
      user0.emails().first();

    when(emails.emailExists(any()))
      .thenReturn(Optional.of(new IdEmailOwner(false, user0.id(), email)));

    final var transaction =
      this.transaction();

    when(transaction.queries(IdDatabaseEmailsQueriesType.class))
      .thenReturn(emails);

    /* Act. */

    final var handler = new IdUCmdEmailAddBegin();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(
          context,
          new IdUCommandEmailAddBegin(email)
        );
      });

    /* Assert. */

    assertEquals(EMAIL_DUPLICATE, ex.errorCode());
  }

  /**
   * Starting email verification works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testOK()
    throws Exception
  {
    /* Arrange. */

    final var user0 =
      this.createUser("user0");
    final var context =
      this.createContextAndSession(user0);

    final var rateLimitService =
      this.rateLimit();
    final var transaction =
      this.transaction();
    final var brandingService =
      this.branding();
    final var templateService =
      this.templates();
    final var mailService =
      this.mail();

    when(rateLimitService.isAllowedByRateLimit(any()))
      .thenReturn(TRUE);

    final var emails =
      mock(IdDatabaseEmailsQueriesType.class);

    final var email =
      user0.emails().first();

    when(emails.emailExists(any()))
      .thenReturn(Optional.empty());

    when(transaction.queries(IdDatabaseEmailsQueriesType.class))
      .thenReturn(emails);

    when(brandingService.title())
      .thenReturn("idstore");

    final var template =
      mock(IdFMTemplateType.class);

    when(templateService.emailVerificationTemplate())
      .thenReturn(template);

    when(mailService.sendMail(any(), any(), any(), any(), any(), any()))
      .thenReturn(CompletableFuture.completedFuture(null));

    /* Act. */

    final var handler =
      new IdUCmdEmailAddBegin();
    final var response =
      handler.execute(context, new IdUCommandEmailAddBegin(email));

    /* Assert. */

    assertEquals(
      new IdUResponseEmailAddBegin(context.requestId()),
      response
    );

    verify(rateLimitService, this.once())
      .isAllowedByRateLimit(eq(user0.id()));

    verify(transaction, this.once())
      .queries(IdDatabaseEmailsQueriesType.class);

    verify(transaction, this.once())
      .userIdSet(user0.id());

    verify(emails, this.once())
      .emailExists(eq(email));

    verify(emails, this.once())
      .emailVerificationCreate(argThat(verification -> {
        return verificationHasEmail(verification, email)
               && verificationHasUser(verification.user(), user0.id());
      }));

    verify(brandingService, this.once())
      .title();

    verify(brandingService, this.once())
      .emailSubject(any());

    verify(mailService, this.once())
      .sendMail(
        any(),
        eq(context.requestId()),
        eq(email),
        any(),
        any(),
        any()
      );

    verify(template, this.once())
      .process(any(), any());

    verifyNoMoreInteractions(brandingService);
    verifyNoMoreInteractions(emails);
    verifyNoMoreInteractions(mailService);
    verifyNoMoreInteractions(rateLimitService);
    verifyNoMoreInteractions(template);
    verifyNoMoreInteractions(transaction);
  }

  /**
   * Mail service failures are indicated.
   *
   * @throws Exception On errors
   */

  @Test
  public void testMailServiceFails()
    throws Exception
  {
    /* Arrange. */

    final var user0 =
      this.createUser("user0");
    final var context =
      this.createContextAndSession(user0);

    final var rateLimitService =
      this.rateLimit();
    final var transaction =
      this.transaction();
    final var brandingService =
      this.branding();
    final var templateService =
      this.templates();
    final var mailService =
      this.mail();

    when(rateLimitService.isAllowedByRateLimit(any()))
      .thenReturn(TRUE);

    final var emails =
      mock(IdDatabaseEmailsQueriesType.class);

    final var email =
      user0.emails().first();

    when(emails.emailExists(any()))
      .thenReturn(Optional.empty());

    when(transaction.queries(IdDatabaseEmailsQueriesType.class))
      .thenReturn(emails);

    when(brandingService.title())
      .thenReturn("idstore");

    final var template =
      mock(IdFMTemplateType.class);

    when(templateService.emailVerificationTemplate())
      .thenReturn(template);

    final var exception =
      new IOException("Printer on fire.");

    when(mailService.sendMail(any(), any(), any(), any(), any(), any()))
      .thenReturn(CompletableFuture.failedFuture(exception));

    /* Act. */

    final var handler =
      new IdUCmdEmailAddBegin();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(context, new IdUCommandEmailAddBegin(email));
      });

    /* Assert. */

    assertEquals(MAIL_SYSTEM_FAILURE, ex.errorCode());

    verify(rateLimitService, this.once())
      .isAllowedByRateLimit(eq(user0.id()));

    verify(transaction, this.once())
      .queries(IdDatabaseEmailsQueriesType.class);

    verify(transaction, this.once())
      .userIdSet(user0.id());

    verify(emails, this.once())
      .emailExists(eq(email));

    verify(emails, this.once())
      .emailVerificationCreate(argThat(verification -> {
        return verificationHasEmail(verification, email)
               && verificationHasUser(verification.user(), user0.id());
      }));

    verify(brandingService, this.once())
      .title();

    verify(brandingService, this.once())
      .emailSubject(any());

    verify(mailService, this.once())
      .sendMail(
        any(),
        eq(context.requestId()),
        eq(email),
        any(),
        any(),
        any()
      );

    verify(template, this.once())
      .process(any(), any());

    verifyNoMoreInteractions(brandingService);
    verifyNoMoreInteractions(emails);
    verifyNoMoreInteractions(mailService);
    verifyNoMoreInteractions(rateLimitService);
    verifyNoMoreInteractions(template);
    verifyNoMoreInteractions(transaction);
  }

  /**
   * Template failures are indicated.
   *
   * @throws Exception On errors
   */

  @Test
  public void testTemplateCrashes()
    throws Exception
  {
    /* Arrange. */

    final var user0 =
      this.createUser("user0");
    final var context =
      this.createContextAndSession(user0);

    final var rateLimitService =
      this.rateLimit();
    final var transaction =
      this.transaction();
    final var brandingService =
      this.branding();
    final var templateService =
      this.templates();
    final var mailService =
      this.mail();

    when(rateLimitService.isAllowedByRateLimit(any()))
      .thenReturn(TRUE);

    final var emails =
      mock(IdDatabaseEmailsQueriesType.class);

    final var email =
      user0.emails().first();

    when(emails.emailExists(any()))
      .thenReturn(Optional.empty());

    when(transaction.queries(IdDatabaseEmailsQueriesType.class))
      .thenReturn(emails);

    when(brandingService.title())
      .thenReturn("idstore");

    final var template =
      mock(IdFMTemplateType.class);

    when(templateService.emailVerificationTemplate())
      .thenReturn(template);

    final var exception =
      new IOException("Template on fire.");

    doThrow(exception)
      .when(template)
      .process(any(), any());

    /* Act. */

    final var handler =
      new IdUCmdEmailAddBegin();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(context, new IdUCommandEmailAddBegin(email));
      });

    /* Assert. */

    assertEquals(IO_ERROR, ex.errorCode());

    verify(rateLimitService, this.once())
      .isAllowedByRateLimit(eq(user0.id()));

    verify(transaction, this.once())
      .queries(IdDatabaseEmailsQueriesType.class);

    verify(transaction, this.once())
      .userIdSet(user0.id());

    verify(emails, this.once())
      .emailExists(eq(email));

    verify(emails, this.once())
      .emailVerificationCreate(argThat(verification -> {
        return verificationHasEmail(verification, email)
               && verificationHasUser(verification.user(), user0.id());
      }));

    verify(brandingService, this.once())
      .title();

    verify(template, this.once())
      .process(any(), any());

    verifyNoMoreInteractions(brandingService);
    verifyNoMoreInteractions(emails);
    verifyNoMoreInteractions(mailService);
    verifyNoMoreInteractions(rateLimitService);
    verifyNoMoreInteractions(template);
    verifyNoMoreInteractions(transaction);
  }

  private static boolean verificationHasUser(
    final UUID user,
    final UUID id)
  {
    return Objects.equals(user, id);
  }

  private static boolean verificationHasEmail(
    final IdEmailVerification verification,
    final IdEmail email)
  {
    return Objects.equals(verification.email(), email);
  }
}

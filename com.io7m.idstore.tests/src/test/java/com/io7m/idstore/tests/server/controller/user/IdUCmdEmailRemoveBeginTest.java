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
import com.io7m.idstore.model.IdEmailVerification;
import com.io7m.idstore.protocol.user.IdUCommandEmailRemoveBegin;
import com.io7m.idstore.protocol.user.IdUResponseEmailRemoveBegin;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.controller.user.IdUCmdEmailRemoveBegin;
import com.io7m.idstore.server.service.templating.IdFMTemplateType;
import org.junit.jupiter.api.Test;
import org.mockito.internal.verification.Times;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.EMAIL_NONEXISTENT;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.EMAIL_VERIFICATION_FAILED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.IO_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.MAIL_SYSTEM_FAILURE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public final class IdUCmdEmailRemoveBeginTest
  extends IdUCmdAbstractContract
{
  /**
   * You cannot remove an email address you do not have.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAddressNotOwned()
    throws Exception
  {
    /* Arrange. */

    when(this.rateLimit().isAllowedByRateLimit(any()))
      .thenReturn(Boolean.TRUE);

    final var user0 =
      this.createUserAndSessionWithEmails("user");
    final var context =
      this.createContextAndSession(user0);

    final var emails =
      mock(IdDatabaseEmailsQueriesType.class);

    final var email =
      new IdEmail("difference@example.com");

    final var transaction =
      this.transaction();

    when(transaction.queries(IdDatabaseEmailsQueriesType.class))
      .thenReturn(emails);

    /* Act. */

    final var handler = new IdUCmdEmailRemoveBegin();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(
          context,
          new IdUCommandEmailRemoveBegin(email)
        );
      });

    /* Assert. */

    assertEquals(EMAIL_NONEXISTENT, ex.errorCode());
  }

  /**
   * You cannot remove the last email address.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAddressLast()
    throws Exception
  {
    /* Arrange. */

    when(this.rateLimit().isAllowedByRateLimit(any()))
      .thenReturn(Boolean.TRUE);

    final var user0 =
      this.createUser("user");
    final var context =
      this.createContextAndSession(user0);

    final var emails =
      mock(IdDatabaseEmailsQueriesType.class);

    final var email =
      user0.emails().first();

    final var transaction =
      this.transaction();

    when(transaction.queries(IdDatabaseEmailsQueriesType.class))
      .thenReturn(emails);

    /* Act. */

    final var handler = new IdUCmdEmailRemoveBegin();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(
          context,
          new IdUCommandEmailRemoveBegin(email)
        );
      });

    /* Assert. */

    assertEquals(EMAIL_VERIFICATION_FAILED, ex.errorCode());
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

    when(this.rateLimit().isAllowedByRateLimit(any()))
      .thenReturn(Boolean.TRUE);

    final var user0 =
      this.createUserAndSessionWithEmails("user");
    final var context =
      this.createContextAndSession(user0);

    final var transaction =
      this.transaction();
    final var brandingService =
      this.branding();
    final var templateService =
      this.templates();
    final var mailService =
      this.mail();

    final var emails =
      mock(IdDatabaseEmailsQueriesType.class);

    final var email =
      user0.emails().first();

    when(emails.emailExists(any()))
      .thenReturn(Optional.empty());

    when(emails.emailVerificationCount())
      .thenReturn(0L);

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
      new IdUCmdEmailRemoveBegin();
    final var response =
      handler.execute(context, new IdUCommandEmailRemoveBegin(email));

    /* Assert. */

    assertEquals(
      new IdUResponseEmailRemoveBegin(context.requestId()),
      response
    );

    verify(transaction, this.once())
      .queries(IdDatabaseEmailsQueriesType.class);

    verify(transaction, atLeast(1))
      .userIdSet(user0.id());

    verify(emails, this.once())
      .emailVerificationCreate(argThat(verification -> {
        return verificationHasEmail(verification, email)
               && verificationHasUser(verification.user(), user0.id());
      }));

    verify(emails, this.once())
      .emailVerificationCount();

    /*
     * The branding service is called once per existing email.
     */

    verify(brandingService, new Times(user0.emails().size()))
      .title();
    verify(brandingService, new Times(user0.emails().size()))
      .emailSubject(any());

    /*
     * One email is sent per registered email address, and one extra one
     * for the new address. The other email addresses don't get a "Permit"
     * link.
     */

    for (final var emailExisting : user0.emails().toList()) {
      if (!Objects.equals(emailExisting, email)) {
        verify(mailService, this.once())
          .sendMail(
            any(),
            eq(context.requestId()),
            eq(emailExisting),
            argThat(headers -> {
              return headers.containsKey("X-IDStore-Verification-Token-Deny")
                && !headers.containsKey("X-IDStore-Verification-Token-Permit");
            }),
            any(),
            any()
          );
      }
    }

    verify(mailService, this.once())
      .sendMail(
        any(),
        eq(context.requestId()),
        eq(email),
        argThat(headers -> {
          return headers.containsKey("X-IDStore-Verification-Token-Deny")
            && headers.containsKey("X-IDStore-Verification-Token-Permit");
        }),
        any(),
        any()
      );

    verify(template, new Times(user0.emails().size()))
      .process(any(), any());

    verifyNoMoreInteractions(brandingService);
    verifyNoMoreInteractions(emails);
    verifyNoMoreInteractions(mailService);
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
      this.createUserAndSessionWithEmails("user");
    final var context =
      this.createContextAndSession(user0);

    final var transaction =
      this.transaction();
    final var brandingService =
      this.branding();
    final var templateService =
      this.templates();
    final var mailService =
      this.mail();

    when(this.rateLimit().isAllowedByRateLimit(any()))
      .thenReturn(Boolean.TRUE);

    final var emails =
      mock(IdDatabaseEmailsQueriesType.class);

    final var email =
      user0.emails().first();

    when(emails.emailExists(any()))
      .thenReturn(Optional.empty());

    when(emails.emailVerificationCount())
      .thenReturn(0L);

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
      new IdUCmdEmailRemoveBegin();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(context, new IdUCommandEmailRemoveBegin(email));
      });

    /* Assert. */

    assertEquals(MAIL_SYSTEM_FAILURE, ex.errorCode());

    verify(transaction, this.once())
      .queries(IdDatabaseEmailsQueriesType.class);

    verify(transaction, atLeast(1))
      .userIdSet(user0.id());

    verify(emails, this.once())
      .emailVerificationCreate(argThat(verification -> {
        return verificationHasEmail(verification, email)
               && verificationHasUser(verification.user(), user0.id());
      }));

    verify(emails, this.once())
      .emailVerificationCount();

    verify(brandingService, this.once())
      .title();

    verify(brandingService, this.once())
      .emailSubject(any());

    verify(mailService, this.once())
      .sendMail(
        any(),
        eq(context.requestId()),
        any(),
        any(),
        any(),
        any()
      );

    verify(template, this.once())
      .process(any(), any());

    verifyNoMoreInteractions(brandingService);
    verifyNoMoreInteractions(emails);
    verifyNoMoreInteractions(mailService);
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

    when(this.rateLimit().isAllowedByRateLimit(any()))
      .thenReturn(Boolean.TRUE);

    final var user0 =
      this.createUserAndSessionWithEmails("user");
    final var context =
      this.createContextAndSession(user0);

    final var transaction =
      this.transaction();
    final var brandingService =
      this.branding();
    final var templateService =
      this.templates();
    final var mailService =
      this.mail();

    final var emails =
      mock(IdDatabaseEmailsQueriesType.class);

    final var email =
      user0.emails().first();

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
      new IdUCmdEmailRemoveBegin();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(context, new IdUCommandEmailRemoveBegin(email));
      });

    /* Assert. */

    assertEquals(IO_ERROR, ex.errorCode());

    verify(transaction, this.once())
      .queries(IdDatabaseEmailsQueriesType.class);

    verify(transaction, atLeast(1))
      .userIdSet(user0.id());

    verify(emails, this.once())
      .emailVerificationCreate(argThat(verification -> {
        return verificationHasEmail(verification, email)
               && verificationHasUser(verification.user(), user0.id());
      }));

    verify(emails, this.once())
      .emailVerificationCount();

    verify(brandingService, this.once())
      .title();

    verify(template, this.once())
      .process(any(), any());

    verifyNoMoreInteractions(brandingService);
    verifyNoMoreInteractions(emails);
    verifyNoMoreInteractions(mailService);
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

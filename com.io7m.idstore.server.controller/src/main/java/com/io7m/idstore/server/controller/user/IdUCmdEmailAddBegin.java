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

package com.io7m.idstore.server.controller.user;

import com.io7m.idstore.database.api.IdDatabaseEmailsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.error_codes.IdException;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdEmailVerification;
import com.io7m.idstore.model.IdToken;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.protocol.user.IdUCommandEmailAddBegin;
import com.io7m.idstore.protocol.user.IdUResponseEmailAddBegin;
import com.io7m.idstore.protocol.user.IdUResponseType;
import com.io7m.idstore.server.api.IdServerConfiguration;
import com.io7m.idstore.server.api.IdServerMailConfiguration;
import com.io7m.idstore.server.controller.IdServerStrings;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.security.IdSecUserActionEmailAddBegin;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationService;
import com.io7m.idstore.server.service.events.IdEventServiceType;
import com.io7m.idstore.server.service.events.IdEventUserEmailVerificationRateLimitExceeded;
import com.io7m.idstore.server.service.mail.IdServerMailServiceType;
import com.io7m.idstore.server.service.ratelimit.IdRateLimitEmailVerificationServiceType;
import com.io7m.idstore.server.service.templating.IdFMEmailVerificationData;
import com.io7m.idstore.server.service.templating.IdFMTemplateServiceType;
import io.opentelemetry.api.trace.Span;

import java.io.StringWriter;
import java.util.Map;
import java.util.Optional;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.EMAIL_DUPLICATE;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.IO_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.RATE_LIMIT_EXCEEDED;
import static com.io7m.idstore.model.IdEmailVerificationOperation.EMAIL_ADD;

/**
 * IdUCmdEmailAddBegin
 */

public final class IdUCmdEmailAddBegin
  extends IdUCmdAbstract<IdUCommandEmailAddBegin>
{
  /**
   * IdUCmdEmailAddBegin
   */

  public IdUCmdEmailAddBegin()
  {

  }

  @Override
  protected IdUResponseType executeActual(
    final IdUCommandContext context,
    final IdUCommandEmailAddBegin command)
    throws IdException
  {
    final var services =
      context.services();
    final var templateService =
      services.requireService(IdFMTemplateServiceType.class);
    final var configurationService =
      services.requireService(IdServerConfigurationService.class);
    final var mailService =
      services.requireService(IdServerMailServiceType.class);
    final var strings =
      services.requireService(IdServerStrings.class);
    final var brandingService =
      services.requireService(IdServerBrandingServiceType.class);
    final var rateLimitService =
      services.requireService(IdRateLimitEmailVerificationServiceType.class);
    final var eventService =
      services.requireService(IdEventServiceType.class);

    final var configuration =
      configurationService.configuration();
    final var mailConfiguration =
      configuration.mailConfiguration();
    final var transaction =
      context.transaction();
    final var emails =
      transaction.queries(IdDatabaseEmailsQueriesType.class);

    final var email =
      command.email();
    final var user =
      context.user();

    transaction.userIdSet(user.id());
    if (!rateLimitService.isAllowedByRateLimit(user.id())) {
      eventService.emit(
        new IdEventUserEmailVerificationRateLimitExceeded(user.id(), email)
      );

      throw context.fail(
        400,
        RATE_LIMIT_EXCEEDED,
        strings.format("emailVerificationRateLimited")
      );
    }

    context.securityCheck(
      new IdSecUserActionEmailAddBegin(user, emails.emailVerificationCount())
    );


    checkPreconditions(context, emails, strings, email);

    transaction.userIdSet(user.id());

    final var verification =
      createVerification(context, emails, mailConfiguration, user, email);

    /*
     * Send a "deny" link to all registered email addresses except for the new
     * one. In the case that a user's account is compromised, and a hostile
     * party tries to add an email address to it, this prevents the operation
     * from happening silently - the user will know something is wrong!
     */

    for (final var emailExisting : user.emails().toList()) {
      sendVerificationMailWithoutPermitLink(
        context,
        templateService,
        configuration,
        mailService,
        brandingService,
        emailExisting,
        verification
      );
    }

    /*
     * Send a "permit" (and a "deny") link to the requested email address. This
     * forces the user to prove that they control the email address being added.
     */

    sendVerificationMailWithPermitLink(
      context,
      templateService,
      configuration,
      mailService,
      brandingService,
      email,
      verification
    );

    return new IdUResponseEmailAddBegin(context.requestId());
  }

  private static void sendVerificationMailWithoutPermitLink(
    final IdUCommandContext context,
    final IdFMTemplateServiceType templateService,
    final IdServerConfiguration configuration,
    final IdServerMailServiceType mailService,
    final IdServerBrandingServiceType brandingService,
    final IdEmail email,
    final IdEmailVerification verification)
    throws IdCommandExecutionFailure
  {
    final var template =
      templateService.emailVerificationTemplate();

    final var linkDeny =
      configuration.userViewAddress()
        .externalAddress()
        .resolve("/email-verification-deny/?token=%s"
                   .formatted(verification.tokenDeny()))
        .normalize();

    final var writer = new StringWriter();
    try {
      template.process(
        new IdFMEmailVerificationData(
          brandingService.title(),
          verification,
          context.remoteHost(),
          context.remoteUserAgent(),
          Optional.empty(),
          linkDeny
        ),
        writer
      );
    } catch (final Exception e) {
      throw new IdCommandExecutionFailure(
        e.getMessage(),
        e,
        IO_ERROR,
        Map.of(),
        Optional.empty(),
        context.requestId(),
        500
      );
    }

    final var mailHeaders =
      Map.ofEntries(
        Map.entry(
          "X-IDStore-Verification-Token-Deny",
          verification.tokenDeny().value()),
        Map.entry(
          "X-IDStore-Verification-From-Request",
          context.requestId().toString()),
        Map.entry(
          "X-IDStore-Verification-Deny",
          linkDeny.toString())
      );

    try {
      mailService.sendMail(
        Span.current(),
        context.requestId(),
        email,
        mailHeaders,
        brandingService.emailSubject("Email verification request"),
        writer.toString()
      ).get();
    } catch (final Exception e) {
      throw context.failMail(email, e);
    }
  }

  private static void sendVerificationMailWithPermitLink(
    final IdUCommandContext context,
    final IdFMTemplateServiceType templateService,
    final IdServerConfiguration configuration,
    final IdServerMailServiceType mailService,
    final IdServerBrandingServiceType brandingService,
    final IdEmail email,
    final IdEmailVerification verification)
    throws IdCommandExecutionFailure
  {
    final var template =
      templateService.emailVerificationTemplate();

    final var linkPermit =
      configuration.userViewAddress()
        .externalAddress()
        .resolve("/email-verification-permit/?token=%s"
                   .formatted(verification.tokenPermit()))
        .normalize();

    final var linkDeny =
      configuration.userViewAddress()
        .externalAddress()
        .resolve("/email-verification-deny/?token=%s"
                   .formatted(verification.tokenDeny()))
        .normalize();

    final var writer = new StringWriter();
    try {
      template.process(
        new IdFMEmailVerificationData(
          brandingService.title(),
          verification,
          context.remoteHost(),
          context.remoteUserAgent(),
          Optional.of(linkPermit),
          linkDeny
        ),
        writer
      );
    } catch (final Exception e) {
      throw new IdCommandExecutionFailure(
        e.getMessage(),
        e,
        IO_ERROR,
        Map.of(),
        Optional.empty(),
        context.requestId(),
        500
      );
    }

    final var mailHeaders =
      Map.ofEntries(
        Map.entry(
          "X-IDStore-Verification-Token-Permit",
          verification.tokenPermit().value()),
        Map.entry(
          "X-IDStore-Verification-Token-Deny",
          verification.tokenDeny().value()),
        Map.entry(
          "X-IDStore-Verification-From-Request",
          context.requestId().toString()),
        Map.entry(
          "X-IDStore-Verification-Permit",
          linkPermit.toString()),
        Map.entry(
          "X-IDStore-Verification-Deny",
          linkDeny.toString())
      );

    try {
      mailService.sendMail(
        Span.current(),
        context.requestId(),
        email,
        mailHeaders,
        brandingService.emailSubject("Email verification request"),
        writer.toString()
      ).get();
    } catch (final Exception e) {
      throw context.failMail(email, e);
    }
  }

  private static IdEmailVerification createVerification(
    final IdUCommandContext context,
    final IdDatabaseEmailsQueriesType emails,
    final IdServerMailConfiguration mailConfiguration,
    final IdUser user,
    final IdEmail email)
    throws IdDatabaseException
  {
    final var tokenAllow =
      IdToken.generate();
    final var tokenDeny =
      IdToken.generate();
    final var expires =
      context.now().plus(mailConfiguration.verificationExpiration());
    final var verification =
      new IdEmailVerification(
        user.id(),
        email,
        tokenAllow,
        tokenDeny,
        EMAIL_ADD,
        expires
      );

    emails.emailVerificationCreate(verification);
    return verification;
  }

  private static void checkPreconditions(
    final IdUCommandContext context,
    final IdDatabaseEmailsQueriesType emails,
    final IdServerStrings strings,
    final IdEmail email)
    throws IdDatabaseException, IdCommandExecutionFailure
  {
    final var existingOpt = emails.emailExists(email);
    if (existingOpt.isPresent()) {
      throw new IdCommandExecutionFailure(
        strings.format("emailDuplicate"),
        EMAIL_DUPLICATE,
        Map.of(),
        Optional.empty(),
        context.requestId(),
        400
      );
    }
  }
}

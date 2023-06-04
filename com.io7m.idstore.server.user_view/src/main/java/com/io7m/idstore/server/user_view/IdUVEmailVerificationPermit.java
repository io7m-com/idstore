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


package com.io7m.idstore.server.user_view;

import com.io7m.idstore.database.api.IdDatabaseEmailsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.model.IdToken;
import com.io7m.idstore.model.IdValidityException;
import com.io7m.idstore.protocol.user.IdUCommandEmailAddPermit;
import com.io7m.idstore.protocol.user.IdUCommandEmailRemovePermit;
import com.io7m.idstore.server.controller.IdServerStrings;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.controller.user.IdUCmdEmailAddPermit;
import com.io7m.idstore.server.controller.user.IdUCmdEmailRemovePermit;
import com.io7m.idstore.server.controller.user.IdUCommandContext;
import com.io7m.idstore.server.http.IdHTTPServletFunctional;
import com.io7m.idstore.server.http.IdHTTPServletFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPServletRequestInformation;
import com.io7m.idstore.server.http.IdHTTPServletResponseType;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.sessions.IdSessionSecretIdentifier;
import com.io7m.idstore.server.service.sessions.IdSessionUser;
import com.io7m.idstore.server.service.templating.IdFMMessageData;
import com.io7m.idstore.server.service.templating.IdFMTemplateServiceType;
import com.io7m.idstore.server.service.templating.IdFMTemplateType;
import com.io7m.jvindicator.core.Vindication;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import jakarta.servlet.http.HttpServletRequest;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.server.http.IdHTTPServletCoreInstrumented.withInstrumentation;
import static com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType.setSpanErrorCode;

/**
 * The endpoint that allows for completing email verification challenges.
 */

public final class IdUVEmailVerificationPermit
  extends IdHTTPServletFunctional
{
  private static final String DESTINATION_ON_FAILURE = "/";

  /**
   * The page that executes a password update.
   *
   * @param services The services
   */

  public IdUVEmailVerificationPermit(
    final RPServiceDirectoryType services)
  {
    super(createCore(services));
  }

  private static IdHTTPServletFunctionalCoreType createCore(
    final RPServiceDirectoryType services)
  {
    final var database =
      services.requireService(IdDatabaseType.class);
    final var strings =
      services.requireService(IdServerStrings.class);
    final var branding =
      services.requireService(IdServerBrandingServiceType.class);
    final var errorTemplate =
      services.requireService(IdFMTemplateServiceType.class)
        .pageMessage();

    return withInstrumentation(services, (request, information) -> {
      return execute(
        services,
        strings,
        database,
        branding,
        errorTemplate,
        request,
        information
      );
    });
  }

  private static IdHTTPServletResponseType execute(
    final RPServiceDirectoryType services,
    final IdServerStrings strings,
    final IdDatabaseType database,
    final IdServerBrandingServiceType branding,
    final IdFMTemplateType<IdFMMessageData> errorTemplate,
    final HttpServletRequest request,
    final IdHTTPServletRequestInformation information)
  {
    final var vindicator =
      Vindication.startWithExceptions(IdValidityException::new);
    final var tokenParameter =
      vindicator.addRequiredParameter("token", IdToken::new);

    try {
      vindicator.check(request.getParameterMap());
    } catch (final IdValidityException e) {
      return IdUVErrorPage.showError(
        strings,
        branding,
        errorTemplate,
        information,
        400,
        e.getMessage(),
        DESTINATION_ON_FAILURE
      );
    }

    try {
      return runForToken(
        services,
        database,
        strings,
        branding,
        errorTemplate,
        information,
        tokenParameter.get()
      );
    } catch (final IdDatabaseException e) {
      setSpanErrorCode(e.errorCode());
      return IdUVErrorPage.showError(
        strings,
        branding,
        errorTemplate,
        information,
        500,
        e.getMessage(),
        DESTINATION_ON_FAILURE
      );
    } catch (final IdCommandExecutionFailure e) {
      setSpanErrorCode(e.errorCode());
      return IdUVErrorPage.showError(
        strings,
        branding,
        errorTemplate,
        information,
        e.httpStatusCode(),
        e.getMessage(),
        DESTINATION_ON_FAILURE
      );
    }
  }

  private static IdHTTPServletResponseType runForToken(
    final RPServiceDirectoryType services,
    final IdDatabaseType database,
    final IdServerStrings strings,
    final IdServerBrandingServiceType branding,
    final IdFMTemplateType<IdFMMessageData> msgTemplate,
    final IdHTTPServletRequestInformation information,
    final IdToken idToken)
    throws IdDatabaseException, IdCommandExecutionFailure
  {
    try (var connection = database.openConnection(IDSTORE)) {
      try (var transaction =
             connection.openTransaction()) {
        final var emails =
          transaction.queries(IdDatabaseEmailsQueriesType.class);
        final var users =
          transaction.queries(IdDatabaseUsersQueriesType.class);
        final var verificationOpt =
          emails.emailVerificationGetPermit(idToken);

        if (verificationOpt.isEmpty()) {
          return IdUVErrorPage.showError(
            strings,
            branding,
            msgTemplate,
            information,
            400,
            strings.format("notFound"),
            DESTINATION_ON_FAILURE
          );
        }

        final var verification =
          verificationOpt.get();
        final var user =
          users.userGetRequire(verification.user());

        /*
         * Create a fake user session in order to run the command. The session
         * is not entered into the session service and so can't be used for
         * any other commands.
         */

        final var fakeSession =
          new IdSessionUser(
            user.id(),
            IdSessionSecretIdentifier.generate()
          );

        final var commandContext =
          new IdUCommandContext(
            services,
            information.requestId(),
            transaction,
            fakeSession,
            user,
            information.remoteAddress(),
            information.userAgent()
          );

        switch (verification.operation()) {
          case EMAIL_ADD -> {
            final var command =
              new IdUCommandEmailAddPermit(idToken);
            new IdUCmdEmailAddPermit()
              .execute(commandContext, command);
          }
          case EMAIL_REMOVE -> {
            final var command =
              new IdUCommandEmailRemovePermit(idToken);
            new IdUCmdEmailRemovePermit()
              .execute(commandContext, command);
          }
        }

        transaction.commit();
        return IdUVEmailVerification.showSuccess(
          strings,
          branding,
          msgTemplate,
          information
        );
      }
    }
  }
}

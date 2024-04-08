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

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.model.IdValidityException;
import com.io7m.idstore.protocol.user.IdUCommandEmailRemoveBegin;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.controller.user.IdUCmdEmailRemoveBegin;
import com.io7m.idstore.server.controller.user.IdUCommandContext;
import com.io7m.idstore.server.http.IdHTTPHandlerFunctional;
import com.io7m.idstore.server.http.IdHTTPHandlerFunctionalCoreAuthenticatedType;
import com.io7m.idstore.server.http.IdHTTPHandlerFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPRequestInformation;
import com.io7m.idstore.server.http.IdHTTPResponseType;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.sessions.IdSessionMessage;
import com.io7m.idstore.server.service.sessions.IdSessionUser;
import com.io7m.idstore.server.service.templating.IdFMMessageData;
import com.io7m.idstore.server.service.templating.IdFMTemplateServiceType;
import com.io7m.idstore.server.service.templating.IdFMTemplateType;
import com.io7m.idstore.strings.IdStrings;
import com.io7m.jvindicator.core.Vindication;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import io.helidon.webserver.http.ServerRequest;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.model.IdUserDomain.USER;
import static com.io7m.idstore.server.http.IdHTTPHandlerCoreInstrumented.withInstrumentation;
import static com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType.setSpanErrorCode;
import static com.io7m.idstore.server.user_view.IdUVHandlerCoreAuthenticated.withAuthentication;
import static com.io7m.idstore.server.user_view.IdUVHandlerCoreMaintenanceAware.withMaintenanceAwareness;
import static com.io7m.idstore.strings.IdStringConstants.EMAIL_VERIFICATION_SENT;
import static com.io7m.idstore.strings.IdStringConstants.EMAIL_VERIFICATION_TITLE;
import static com.io7m.idstore.strings.IdStringConstants.ERROR;

/**
 * The page that executes the email removal.
 */

public final class IdUVEmailRemoveRun extends IdHTTPHandlerFunctional
{
  private static final String DESTINATION_ON_FAILURE = "/";
  private static final String DESTINATION_ON_SUCCESS = "/";

  /**
   * The page that executes the email removal.
   *
   * @param services The services
   */

  public IdUVEmailRemoveRun(
    final RPServiceDirectoryType services)
  {
    super(createCore(services));
  }

  private static IdHTTPHandlerFunctionalCoreType createCore(
    final RPServiceDirectoryType services)
  {
    final var database =
      services.requireService(IdDatabaseType.class);
    final var strings =
      services.requireService(IdStrings.class);
    final var branding =
      services.requireService(IdServerBrandingServiceType.class);
    final var template =
      services.requireService(IdFMTemplateServiceType.class)
        .pageMessage();

    final IdHTTPHandlerFunctionalCoreAuthenticatedType<IdSessionUser, IdUser> main =
      (request, information, session, user) -> {
        return execute(
          services,
          strings,
          database,
          branding,
          template,
          session,
          user,
          request,
          information
        );
      };

    final var authenticated =
      withAuthentication(services, main);
    final var maintenanceAware =
      withMaintenanceAwareness(services, authenticated);

    return withInstrumentation(services, USER, maintenanceAware);
  }

  private static IdHTTPResponseType execute(
    final RPServiceDirectoryType services,
    final IdStrings strings,
    final IdDatabaseType database,
    final IdServerBrandingServiceType branding,
    final IdFMTemplateType<IdFMMessageData> template,
    final IdSessionUser session,
    final IdUser user,
    final ServerRequest request,
    final IdHTTPRequestInformation information)
  {
    final var vindicator =
      Vindication.startWithExceptions(IdValidityException::new);
    final var emailParameter =
      vindicator.addRequiredParameter("email", IdEmail::new);

    try {
      vindicator.check(request.query().toMap());
    } catch (final IdValidityException e) {
      session.messageCurrentSet(
        new IdSessionMessage(
          information.requestId(),
          true,
          false,
          strings.format(ERROR),
          e.getMessage(),
          DESTINATION_ON_FAILURE
        )
      );
      return IdUVMessage.showMessage(information, session, branding, template);
    }

    try (var connection = database.openConnection(IDSTORE)) {
      try (var transaction = connection.openTransaction()) {
        final var context =
          new IdUCommandContext(
            services,
            transaction,
            session,
            user,
            information.remoteAddress(),
            information.userAgent()
          );

        final var email =
          emailParameter.get();
        final var command =
          new IdUCommandEmailRemoveBegin(information.requestId(), email);
        new IdUCmdEmailRemoveBegin()
          .execute(context, command);

        transaction.commit();

        session.messageCurrentSet(
          new IdSessionMessage(
            information.requestId(),
            false,
            false,
            strings.format(EMAIL_VERIFICATION_TITLE),
            strings.format(EMAIL_VERIFICATION_SENT, email),
            DESTINATION_ON_SUCCESS
          )
        );
      }
    } catch (final IdDatabaseException e) {
      setSpanErrorCode(e.errorCode());
      session.messageCurrentSet(
        new IdSessionMessage(
          information.requestId(),
          true,
          true,
          strings.format(ERROR),
          e.getMessage(),
          DESTINATION_ON_FAILURE
        )
      );
    } catch (final IdCommandExecutionFailure e) {
      setSpanErrorCode(e.errorCode());
      session.messageCurrentSet(
        new IdSessionMessage(
          information.requestId(),
          true,
          e.httpStatusCode() >= 500,
          strings.format(ERROR),
          e.getMessage(),
          DESTINATION_ON_FAILURE
        )
      );
    }

    return IdUVMessage.showMessage(information, session, branding, template);
  }
}

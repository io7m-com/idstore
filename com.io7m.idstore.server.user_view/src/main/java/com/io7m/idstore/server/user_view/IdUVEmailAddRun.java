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
import com.io7m.idstore.protocol.user.IdUCommandEmailAddBegin;
import com.io7m.idstore.server.controller.IdServerStrings;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.controller.user.IdUCmdEmailAddBegin;
import com.io7m.idstore.server.controller.user.IdUCommandContext;
import com.io7m.idstore.server.http.IdHTTPServletFunctional;
import com.io7m.idstore.server.http.IdHTTPServletFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPServletRequestInformation;
import com.io7m.idstore.server.http.IdHTTPServletResponseType;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.sessions.IdSessionMessage;
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
import static com.io7m.idstore.server.user_view.IdUVServletCoreAuthenticated.withAuthentication;

/**
 * The page that executes the email addition.
 */

public final class IdUVEmailAddRun extends IdHTTPServletFunctional
{
  private static final String DESTINATION_ON_FAILURE = "/email-add";
  private static final String DESTINATION_ON_SUCCESS = "/";

  /**
   * The page that executes the email addition.
   *
   * @param services The services
   */

  public IdUVEmailAddRun(
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
    final var template =
      services.requireService(IdFMTemplateServiceType.class)
        .pageMessage();

    return withInstrumentation(services, (request, information) -> {
      return withAuthentication(
        services,
        (r0, info1, session, user) -> {
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
        }).execute(request, information);
    });
  }

  private static IdHTTPServletResponseType execute(
    final RPServiceDirectoryType services,
    final IdServerStrings strings,
    final IdDatabaseType database,
    final IdServerBrandingServiceType branding,
    final IdFMTemplateType<IdFMMessageData> template,
    final IdSessionUser session,
    final IdUser user,
    final HttpServletRequest request,
    final IdHTTPServletRequestInformation information)
  {
    final var vindicator =
      Vindication.startWithExceptions(IdValidityException::new);
    final var emailParameter =
      vindicator.addRequiredParameter("email", IdEmail::new);

    try {
      vindicator.check(request.getParameterMap());
    } catch (final IdValidityException e) {
      session.messageCurrentSet(
        new IdSessionMessage(
          information.requestId(),
          true,
          false,
          strings.format("error"),
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
            information.requestId(),
            transaction,
            session,
            user,
            information.remoteAddress(),
            information.userAgent()
          );

        final var email =
          emailParameter.get();
        final var command =
          new IdUCommandEmailAddBegin(email);
        new IdUCmdEmailAddBegin()
          .execute(context, command);

        transaction.commit();

        session.messageCurrentSet(
          new IdSessionMessage(
            information.requestId(),
            false,
            false,
            strings.format("emailVerificationTitle"),
            strings.format("emailVerificationSent", email),
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
          strings.format("error"),
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
          strings.format("error"),
          e.getMessage(),
          DESTINATION_ON_FAILURE
        )
      );
    }

    return IdUVMessage.showMessage(information, session, branding, template);
  }
}

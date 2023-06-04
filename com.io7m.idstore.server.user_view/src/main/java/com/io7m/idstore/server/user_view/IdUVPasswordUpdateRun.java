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
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.model.IdValidityException;
import com.io7m.idstore.protocol.user.IdUCommandPasswordUpdate;
import com.io7m.idstore.server.controller.IdServerStrings;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.controller.user.IdUCmdPasswordUpdate;
import com.io7m.idstore.server.controller.user.IdUCommandContext;
import com.io7m.idstore.server.http.IdHTTPServletFunctional;
import com.io7m.idstore.server.http.IdHTTPServletFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPServletRequestInformation;
import com.io7m.idstore.server.http.IdHTTPServletResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPServletResponseType;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.sessions.IdSessionMessage;
import com.io7m.idstore.server.service.sessions.IdSessionUser;
import com.io7m.idstore.server.service.templating.IdFMMessageData;
import com.io7m.idstore.server.service.templating.IdFMTemplateServiceType;
import com.io7m.idstore.server.service.templating.IdFMTemplateType;
import com.io7m.jvindicator.core.Vindication;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import freemarker.template.TemplateException;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.server.http.IdHTTPServletCoreInstrumented.withInstrumentation;
import static com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType.setSpanErrorCode;
import static com.io7m.idstore.server.user_view.IdUVServletCoreAuthenticated.withAuthentication;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The page that executes a password update.
 */

public final class IdUVPasswordUpdateRun extends IdHTTPServletFunctional
{
  private static final String DESTINATION_ON_FAILURE = "/password-update";
  private static final String DESTINATION_ON_SUCCESS = "/";

  /**
   * The page that executes a password update.
   *
   * @param services The services
   */

  public IdUVPasswordUpdateRun(
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
        (req0, info0, session, user) -> {
          return execute(
            services,
            database,
            strings,
            branding,
            template,
            session,
            user,
            req0,
            info0
          );
        }).execute(request, information);
    });
  }

  private static IdHTTPServletResponseType execute(
    final RPServiceDirectoryType services,
    final IdDatabaseType database,
    final IdServerStrings strings,
    final IdServerBrandingServiceType branding,
    final IdFMTemplateType<IdFMMessageData> template,
    final IdSessionUser session,
    final IdUser user,
    final HttpServletRequest request,
    final IdHTTPServletRequestInformation information)
  {
    final var vindicator =
      Vindication.startWithExceptions(IdValidityException::new);
    final var password0Parameter =
      vindicator.addRequiredParameter("password0", Vindication.strings());
    final var password1Parameter =
      vindicator.addRequiredParameter("password1", Vindication.strings());

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

        final var command =
          new IdUCommandPasswordUpdate(
            password0Parameter.get(),
            password1Parameter.get()
          );
        new IdUCmdPasswordUpdate()
          .execute(context, command);

        transaction.commit();
        return showConfirmed(strings, branding, template, information);
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

  private static IdHTTPServletResponseType showConfirmed(
    final IdServerStrings strings,
    final IdServerBrandingServiceType branding,
    final IdFMTemplateType<IdFMMessageData> template,
    final IdHTTPServletRequestInformation information)
  {
    try (var writer = new StringWriter()) {
      template.process(
        new IdFMMessageData(
          branding.htmlTitle(strings.format("passwordUpdateSuccessTitle")),
          branding.title(),
          information.requestId(),
          false,
          false,
          strings.format("passwordUpdateSuccessTitle"),
          strings.format("passwordUpdateSuccess"),
          DESTINATION_ON_SUCCESS
        ),
        writer
      );

      return new IdHTTPServletResponseFixedSize(
        200,
        IdUVContentTypes.xhtml(),
        writer.toString().getBytes(UTF_8)
      );
    } catch (final TemplateException e) {
      throw new IllegalStateException(e);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}

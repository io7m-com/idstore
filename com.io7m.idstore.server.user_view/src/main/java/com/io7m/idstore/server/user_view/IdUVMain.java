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
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.model.IdLogin;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.server.http.IdHTTPServletFunctional;
import com.io7m.idstore.server.http.IdHTTPServletFunctionalCoreAuthenticatedType;
import com.io7m.idstore.server.http.IdHTTPServletFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPServletRequestInformation;
import com.io7m.idstore.server.http.IdHTTPServletResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPServletResponseType;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.sessions.IdSessionUser;
import com.io7m.idstore.server.service.templating.IdFMMessageData;
import com.io7m.idstore.server.service.templating.IdFMTemplateServiceType;
import com.io7m.idstore.server.service.templating.IdFMTemplateType;
import com.io7m.idstore.server.service.templating.IdFMUserSelfData;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import freemarker.template.TemplateException;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.model.IdUserDomain.USER;
import static com.io7m.idstore.server.http.IdHTTPServletCoreInstrumented.withInstrumentation;
import static com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType.setSpanErrorCode;
import static com.io7m.idstore.server.user_view.IdUVServletCoreAuthenticated.withAuthentication;
import static com.io7m.idstore.server.user_view.IdUVServletCoreMaintenanceAware.withMaintenanceAwareness;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The main user profile page.
 */

public final class IdUVMain extends IdHTTPServletFunctional
{
  /**
   * The main user profile page.
   *
   * @param services The services
   */

  public IdUVMain(
    final RPServiceDirectoryType services)
  {
    super(createCore(services));
  }

  private static IdHTTPServletFunctionalCoreType createCore(
    final RPServiceDirectoryType services)
  {
    final var database =
      services.requireService(IdDatabaseType.class);
    final var branding =
      services.requireService(IdServerBrandingServiceType.class);
    final var templates =
      services.requireService(IdFMTemplateServiceType.class);
    final var userTemplate =
      templates.pageUserSelfTemplate();
    final var msgTemplate =
      templates.pageMessage();

    final IdHTTPServletFunctionalCoreAuthenticatedType<IdSessionUser, IdUser> main =
      (request, information, session, user) -> {
        return execute(
          database,
          branding,
          userTemplate,
          msgTemplate,
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

  private static IdHTTPServletResponseType execute(
    final IdDatabaseType database,
    final IdServerBrandingServiceType branding,
    final IdFMTemplateType<IdFMUserSelfData> userTemplate,
    final IdFMTemplateType<IdFMMessageData> msgTemplate,
    final IdSessionUser session,
    final IdUser user,
    final HttpServletRequest request,
    final IdHTTPServletRequestInformation information)
  {
    final var httpSession =
      request.getSession(true);

    try (var writer = new StringWriter()) {
      userTemplate.process(
        new IdFMUserSelfData(
          branding.htmlTitle("User Profile"),
          branding.title(),
          user,
          loginHistory(database, session.userId())
        ),
        writer
      );

      return new IdHTTPServletResponseFixedSize(
        200,
        IdUVContentTypes.xhtml(),
        writer.toString().getBytes(UTF_8)
      );
    } catch (final IdDatabaseException e) {
      setSpanErrorCode(e.errorCode());
      httpSession.setAttribute("ErrorMessage", e.getMessage());
      return IdUVMessage.showMessage(
        information,
        session,
        branding,
        msgTemplate);
    } catch (final TemplateException e) {
      throw new IllegalStateException(e);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static List<IdLogin> loginHistory(
    final IdDatabaseType database,
    final UUID userId)
    throws IdDatabaseException
  {
    try (var c = database.openConnection(IDSTORE)) {
      try (var t = c.openTransaction()) {
        final var u = t.queries(IdDatabaseUsersQueriesType.class);
        return u.userLoginHistory(userId, 30);
      }
    }
  }
}

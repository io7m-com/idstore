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
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.server.controller.IdServerStrings;
import com.io7m.idstore.server.http.IdHTTPServletFunctionalCoreAuthenticatedType;
import com.io7m.idstore.server.http.IdHTTPServletFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPServletRequestInformation;
import com.io7m.idstore.server.http.IdHTTPServletResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPServletResponseType;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.sessions.IdSessionSecretIdentifier;
import com.io7m.idstore.server.service.sessions.IdSessionUser;
import com.io7m.idstore.server.service.sessions.IdSessionUserService;
import com.io7m.idstore.server.service.templating.IdFMLoginData;
import com.io7m.idstore.server.service.templating.IdFMMessageData;
import com.io7m.idstore.server.service.templating.IdFMTemplateServiceType;
import com.io7m.idstore.server.service.templating.IdFMTemplateType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import freemarker.template.TemplateException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A core that executes the given core under authentication.
 */

public final class IdUVServletCoreAuthenticated
  implements IdHTTPServletFunctionalCoreType
{
  private final IdHTTPServletFunctionalCoreAuthenticatedType<IdSessionUser, IdUser> core;
  private final IdDatabaseType database;
  private final IdSessionUserService userSessions;
  private final IdServerStrings strings;
  private final IdFMTemplateType<IdFMMessageData> template;
  private final IdServerBrandingServiceType branding;
  private final IdFMTemplateType<IdFMLoginData> loginTemplate;
  private IdUVServletCoreAuthenticated(
    final RPServiceDirectoryType services,
    final IdHTTPServletFunctionalCoreAuthenticatedType<IdSessionUser, IdUser> inCore)
  {
    Objects.requireNonNull(services, "services");

    this.core =
      Objects.requireNonNull(inCore, "core");
    this.strings =
      services.requireService(IdServerStrings.class);
    this.database =
      services.requireService(IdDatabaseType.class);
    this.userSessions =
      services.requireService(IdSessionUserService.class);
    this.branding =
      services.requireService(IdServerBrandingServiceType.class);

    final var templates =
      services.requireService(IdFMTemplateServiceType.class);
    this.template =
      templates.pageMessage();
    this.loginTemplate =
      templates.pageLoginTemplate();
  }

  /**
   * @param services The services
   * @param inCore   The executed core
   *
   * @return A core that executes the given core under authentication
   */

  public static IdHTTPServletFunctionalCoreType withAuthentication(
    final RPServiceDirectoryType services,
    final IdHTTPServletFunctionalCoreAuthenticatedType<IdSessionUser, IdUser> inCore)
  {
    return new IdUVServletCoreAuthenticated(services, inCore);
  }

  @Override
  public IdHTTPServletResponseType execute(
    final HttpServletRequest request,
    final IdHTTPServletRequestInformation information)
  {
    final var httpSession =
      request.getSession(true);
    final var userSessionId =
      (IdSessionSecretIdentifier) httpSession.getAttribute("ID");

    if (userSessionId == null) {
      return this.notAuthenticated(httpSession);
    }

    final var userSessionOpt =
      this.userSessions.findSession(userSessionId);

    if (userSessionOpt.isEmpty()) {
      return this.notAuthenticated(httpSession);
    }

    final var userSession =
      userSessionOpt.get();

    final Optional<IdUser> userOpt;
    try {
      userOpt = this.userGet(userSession.userId());
    } catch (final IdDatabaseException e) {
      return this.errorOf(information, e);
    }

    if (userOpt.isEmpty()) {
      return this.notAuthenticated(httpSession);
    }

    return this.core.executeAuthenticated(
      request,
      information,
      userSession,
      userOpt.get()
    );
  }

  private IdHTTPServletResponseType errorOf(
    final IdHTTPServletRequestInformation information,
    final IdDatabaseException e)
  {
    try (var writer = new StringWriter()) {
      this.template.process(
        new IdFMMessageData(
          this.branding.htmlTitle(this.strings.format("error")),
          this.branding.title(),
          information.requestId(),
          true,
          true,
          this.strings.format("error"),
          e.getMessage(),
          "/"
        ),
        writer
      );
      return new IdHTTPServletResponseFixedSize(
        500,
        "application/xhtml+xml",
        writer.toString().getBytes(UTF_8)
      );
    } catch (final IOException ex) {
      throw new UncheckedIOException(ex);
    } catch (final TemplateException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private IdHTTPServletResponseType notAuthenticated(
    final HttpSession session)
  {
    return IdUVLogin.showLoginForm(
      this.branding,
      this.loginTemplate,
      session,
      401
    );
  }

  private Optional<IdUser> userGet(
    final UUID id)
    throws IdDatabaseException
  {
    try (var c = this.database.openConnection(IDSTORE)) {
      try (var t = c.openTransaction()) {
        final var q =
          t.queries(IdDatabaseUsersQueriesType.class);
        return q.userGet(id);
      }
    }
  }
}

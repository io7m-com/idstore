/*
 * Copyright © 2022 Mark Raynsford <code@io7m.com> https://www.io7m.com
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
import com.io7m.idstore.server.http.IdCommonInstrumentedServlet;
import com.io7m.idstore.server.http.IdRequestUniqueIDs;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.sessions.IdSessionSecretIdentifier;
import com.io7m.idstore.server.service.sessions.IdSessionUser;
import com.io7m.idstore.server.service.sessions.IdSessionUserService;
import com.io7m.idstore.server.service.templating.IdFMMessageData;
import com.io7m.idstore.server.service.templating.IdFMTemplateServiceType;
import com.io7m.idstore.server.service.templating.IdFMTemplateType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import freemarker.template.TemplateException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;

/**
 * A servlet that checks that a user is authenticated before delegating
 * execution to a subclass.
 */

public abstract class IdUViewAuthenticatedServlet
  extends IdCommonInstrumentedServlet
{
  private final IdServerStrings strings;
  private final IdDatabaseType database;
  private final RPServiceDirectoryType services;
  private final IdSessionUserService userSessions;
  private final IdFMTemplateType<IdFMMessageData> template;
  private final IdServerBrandingServiceType branding;
  private IdUser user;
  private IdSessionUser userSession;

  /**
   * A servlet that checks that a user is authenticated before delegating
   * execution to a subclass.
   *
   * @param inServices The service directory
   */

  protected IdUViewAuthenticatedServlet(
    final RPServiceDirectoryType inServices)
  {
    super(Objects.requireNonNull(inServices, "inServices"));

    this.services =
      inServices;
    this.strings =
      inServices.requireService(IdServerStrings.class);
    this.database =
      inServices.requireService(IdDatabaseType.class);
    this.userSessions =
      inServices.requireService(IdSessionUserService.class);
    this.branding =
      inServices.requireService(IdServerBrandingServiceType.class);
    this.template =
      inServices.requireService(IdFMTemplateServiceType.class)
        .pageMessage();
  }

  protected final IdSessionUser userSession()
  {
    return this.userSession;
  }

  protected final IdUser user()
  {
    return this.user;
  }

  protected final UUID userId()
  {
    return this.user().id();
  }

  protected final IdServerStrings strings()
  {
    return this.strings;
  }

  protected abstract void serviceAuthenticated(
    HttpServletRequest request,
    HttpServletResponse servletResponse,
    IdSessionUser session)
    throws Exception;

  @Override
  protected final void service(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse)
    throws ServletException, IOException
  {
    try {
      final var httpSession =
        request.getSession(true);
      final var userSessionId =
        (IdSessionSecretIdentifier) httpSession.getAttribute("ID");

      if (userSessionId != null) {
        final var userSessionOpt =
          this.userSessions.findSession(userSessionId);

        if (userSessionOpt.isPresent()) {
          this.userSession = userSessionOpt.get();
          this.user = this.userGet(this.userSession.userId());
          this.serviceAuthenticated(request, servletResponse, this.userSession);
          return;
        }
      }

      servletResponse.setStatus(401);
      new IdUViewLogin(this.services).service(request, servletResponse);
    } catch (final Exception e) {
      this.showError(request, servletResponse, e.getMessage(), true);
    }
  }

  private void showError(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse,
    final String message,
    final boolean isServerError)
    throws IOException
  {
    try (var writer = servletResponse.getWriter()) {
      if (isServerError) {
        servletResponse.setStatus(500);
      } else {
        servletResponse.setStatus(400);
      }

      this.template.process(
        new IdFMMessageData(
          this.branding.htmlTitle(this.strings.format("error")),
          this.branding.title(),
          IdRequestUniqueIDs.requestIdFor(request),
          true,
          isServerError,
          this.strings.format("error"),
          message,
          "/"
        ),
        writer
      );
    } catch (final TemplateException e) {
      throw new IOException(e);
    }
  }

  private IdUser userGet(final UUID id)
    throws IdDatabaseException
  {
    try (var c = this.database.openConnection(IDSTORE)) {
      try (var t = c.openTransaction()) {
        final var q =
          t.queries(IdDatabaseUsersQueriesType.class);
        return q.userGetRequire(id);
      }
    }
  }

  protected final IdDatabaseType database()
  {
    return this.database;
  }

  protected final RPServiceDirectoryType services()
  {
    return this.services;
  }
}

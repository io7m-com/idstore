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

package com.io7m.idstore.server.internal.user_view;

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.server.internal.IdServerBrandingService;
import com.io7m.idstore.server.internal.IdServerClock;
import com.io7m.idstore.server.internal.IdServerStrings;
import com.io7m.idstore.server.internal.IdUserSession;
import com.io7m.idstore.server.internal.IdUserSessionService;
import com.io7m.idstore.server.internal.common.IdCommonInstrumentedServlet;
import com.io7m.idstore.server.internal.freemarker.IdFMMessageData;
import com.io7m.idstore.server.internal.freemarker.IdFMTemplateService;
import com.io7m.idstore.server.internal.freemarker.IdFMTemplateType;
import com.io7m.idstore.services.api.IdServiceDirectoryType;
import freemarker.template.TemplateException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.server.internal.IdServerRequestDecoration.requestIdFor;
import static com.io7m.idstore.server.logging.IdServerMDCRequestProcessor.mdcForRequest;

/**
 * A servlet that checks that a user is authenticated before delegating
 * execution to a subclass.
 */

public abstract class IdUViewAuthenticatedServlet
  extends IdCommonInstrumentedServlet
{
  private final IdServerClock clock;
  private final IdServerStrings strings;
  private final IdDatabaseType database;
  private final IdServiceDirectoryType services;
  private final IdUserSessionService userControllers;
  private final IdFMTemplateType<IdFMMessageData> template;
  private final IdServerBrandingService branding;
  private IdUser user;
  private IdUserSession userController;

  /**
   * A servlet that checks that a user is authenticated before delegating
   * execution to a subclass.
   *
   * @param inServices The service directory
   */

  protected IdUViewAuthenticatedServlet(
    final IdServiceDirectoryType inServices)
  {
    super(Objects.requireNonNull(inServices, "inServices"));

    this.services =
      inServices;
    this.strings =
      inServices.requireService(IdServerStrings.class);
    this.clock =
      inServices.requireService(IdServerClock.class);
    this.database =
      inServices.requireService(IdDatabaseType.class);
    this.userControllers =
      inServices.requireService(IdUserSessionService.class);
    this.branding =
      inServices.requireService(IdServerBrandingService.class);
    this.template =
      inServices.requireService(IdFMTemplateService.class)
        .pageMessage();
  }

  protected final IdUserSession userController()
  {
    return this.userController;
  }

  protected final IdUser user()
  {
    return this.user;
  }

  protected final UUID userId()
  {
    return this.user().id();
  }

  protected final IdServerClock clock()
  {
    return this.clock;
  }

  protected final IdServerStrings strings()
  {
    return this.strings;
  }

  protected abstract Logger logger();

  protected abstract void serviceAuthenticated(
    HttpServletRequest request,
    HttpServletResponse servletResponse,
    HttpSession session)
    throws Exception;

  @Override
  protected final void service(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse)
    throws ServletException, IOException
  {
    try (var ignored0 = mdcForRequest((Request) request)) {
      try {
        final var session = request.getSession(true);
        final var userId = (UUID) session.getAttribute("UserID");
        if (userId != null) {
          this.user = this.userGet(userId);
          this.userController =
            this.userControllers.createOrGet(userId, session.getId());
          this.serviceAuthenticated(request, servletResponse, session);
          return;
        }

        servletResponse.setStatus(401);
        new IdUViewLogin(this.services).service(request, servletResponse);
      } catch (final Exception e) {
        this.showError(request, servletResponse, e.getMessage(), true);
      }
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
          requestIdFor(request),
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

  protected final IdServiceDirectoryType services()
  {
    return this.services;
  }
}

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
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.protocol.user_v1.IdU1Messages;
import com.io7m.idstore.server.internal.IdHTTPErrorStatusException;
import com.io7m.idstore.server.internal.IdServerClock;
import com.io7m.idstore.server.internal.IdServerStrings;
import com.io7m.idstore.server.internal.IdServerUserController;
import com.io7m.idstore.server.internal.IdServerUserControllersService;
import com.io7m.idstore.server.internal.user_v1.IdU1Sends;
import com.io7m.idstore.services.api.IdServiceDirectoryType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PASSWORD_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SQL_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.USER_NONEXISTENT;
import static com.io7m.idstore.server.internal.IdServerRequestDecoration.requestIdFor;
import static com.io7m.idstore.server.logging.IdServerMDCRequestProcessor.mdcForRequest;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;

/**
 * A servlet that checks that a user is authenticated before delegating
 * execution to a subclass.
 */

public abstract class IdUViewAuthenticatedServlet extends HttpServlet
{
  private final IdU1Sends sends;
  private final IdServerClock clock;
  private final IdServerStrings strings;
  private final IdU1Messages messages;
  private final IdDatabaseType database;
  private final IdServiceDirectoryType services;
  private final IdServerUserControllersService userControllers;
  private IdUser user;
  private IdServerUserController userController;

  /**
   * A servlet that checks that a user is authenticated before delegating
   * execution to a subclass.
   *
   * @param inServices The service directory
   */

  protected IdUViewAuthenticatedServlet(
    final IdServiceDirectoryType inServices)
  {
    this.services =
      Objects.requireNonNull(inServices, "services");

    this.messages =
      inServices.requireService(IdU1Messages.class);
    this.strings =
      inServices.requireService(IdServerStrings.class);
    this.clock =
      inServices.requireService(IdServerClock.class);
    this.sends =
      inServices.requireService(IdU1Sends.class);
    this.database =
      inServices.requireService(IdDatabaseType.class);
    this.userControllers =
      inServices.requireService(IdServerUserControllersService.class);
  }

  protected final IdServerUserController userController()
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

  protected final IdU1Sends sends()
  {
    return this.sends;
  }

  protected final IdServerClock clock()
  {
    return this.clock;
  }

  protected final IdServerStrings strings()
  {
    return this.strings;
  }

  protected final IdU1Messages messages()
  {
    return this.messages;
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
      } catch (final IdHTTPErrorStatusException e) {
        this.sends.sendError(
          servletResponse,
          requestIdFor(request),
          e.statusCode(),
          e.errorCode(),
          e.getMessage()
        );
      } catch (final IdPasswordException e) {
        this.logger().debug("password: ", e);
        this.sends.sendError(
          servletResponse,
          requestIdFor(request),
          INTERNAL_SERVER_ERROR_500,
          PASSWORD_ERROR,
          e.getMessage()
        );
      } catch (final IdDatabaseException e) {
        this.logger().debug("database: ", e);

        if (Objects.equals(e.errorCode(), USER_NONEXISTENT)) {
          final var session = request.getSession(false);
          if (session != null) {
            session.invalidate();
          }
        }

        this.sends.sendError(
          servletResponse,
          requestIdFor(request),
          INTERNAL_SERVER_ERROR_500,
          SQL_ERROR,
          e.getMessage()
        );
      } catch (final Exception e) {
        this.logger().trace("exception: ", e);
        throw new IOException(e);
      }
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

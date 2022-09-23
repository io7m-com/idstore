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

package com.io7m.idstore.server.internal.user_v1;

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.protocol.user.cb1.IdUCB1Messages;
import com.io7m.idstore.server.internal.IdHTTPErrorStatusException;
import com.io7m.idstore.server.internal.IdServerClock;
import com.io7m.idstore.server.internal.IdServerStrings;
import com.io7m.idstore.server.internal.common.IdCommonInstrumentedServlet;
import com.io7m.idstore.services.api.IdServiceDirectoryType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.AUTHENTICATION_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PASSWORD_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SQL_ERROR;
import static com.io7m.idstore.server.internal.IdServerRequestDecoration.requestIdFor;
import static com.io7m.idstore.server.logging.IdServerMDCRequestProcessor.mdcForRequest;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;

/**
 * A servlet that checks that a user is authenticated before delegating
 * execution to a subclass.
 */

public abstract class IdU1AuthenticatedServlet
  extends IdCommonInstrumentedServlet
{
  private final IdUCB1Sends sends;
  private final IdServerClock clock;
  private final IdServerStrings strings;
  private final IdUCB1Messages messages;
  private final IdDatabaseType database;
  private IdUser user;

  /**
   * A servlet that checks that a user is authenticated before delegating
   * execution to a subclass.
   *
   * @param services The service directory
   */

  protected IdU1AuthenticatedServlet(
    final IdServiceDirectoryType services)
  {
    super(Objects.requireNonNull(services, "services"));

    this.messages =
      services.requireService(IdUCB1Messages.class);
    this.strings =
      services.requireService(IdServerStrings.class);
    this.clock =
      services.requireService(IdServerClock.class);
    this.sends =
      services.requireService(IdUCB1Sends.class);
    this.database =
      services.requireService(IdDatabaseType.class);
  }

  /**
   * @return The authenticated user
   */

  protected final IdUser user()
  {
    return this.user;
  }

  protected final IdUCB1Sends sends()
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

  protected final IdUCB1Messages messages()
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
        final var session = request.getSession(false);
        if (session != null) {
          final var userId = (UUID) session.getAttribute("UserID");
          this.user = this.userGet(userId);
          this.serviceAuthenticated(request, servletResponse, session);
          return;
        }

        this.logger().debug("unauthenticated!");
        this.sends.sendError(
          servletResponse,
          requestIdFor(request),
          HttpStatus.UNAUTHORIZED_401,
          AUTHENTICATION_ERROR,
          this.strings.format("unauthorized")
        );
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
}

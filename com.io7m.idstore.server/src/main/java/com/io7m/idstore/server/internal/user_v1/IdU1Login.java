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
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.user.IdUCommandLogin;
import com.io7m.idstore.protocol.user.IdUResponseLogin;
import com.io7m.idstore.protocol.user.cb1.IdUCB1Messages;
import com.io7m.idstore.server.internal.IdHTTPErrorStatusException;
import com.io7m.idstore.server.internal.IdRequestLimits;
import com.io7m.idstore.server.internal.IdRequests;
import com.io7m.idstore.server.internal.IdServerClock;
import com.io7m.idstore.server.internal.IdServerConfigurationService;
import com.io7m.idstore.server.internal.IdServerStrings;
import com.io7m.idstore.services.api.IdServiceDirectoryType;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.AUTHENTICATION_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.BANNED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.HTTP_METHOD_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PASSWORD_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SQL_ERROR;
import static com.io7m.idstore.server.internal.IdServerRequestDecoration.requestIdFor;
import static com.io7m.idstore.server.logging.IdServerMDCRequestProcessor.mdcForRequest;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.FORBIDDEN_403;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;
import static org.eclipse.jetty.http.HttpStatus.METHOD_NOT_ALLOWED_405;
import static org.eclipse.jetty.http.HttpStatus.UNAUTHORIZED_401;

/**
 * A servlet that handles user logins.
 */

public final class IdU1Login extends HttpServlet
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdU1Login.class);

  private final IdDatabaseType database;
  private final IdUCB1Messages messages;
  private final IdServerStrings strings;
  private final IdServerClock clock;
  private final IdUCB1Sends errors;
  private final IdRequestLimits limits;
  private final IdServerConfigurationService configuration;

  /**
   * A servlet that handles user logins.
   *
   * @param inServices The service directory
   */

  public IdU1Login(
    final IdServiceDirectoryType inServices)
  {
    Objects.requireNonNull(inServices, "inServices");

    this.database =
      inServices.requireService(IdDatabaseType.class);
    this.messages =
      inServices.requireService(IdUCB1Messages.class);
    this.strings =
      inServices.requireService(IdServerStrings.class);
    this.clock =
      inServices.requireService(IdServerClock.class);
    this.errors =
      inServices.requireService(IdUCB1Sends.class);
    this.limits =
      inServices.requireService(IdRequestLimits.class);
    this.configuration =
      inServices.requireService(IdServerConfigurationService.class);
  }

  @Override
  protected void service(
    final HttpServletRequest request,
    final HttpServletResponse response)
    throws IOException
  {
    try (var ignored0 = mdcForRequest(request)) {
      try {
        if (!Objects.equals(request.getMethod(), "POST")) {
          throw new IdHTTPErrorStatusException(
            METHOD_NOT_ALLOWED_405,
            HTTP_METHOD_ERROR,
            this.strings.format("methodNotAllowed")
          );
        }

        final var login =
          this.readLoginCommand(request);

        try (var connection = this.database.openConnection(IDSTORE)) {
          try (var transaction = connection.openTransaction()) {
            final var users =
              transaction.queries(IdDatabaseUsersQueriesType.class);
            this.tryLogin(request, response, users, login);
            transaction.commit();
          }
        }

      } catch (final IdHTTPErrorStatusException e) {
        this.errors.sendError(
          response,
          requestIdFor(request),
          e.statusCode(),
          e.errorCode(),
          e.getMessage()
        );
      } catch (final IdPasswordException e) {
        LOG.debug("password: ", e);
        this.errors.sendError(
          response,
          requestIdFor(request),
          INTERNAL_SERVER_ERROR_500,
          PASSWORD_ERROR,
          e.getMessage()
        );
      } catch (final IdDatabaseException e) {
        LOG.debug("database: ", e);
        this.errors.sendError(
          response,
          requestIdFor(request),
          INTERNAL_SERVER_ERROR_500,
          SQL_ERROR,
          e.getMessage()
        );
      }
    }
  }

  private void checkBan(
    final IdDatabaseUsersQueriesType users,
    final IdUser user)
    throws IdDatabaseException, IdHTTPErrorStatusException
  {
    final var banOpt =
      users.userBanGet(user.id());

    /*
     * If there's no ban, allow the login.
     */

    if (banOpt.isEmpty()) {
      return;
    }

    final var ban = banOpt.get();
    final var expiresOpt = ban.expires();

    /*
     * If there's no expiration on the ban, deny the login.
     */

    if (expiresOpt.isEmpty()) {
      throw new IdHTTPErrorStatusException(
        FORBIDDEN_403,
        BANNED,
        this.strings.format("bannedNoExpire", ban.reason())
      );
    }

    /*
     * If the current time is before the expiration date, deny the login.
     */

    final var timeExpires = expiresOpt.get();
    final var timeNow = this.clock.now();

    if (timeNow.compareTo(timeExpires) < 0) {
      throw new IdHTTPErrorStatusException(
        FORBIDDEN_403,
        BANNED,
        this.strings.format("banned", ban.reason(), timeExpires)
      );
    }
  }

  private void tryLogin(
    final HttpServletRequest request,
    final HttpServletResponse response,
    final IdDatabaseUsersQueriesType users,
    final IdUCommandLogin login)
    throws
    IdHTTPErrorStatusException,
    IdDatabaseException,
    IdPasswordException, IOException
  {
    final var userOpt =
      users.userGetForName(login.userName());

    if (userOpt.isEmpty()) {
      throw new IdHTTPErrorStatusException(
        UNAUTHORIZED_401,
        AUTHENTICATION_ERROR,
        this.strings.format("loginFailed")
      );
    }

    final var user = userOpt.get();
    this.checkBan(users, user);

    final var ok =
      user.password().check(login.password());

    if (!ok) {
      throw new IdHTTPErrorStatusException(
        UNAUTHORIZED_401,
        AUTHENTICATION_ERROR,
        this.strings.format("loginFailed")
      );
    }

    LOG.info("user '{}' logged in", login.userName());
    final var session = request.getSession();
    session.setAttribute("UserID", user.id());
    response.setStatus(200);

    users.userLogin(
      user.id(),
      IdRequests.requestUserAgent(request),
      request.getRemoteAddr(),
      this.configuration.configuration()
        .history()
        .userLoginHistoryLimit()
    );

    this.sendLoginResponse(request, response, user);
  }

  private void sendLoginResponse(
    final HttpServletRequest request,
    final HttpServletResponse response,
    final IdUser user)
    throws IOException
  {
    response.setStatus(200);
    response.setContentType(IdUCB1Messages.contentType());

    try {
      final var data =
        this.messages.serialize(new IdUResponseLogin(requestIdFor(request)));
      response.setContentLength(data.length);
      try (var output = response.getOutputStream()) {
        output.write(data);
      }
    } catch (final IdProtocolException e) {
      throw new IOException(e);
    }
  }

  private IdUCommandLogin readLoginCommand(
    final HttpServletRequest request)
    throws IdHTTPErrorStatusException, IOException
  {
    try (var input = this.limits.boundedMaximumInput(request, 1024)) {
      final var data = input.readAllBytes();
      final var message = this.messages.parse(data);
      if (message instanceof IdUCommandLogin login) {
        return login;
      }
    } catch (final IdProtocolException e) {
      throw new IdHTTPErrorStatusException(
        BAD_REQUEST_400,
        PROTOCOL_ERROR,
        e.getMessage(),
        e
      );
    }

    throw new IdHTTPErrorStatusException(
      BAD_REQUEST_400,
      PROTOCOL_ERROR,
      this.strings.format("expectedCommand", "CommandLogin")
    );
  }
}

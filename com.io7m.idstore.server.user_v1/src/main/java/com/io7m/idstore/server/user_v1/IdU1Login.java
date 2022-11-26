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


package com.io7m.idstore.server.user_v1;

import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.error_codes.IdException;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.user.IdUCommandLogin;
import com.io7m.idstore.protocol.user.IdUResponseLogin;
import com.io7m.idstore.protocol.user.cb.IdUCB1Messages;
import com.io7m.idstore.server.controller.IdServerStrings;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.controller.user.IdUserLoginService;
import com.io7m.idstore.server.http.IdCommonInstrumentedServlet;
import com.io7m.idstore.server.http.IdHTTPErrorStatusException;
import com.io7m.idstore.server.http.IdRequestUniqueIDs;
import com.io7m.idstore.server.service.reqlimit.IdRequestLimitExceeded;
import com.io7m.idstore.server.service.reqlimit.IdRequestLimits;
import com.io7m.idstore.services.api.IdServiceDirectoryType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Objects;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.HTTP_METHOD_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.IO_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;
import static org.eclipse.jetty.http.HttpStatus.METHOD_NOT_ALLOWED_405;
import static org.eclipse.jetty.http.HttpStatus.PAYLOAD_TOO_LARGE_413;

/**
 * A servlet that handles user logins.
 */

public final class IdU1Login extends IdCommonInstrumentedServlet
{
  private final IdDatabaseType database;
  private final IdUCB1Messages messages;
  private final IdServerStrings strings;
  private final IdUCB1Sends errors;
  private final IdRequestLimits limits;
  private final IdUserLoginService logins;

  /**
   * A servlet that handles user logins.
   *
   * @param inServices The service directory
   */

  public IdU1Login(
    final IdServiceDirectoryType inServices)
  {
    super(Objects.requireNonNull(inServices, "services"));

    this.database =
      inServices.requireService(IdDatabaseType.class);
    this.messages =
      inServices.requireService(IdUCB1Messages.class);
    this.strings =
      inServices.requireService(IdServerStrings.class);
    this.errors =
      inServices.requireService(IdUCB1Sends.class);
    this.limits =
      inServices.requireService(IdRequestLimits.class);
    this.logins =
      inServices.requireService(IdUserLoginService.class);
  }

  @Override
  protected void service(
    final HttpServletRequest request,
    final HttpServletResponse response)
    throws IOException
  {
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
          final var loggedIn =
            this.logins.userLogin(
              transaction,
              IdRequestUniqueIDs.requestIdFor(request),
              login.userName().value(),
              login.password(),
              login.metadata()
            );

          transaction.commit();

          this.sendLoginResponse(
            request,
            response,
            loggedIn.user()
          );
        }
      }

    } catch (final IdCommandExecutionFailure e) {
      this.errors.sendError(
        response,
        IdRequestUniqueIDs.requestIdFor(request),
        e.httpStatusCode(),
        e.errorCode(),
        e.getMessage()
      );
    } catch (final IdHTTPErrorStatusException e) {
      this.errors.sendError(
        response,
        IdRequestUniqueIDs.requestIdFor(request),
        e.statusCode(),
        e.errorCode(),
        e.getMessage()
      );
    } catch (final IdException e) {
      this.errors.sendError(
        response,
        IdRequestUniqueIDs.requestIdFor(request),
        INTERNAL_SERVER_ERROR_500,
        e.errorCode(),
        e.getMessage()
      );
    } catch (final Exception e) {
      this.errors.sendError(
        response,
        IdRequestUniqueIDs.requestIdFor(request),
        INTERNAL_SERVER_ERROR_500,
        IO_ERROR,
        e.getMessage()
      );
    }
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
        this.messages.serialize(
          new IdUResponseLogin(
            IdRequestUniqueIDs.requestIdFor(request),
            user.withRedactedPassword())
        );
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
    } catch (final IdRequestLimitExceeded e) {
      throw new IdHTTPErrorStatusException(
        PAYLOAD_TOO_LARGE_413,
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

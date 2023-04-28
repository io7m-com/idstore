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


package com.io7m.idstore.server.admin_v1;

import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.error_codes.IdException;
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.protocol.admin.IdACommandLogin;
import com.io7m.idstore.protocol.admin.IdAResponseLogin;
import com.io7m.idstore.protocol.admin.cb.IdACB1Messages;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.server.controller.IdServerStrings;
import com.io7m.idstore.server.controller.admin.IdAdminLoginService;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.http.IdCommonInstrumentedServlet;
import com.io7m.idstore.server.http.IdHTTPErrorStatusException;
import com.io7m.idstore.server.http.IdRequestUniqueIDs;
import com.io7m.idstore.server.service.reqlimit.IdRequestLimitExceeded;
import com.io7m.idstore.server.service.reqlimit.IdRequestLimits;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import com.io7m.seltzer.api.SStructuredError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.HTTP_METHOD_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.IO_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;
import static org.eclipse.jetty.http.HttpStatus.METHOD_NOT_ALLOWED_405;
import static org.eclipse.jetty.http.HttpStatus.PAYLOAD_TOO_LARGE_413;

/**
 * A servlet that handles admin logins.
 */

public final class IdA1Login extends IdCommonInstrumentedServlet
{
  private final IdDatabaseType database;
  private final IdACB1Messages messages;
  private final IdServerStrings strings;
  private final IdACB1Sends errors;
  private final IdRequestLimits limits;
  private final IdAdminLoginService logins;

  /**
   * A servlet that handles admin logins.
   *
   * @param inServices The service directory
   */

  public IdA1Login(
    final RPServiceDirectoryType inServices)
  {
    super(Objects.requireNonNull(inServices, "services"));

    this.database =
      inServices.requireService(IdDatabaseType.class);
    this.messages =
      inServices.requireService(IdACB1Messages.class);
    this.strings =
      inServices.requireService(IdServerStrings.class);
    this.errors =
      inServices.requireService(IdACB1Sends.class);
    this.limits =
      inServices.requireService(IdRequestLimits.class);
    this.logins =
      inServices.requireService(IdAdminLoginService.class);
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
          this.strings.format("methodNotAllowed"),
          HTTP_METHOD_ERROR,
          Map.of(),
          Optional.empty(),
          METHOD_NOT_ALLOWED_405
        );
      }

      final var login =
        this.readLoginCommand(request);

      try (var connection = this.database.openConnection(IDSTORE)) {
        try (var transaction = connection.openTransaction()) {
          final var loggedIn =
            this.logins.adminLogin(
              transaction,
              IdRequestUniqueIDs.requestIdFor(request),
              login.userName().value(),
              login.password(),
              Map.of()
            );

          transaction.commit();

          request.getSession(true)
            .setAttribute("ID", loggedIn.session().id());

          this.sendLoginResponse(
            request,
            response,
            loggedIn.admin()
          );
        }
      }

    } catch (final IdCommandExecutionFailure e) {
      this.errors.sendError(
        response,
        IdRequestUniqueIDs.requestIdFor(request),
        e.httpStatusCode(),
        e
      );
    } catch (final IdHTTPErrorStatusException e) {
      this.errors.sendError(
        response,
        IdRequestUniqueIDs.requestIdFor(request),
        e.httpStatusCode(),
        e
      );
    } catch (final IdException e) {
      this.errors.sendError(
        response,
        IdRequestUniqueIDs.requestIdFor(request),
        INTERNAL_SERVER_ERROR_500,
        e
      );
    } catch (final Exception e) {
      this.errors.sendError(
        response,
        IdRequestUniqueIDs.requestIdFor(request),
        INTERNAL_SERVER_ERROR_500,
        new SStructuredError<>(
          IO_ERROR,
          e.getMessage(),
          Map.of(),
          Optional.empty(),
          Optional.of(e)
        )
      );
    }
  }

  private void sendLoginResponse(
    final HttpServletRequest request,
    final HttpServletResponse response,
    final IdAdmin admin)
    throws IOException
  {
    response.setStatus(200);
    response.setContentType(IdACB1Messages.contentType());

    try {
      final var data =
        this.messages.serialize(
          new IdAResponseLogin(
            IdRequestUniqueIDs.requestIdFor(request),
            admin.withRedactedPassword())
        );
      response.setContentLength(data.length);
      try (var output = response.getOutputStream()) {
        output.write(data);
      }
    } catch (final IdProtocolException e) {
      throw new IOException(e);
    }
  }

  private IdACommandLogin readLoginCommand(
    final HttpServletRequest request)
    throws IdHTTPErrorStatusException, IOException
  {
    try (var input = this.limits.boundedMaximumInput(request, 1024)) {
      final var data = input.readAllBytes();
      final var message = this.messages.parse(data);
      if (message instanceof IdACommandLogin login) {
        return login;
      }
    } catch (final IdProtocolException e) {
      throw new IdHTTPErrorStatusException(
        e.getMessage(),
        e,
        PROTOCOL_ERROR,
        Map.of(),
        Optional.empty(),
        BAD_REQUEST_400
      );
    } catch (final IdRequestLimitExceeded e) {
      throw new IdHTTPErrorStatusException(
        e.getMessage(),
        e,
        PROTOCOL_ERROR,
        Map.of(),
        Optional.empty(),
        PAYLOAD_TOO_LARGE_413
      );
    }

    throw new IdHTTPErrorStatusException(
      this.strings.format("expectedCommand", "CommandLogin"),
      PROTOCOL_ERROR,
      Map.of(),
      Optional.empty(),
      BAD_REQUEST_400
    );
  }
}

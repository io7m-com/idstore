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

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseTransactionType;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.user.IdUCommandType;
import com.io7m.idstore.protocol.user.IdUResponseError;
import com.io7m.idstore.protocol.user.IdUResponseType;
import com.io7m.idstore.protocol.user.cb.IdUCB1Messages;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.controller.user.IdUCommandContext;
import com.io7m.idstore.server.controller.user.IdUCommandExecutor;
import com.io7m.idstore.server.http.IdHTTPErrorStatusException;
import com.io7m.idstore.server.http.IdRequestUniqueIDs;
import com.io7m.idstore.server.http.IdRequestUserAgents;
import com.io7m.idstore.server.service.reqlimit.IdRequestLimits;
import com.io7m.idstore.server.service.sessions.IdSessionUser;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import io.opentelemetry.api.trace.Span;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;

/**
 * A servlet for executing a single command.
 */

public final class IdU1CommandServlet extends IdU1AuthenticatedServlet
{
  private final IdDatabaseType database;
  private final IdRequestLimits limits;
  private final IdUCB1Messages messages;
  private final IdUCommandExecutor executor;
  private final RPServiceDirectoryType services;

  /**
   * A servlet for executing a single command.
   *
   * @param inServices The service directory
   */

  public IdU1CommandServlet(
    final RPServiceDirectoryType inServices)
  {
    super(inServices);

    this.services =
      Objects.requireNonNull(inServices, "inServices");
    this.database =
      inServices.requireService(IdDatabaseType.class);
    this.limits =
      inServices.requireService(IdRequestLimits.class);
    this.messages =
      inServices.requireService(IdUCB1Messages.class);
    this.executor =
      new IdUCommandExecutor();
  }

  @Override
  protected void serviceAuthenticated(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse,
    final IdSessionUser session)
    throws Exception
  {
    try (var input = this.limits.boundedMaximumInput(request, 1048576)) {
      final var data = input.readAllBytes();
      final var message = this.messages.parse(data);
      if (message instanceof IdUCommandType<?> command) {
        this.executeCommand(request, servletResponse, session, command);
        return;
      }
    } catch (final IdProtocolException e) {
      throw new IdHTTPErrorStatusException(
        e.getMessage(),
        e.errorCode(),
        e.attributes(),
        e.remediatingAction(),
        BAD_REQUEST_400
      );
    }

    throw new IdHTTPErrorStatusException(
      this.strings().format("expectedCommand", "IdU1CommandType"),
      PROTOCOL_ERROR,
      Map.of(),
      Optional.empty(),
      BAD_REQUEST_400
    );
  }

  private void executeCommand(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse,
    final IdSessionUser session,
    final IdUCommandType<?> command)
    throws IdDatabaseException, IOException, InterruptedException
  {
    try (var connection = this.database.openConnection(IDSTORE)) {
      try (var transaction = connection.openTransaction()) {
        this.executeCommandInTransaction(
          request,
          servletResponse,
          session,
          command,
          transaction
        );
      }
    }
  }

  private void executeCommandInTransaction(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse,
    final IdSessionUser session,
    final IdUCommandType<?> command,
    final IdDatabaseTransactionType transaction)
    throws IOException, IdDatabaseException, InterruptedException
  {
    final var context =
      new IdUCommandContext(
        this.services,
        IdRequestUniqueIDs.requestIdFor(request),
        transaction,
        session,
        this.user(),
        request.getRemoteAddr(),
        IdRequestUserAgents.requestUserAgent(request)
      );

    final var sends =
      this.sends();

    try {
      final IdUResponseType result = this.executor.execute(context, command);
      sends.send(servletResponse, 200, result);
      if (result instanceof IdUResponseError error) {
        Span.current().setAttribute("idstore.errorCode", error.errorCode());
      } else {
        transaction.commit();
      }
    } catch (final IdCommandExecutionFailure e) {
      Span.current().setAttribute("idstore.errorCode", e.errorCode().id());
      sends.send(
        servletResponse,
        e.httpStatusCode(),
        new IdUResponseError(
          e.requestId(),
          e.errorCode().id(),
          e.getMessage(),
          e.attributes(),
          e.remediatingAction()
        ));
    }
  }
}

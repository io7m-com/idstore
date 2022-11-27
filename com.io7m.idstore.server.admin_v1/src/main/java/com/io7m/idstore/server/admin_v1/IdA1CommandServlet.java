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

package com.io7m.idstore.server.admin_v1;

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseTransactionType;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.protocol.admin.IdACommandType;
import com.io7m.idstore.protocol.admin.IdAResponseError;
import com.io7m.idstore.protocol.admin.IdAResponseType;
import com.io7m.idstore.protocol.admin.cb.IdACB1Messages;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.server.controller.admin.IdACommandContext;
import com.io7m.idstore.server.controller.admin.IdACommandExecutor;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.http.IdHTTPErrorStatusException;
import com.io7m.idstore.server.http.IdRequestUniqueIDs;
import com.io7m.idstore.server.service.reqlimit.IdRequestLimits;
import com.io7m.idstore.server.service.sessions.IdSessionAdmin;
import com.io7m.idstore.services.api.IdServiceDirectoryType;
import io.opentelemetry.api.trace.Span;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.IO_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;

/**
 * A servlet for executing a single command.
 */

public final class IdA1CommandServlet extends IdA1AuthenticatedServlet
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdA1CommandServlet.class);

  private final IdDatabaseType database;
  private final IdRequestLimits limits;
  private final IdACB1Messages messages;
  private final IdACommandExecutor executor;
  private final IdServiceDirectoryType services;

  /**
   * A servlet for executing a single command.
   *
   * @param inServices The service directory
   */

  public IdA1CommandServlet(
    final IdServiceDirectoryType inServices)
  {
    super(inServices);

    this.services =
      Objects.requireNonNull(inServices, "inServices");
    this.database =
      inServices.requireService(IdDatabaseType.class);
    this.limits =
      inServices.requireService(IdRequestLimits.class);
    this.messages =
      inServices.requireService(IdACB1Messages.class);
    this.executor =
      new IdACommandExecutor();
  }

  @Override
  protected void serviceAuthenticated(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse,
    final IdSessionAdmin session)
    throws Exception
  {
    final var requestId =
      IdRequestUniqueIDs.requestIdFor(request);

    try (var input = this.limits.boundedMaximumInput(request, 1048576)) {
      final var data = input.readAllBytes();
      final var message = this.messages.parse(data);
      if (message instanceof IdACommandType<?> command) {
        this.executeCommand(
          request,
          servletResponse,
          session,
          requestId,
          command);
        return;
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
      this.strings().format("expectedCommand", "IdA1CommandType")
    );
  }

  private void executeCommand(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse,
    final IdSessionAdmin session,
    final UUID requestId,
    final IdACommandType<?> command)
    throws IdDatabaseException, IOException
  {
    try (var connection = this.database.openConnection(IDSTORE)) {
      try (var transaction = connection.openTransaction()) {
        this.executeCommandInTransaction(
          request,
          servletResponse,
          session,
          requestId,
          command,
          transaction
        );
      }
    }
  }

  private void executeCommandInTransaction(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse,
    final IdSessionAdmin session,
    final UUID requestId,
    final IdACommandType<?> command,
    final IdDatabaseTransactionType transaction)
    throws IOException
  {
    final var admin =
      this.admin();

    final var agent =
      Optional.ofNullable(request.getHeader("User-Agent"))
        .orElse("<unavailable>");

    final var context =
      new IdACommandContext(
        this.services,
        requestId,
        transaction,
        session,
        request.getRemoteHost(),
        agent,
        admin
      );

    final var sends = this.sends();

    try {
      final IdAResponseType result = this.executor.execute(context, command);
      sends.send(servletResponse, 200, result);
      if (result instanceof IdAResponseError error) {
        Span.current().setAttribute("idstore.errorCode", error.errorCode());
      } else {
        transaction.commit();
      }
    } catch (final IdCommandExecutionFailure e) {
      sends.send(
        servletResponse,
        e.httpStatusCode(),
        new IdAResponseError(e.requestId(), e.errorCode().id(), e.getMessage())
      );
    } catch (final Exception e) {
      sends.send(
        servletResponse,
        500,
        new IdAResponseError(requestId, IO_ERROR.id(), e.getMessage())
      );
    }
  }
}

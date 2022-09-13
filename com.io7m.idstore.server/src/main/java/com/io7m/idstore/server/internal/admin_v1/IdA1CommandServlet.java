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

package com.io7m.idstore.server.internal.admin_v1;

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseTransactionType;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandType;
import com.io7m.idstore.protocol.admin_v1.IdA1Messages;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseError;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseType;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.server.internal.IdHTTPErrorStatusException;
import com.io7m.idstore.server.internal.IdRequestLimits;
import com.io7m.idstore.server.internal.IdUserSessionService;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.services.api.IdServiceDirectoryType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.IO_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.idstore.server.internal.IdServerRequestDecoration.requestIdFor;
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
  private final IdA1Messages messages;
  private final IdA1CommandExecutor executor;
  private final IdServiceDirectoryType services;
  private final IdUserSessionService sessions;

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
      inServices.requireService(IdA1Messages.class);
    this.sessions =
      inServices.requireService(IdUserSessionService.class);
    this.executor =
      new IdA1CommandExecutor();
  }

  @Override
  protected Logger logger()
  {
    return LOG;
  }

  @Override
  protected void serviceAuthenticated(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse,
    final HttpSession session)
    throws Exception
  {
    final var requestId =
      requestIdFor(request);

    try (var input = this.limits.boundedMaximumInput(request, 1048576)) {
      final var data = input.readAllBytes();
      final var message = this.messages.parse(data);
      if (message instanceof IdA1CommandType command) {
        this.executeCommand(request, servletResponse, session, requestId, command);
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
    final HttpSession session,
    final UUID requestId,
    final IdA1CommandType<?> command)
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
    final HttpSession session,
    final UUID requestId,
    final IdA1CommandType<?> command,
    final IdDatabaseTransactionType transaction)
    throws IOException, IdDatabaseException
  {
    final var admin =
      this.admin();
    final var userSession =
      this.sessions.createOrGet(admin.id(), session.getId());

    final var context =
      new IdA1CommandContext(
        this.services,
        this.strings(),
        requestId,
        transaction,
        this.clock(),
        admin,
        userSession,
        request.getRemoteHost(),
        Optional.ofNullable(request.getHeader("User-Agent"))
          .orElse("<unavailable>")
      );

    final var sends = this.sends();

    try {
      final IdA1ResponseType result = this.executor.execute(context, command);
      sends.send(servletResponse, 200, result);
      if (!(result instanceof IdA1ResponseError)) {
        transaction.commit();
      }
    } catch (final IdCommandExecutionFailure e) {
      sends.send(
        servletResponse,
        e.httpStatusCode(),
        new IdA1ResponseError(
          e.requestId(),
          e.errorCode().id(),
          e.getMessage()
        ));
    } catch (final Exception e) {
      sends.send(
        servletResponse,
        500,
        new IdA1ResponseError(
          requestId,
          IO_ERROR.id(),
          e.getMessage()
        ));
    }
  }
}

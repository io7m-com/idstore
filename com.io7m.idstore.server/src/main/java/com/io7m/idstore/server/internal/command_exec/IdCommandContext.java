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


package com.io7m.idstore.server.internal.command_exec;

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseTransactionType;
import com.io7m.idstore.error_codes.IdErrorCode;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdValidityException;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.api.IdProtocolMessageType;
import com.io7m.idstore.server.internal.IdServerClock;
import com.io7m.idstore.server.internal.IdServerStrings;
import com.io7m.idstore.server.internal.IdServerTelemetryService;
import com.io7m.idstore.server.security.IdSecActionType;
import com.io7m.idstore.server.security.IdSecPolicyResultDenied;
import com.io7m.idstore.server.security.IdSecurity;
import com.io7m.idstore.server.security.IdSecurityException;
import com.io7m.idstore.services.api.IdServiceDirectoryType;
import io.opentelemetry.api.trace.Tracer;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.HTTP_PARAMETER_INVALID;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.MAIL_SYSTEM_FAILURE;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PASSWORD_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SECURITY_POLICY_DENIED;
import static org.eclipse.jetty.http.HttpStatus.FORBIDDEN_403;

/**
 * The context for execution of a command (or set of commands in a
 * transaction).
 *
 * @param <E> The type of error messages
 * @param <S> The type of sessions
 */

public abstract class IdCommandContext<E extends IdProtocolMessageType, S>
{
  private final IdServiceDirectoryType services;
  private final UUID requestId;
  private final IdDatabaseTransactionType transaction;
  private final IdServerClock clock;
  private final IdServerStrings strings;
  private final S session;
  private final String remoteHost;
  private final String remoteUserAgent;
  private final Tracer tracer;

  /**
   * The context for execution of a command (or set of commands in a
   * transaction).
   *
   * @param inServices        The service directory
   * @param inStrings         The string resources
   * @param inRequestId       The request ID
   * @param inTransaction     The transaction
   * @param inClock           The clock
   * @param inSession         The user session
   * @param inRemoteHost      The remote host
   * @param inRemoteUserAgent The remote user agent
   */

  public IdCommandContext(
    final IdServiceDirectoryType inServices,
    final IdServerStrings inStrings,
    final UUID inRequestId,
    final IdDatabaseTransactionType inTransaction,
    final IdServerClock inClock,
    final S inSession,
    final String inRemoteHost,
    final String inRemoteUserAgent)
  {
    this.services =
      Objects.requireNonNull(inServices, "services");
    this.requestId =
      Objects.requireNonNull(inRequestId, "requestId");
    this.transaction =
      Objects.requireNonNull(inTransaction, "transaction");
    this.clock =
      Objects.requireNonNull(inClock, "clock");
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.session =
      Objects.requireNonNull(inSession, "inSession");
    this.remoteHost =
      Objects.requireNonNull(inRemoteHost, "remoteHost");
    this.remoteUserAgent =
      Objects.requireNonNull(inRemoteUserAgent, "remoteUserAgent");
    this.tracer =
      inServices.requireService(IdServerTelemetryService.class)
        .tracer();
  }

  /**
   * @return The user session
   */

  public final S session()
  {
    return this.session;
  }

  /**
   * @return The remote host
   */

  public final String remoteHost()
  {
    return this.remoteHost;
  }

  /**
   * @return The remote user agent
   */

  public final String remoteUserAgent()
  {
    return this.remoteUserAgent;
  }

  /**
   * @return The service directory used during execution
   */

  public final IdServiceDirectoryType services()
  {
    return this.services;
  }

  /**
   * @return The ID of the incoming request
   */

  public final UUID requestId()
  {
    return this.requestId;
  }

  /**
   * @return The database transaction
   */

  public final IdDatabaseTransactionType transaction()
  {
    return this.transaction;
  }

  /**
   * @return The OpenTelemetry tracer
   */

  public final Tracer tracer()
  {
    return this.tracer;
  }

  /**
   * @return The current time
   */

  public final OffsetDateTime now()
  {
    return this.clock.now();
  }

  /**
   * Produce an exception indicating an error, with a formatted error message.
   *
   * @param statusCode The HTTP status code
   * @param errorCode  The error code
   * @param messageId  The string resource message ID
   * @param args       The string resource format arguments
   *
   * @return An execution failure
   */

  public final IdCommandExecutionFailure failFormatted(
    final int statusCode,
    final IdErrorCode errorCode,
    final String messageId,
    final Object... args)
  {
    return this.fail(
      statusCode,
      errorCode,
      this.strings.format(messageId, args)
    );
  }

  /**
   * Produce an exception indicating an error, with a string constant message.
   *
   * @param statusCode The HTTP status code
   * @param errorCode  The error code
   * @param message    The string message
   *
   * @return An execution failure
   */

  public final IdCommandExecutionFailure fail(
    final int statusCode,
    final IdErrorCode errorCode,
    final String message)
  {
    return new IdCommandExecutionFailure(
      message,
      this.requestId,
      statusCode,
      errorCode
    );
  }

  protected abstract E error(
    UUID id,
    IdErrorCode errorCode,
    String message
  );

  /**
   * Produce an exception indicating a database error.
   *
   * @param e The database exception
   *
   * @return An execution failure
   */

  public final IdCommandExecutionFailure failDatabase(
    final IdDatabaseException e)
  {
    return new IdCommandExecutionFailure(
      e.getMessage(),
      e,
      this.requestId,
      500,
      e.errorCode()
    );
  }

  /**
   * Produce an exception indicating a security policy error.
   *
   * @param e The security exception
   *
   * @return An execution failure
   */

  public IdCommandExecutionFailure failSecurity(
    final IdSecurityException e)
  {
    return new IdCommandExecutionFailure(
      e.getMessage(),
      e,
      this.requestId,
      500,
      SECURITY_POLICY_DENIED
    );
  }

  /**
   * Produce an exception indicating a mail system error.
   *
   * @param email The email address
   * @param e     The mail system exception
   *
   * @return An execution failure
   */

  public final IdCommandExecutionFailure failMail(
    final IdEmail email,
    final Exception e)
  {
    return new IdCommandExecutionFailure(
      this.strings.format(
        "mailSystemFailure",
        email,
        e.getMessage()
      ),
      e,
      this.requestId,
      500,
      MAIL_SYSTEM_FAILURE
    );
  }

  /**
   * Produce an exception indicating a validation error.
   *
   * @param e The exception
   *
   * @return An execution failure
   */

  public IdCommandExecutionFailure failValidity(
    final IdValidityException e)
  {
    return new IdCommandExecutionFailure(
      e.getMessage(),
      e,
      this.requestId,
      400,
      HTTP_PARAMETER_INVALID
    );
  }

  /**
   * Produce an exception indicating a password format error.
   *
   * @param e The exception
   *
   * @return An execution failure
   */

  public IdCommandExecutionFailure failPassword(
    final IdPasswordException e)
  {
    return new IdCommandExecutionFailure(
      e.getMessage(),
      e,
      this.requestId,
      400,
      PASSWORD_ERROR
    );
  }

  /**
   * Produce an exception indicating a protocol error.
   *
   * @param e The exception
   *
   * @return An execution failure
   */

  public IdCommandExecutionFailure failProtocol(
    final IdProtocolException e)
  {
    return new IdCommandExecutionFailure(
      e.getMessage(),
      e,
      this.requestId,
      400,
      PROTOCOL_ERROR
    );
  }

  /**
   * Check the given action against the security policy.
   *
   * @param action The action
   *
   * @throws IdSecurityException       On errors
   * @throws IdCommandExecutionFailure On errors
   */

  public void securityCheck(
    final IdSecActionType action)
    throws IdSecurityException, IdCommandExecutionFailure
  {
    if (IdSecurity.check(action) instanceof IdSecPolicyResultDenied denied) {
      throw this.fail(
        FORBIDDEN_403,
        SECURITY_POLICY_DENIED,
        denied.message()
      );
    }
  }
}

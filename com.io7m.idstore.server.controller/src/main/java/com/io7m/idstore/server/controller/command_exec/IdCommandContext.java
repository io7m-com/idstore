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


package com.io7m.idstore.server.controller.command_exec;

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseTransactionType;
import com.io7m.idstore.error_codes.IdErrorCode;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdValidityException;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.api.IdProtocolMessageType;
import com.io7m.idstore.server.controller.IdServerStrings;
import com.io7m.idstore.server.security.IdSecActionType;
import com.io7m.idstore.server.security.IdSecPolicyResultDenied;
import com.io7m.idstore.server.security.IdSecurity;
import com.io7m.idstore.server.security.IdSecurityException;
import com.io7m.idstore.server.service.clock.IdServerClock;
import com.io7m.idstore.server.service.sessions.IdSessionType;
import com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import io.opentelemetry.api.trace.Tracer;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.HTTP_PARAMETER_INVALID;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.MAIL_SYSTEM_FAILURE;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SECURITY_POLICY_DENIED;

/**
 * The context for execution of a command (or set of commands in a
 * transaction).
 *
 * @param <E> The type of error messages
 * @param <S> The type of sessions
 */

public abstract class IdCommandContext<E extends IdProtocolMessageType, S extends IdSessionType>
{
  private final RPServiceDirectoryType services;
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
   * @param inRequestId       The request ID
   * @param inTransaction     The transaction
   * @param inSession         The user session
   * @param inRemoteHost      The remote host
   * @param inRemoteUserAgent The remote user agent
   */

  public IdCommandContext(
    final RPServiceDirectoryType inServices,
    final UUID inRequestId,
    final IdDatabaseTransactionType inTransaction,
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
    this.session =
      Objects.requireNonNull(inSession, "inSession");
    this.remoteHost =
      Objects.requireNonNull(inRemoteHost, "remoteHost");
    this.remoteUserAgent =
      Objects.requireNonNull(inRemoteUserAgent, "remoteUserAgent");

    this.clock =
      inServices.requireService(IdServerClock.class);
    this.strings =
      inServices.requireService(IdServerStrings.class);
    this.tracer =
      inServices.requireService(IdServerTelemetryServiceType.class)
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

  public final RPServiceDirectoryType services()
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
      e.errorCode(),
      e.attributes(),
      e.remediatingAction(),
      this.requestId,
      500
    );
  }

  /**
   * Produce an exception indicating a security policy error.
   *
   * @param e The security exception
   *
   * @return An execution failure
   */

  public final IdCommandExecutionFailure failSecurity(
    final IdSecurityException e)
  {
    return new IdCommandExecutionFailure(
      e.getMessage(),
      e,
      e.errorCode(),
      e.attributes(),
      e.remediatingAction(),
      this.requestId,
      500
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
      MAIL_SYSTEM_FAILURE,
      Map.of(),
      Optional.empty(),
      this.requestId,
      500
    );
  }

  /**
   * Produce an exception indicating a validation error.
   *
   * @param e The exception
   *
   * @return An execution failure
   */

  public final IdCommandExecutionFailure failValidity(
    final IdValidityException e)
  {
    return new IdCommandExecutionFailure(
      e.getMessage(),
      e,
      HTTP_PARAMETER_INVALID,
      Map.of(),
      Optional.empty(),
      this.requestId,
      400
    );
  }

  /**
   * Produce an exception indicating a password format error.
   *
   * @param e The exception
   *
   * @return An execution failure
   */

  public final IdCommandExecutionFailure failPassword(
    final IdPasswordException e)
  {
    return new IdCommandExecutionFailure(
      e.getMessage(),
      e,
      e.errorCode(),
      e.attributes(),
      e.remediatingAction(),
      this.requestId,
      400
    );
  }

  /**
   * Produce an exception indicating a protocol error.
   *
   * @param e The exception
   *
   * @return An execution failure
   */

  public final IdCommandExecutionFailure failProtocol(
    final IdProtocolException e)
  {
    return new IdCommandExecutionFailure(
      e.getMessage(),
      e,
      e.errorCode(),
      e.attributes(),
      e.remediatingAction(),
      this.requestId,
      400
    );
  }

  /**
   * Check the given action against the security policy.
   *
   * @param action The action
   *
   * @throws IdSecurityException On errors
   */

  public final void securityCheck(
    final IdSecActionType action)
    throws IdSecurityException
  {
    if (IdSecurity.check(action) instanceof final IdSecPolicyResultDenied denied) {
      throw new IdSecurityException(
        denied.message(),
        SECURITY_POLICY_DENIED,
        Map.of(),
        Optional.empty()
      );
    }
  }

  /**
   * Fail with a constant error message.
   *
   * @param statusCode The status code
   * @param errorCode  The error code
   * @param text       The message
   *
   * @return An exception
   */

  public final IdCommandExecutionFailure fail(
    final int statusCode,
    final IdErrorCode errorCode,
    final String text)
  {
    return new IdCommandExecutionFailure(
      text,
      errorCode,
      Map.of(),
      Optional.empty(),
      this.requestId,
      statusCode
    );
  }

  /**
   * Fail with a constant error message.
   *
   * @param statusCode The status code
   * @param errorCode  The error code
   * @param text       The message
   * @param args       The format arguments
   *
   * @return An exception
   */

  public final IdCommandExecutionFailure failFormatted(
    final int statusCode,
    final IdErrorCode errorCode,
    final String text,
    final Object... args)
  {
    return new IdCommandExecutionFailure(
      this.strings.format(text, args),
      errorCode,
      Map.of(),
      Optional.empty(),
      this.requestId,
      statusCode
    );
  }
}

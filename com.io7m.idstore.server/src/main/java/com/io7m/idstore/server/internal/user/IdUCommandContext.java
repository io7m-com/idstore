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


package com.io7m.idstore.server.internal.user;

import com.io7m.idstore.database.api.IdDatabaseTransactionType;
import com.io7m.idstore.error_codes.IdErrorCode;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.protocol.user.IdUResponseError;
import com.io7m.idstore.protocol.user.IdUResponseType;
import com.io7m.idstore.server.internal.IdServerClock;
import com.io7m.idstore.server.internal.IdServerStrings;
import com.io7m.idstore.server.internal.IdUserSession;
import com.io7m.idstore.server.internal.IdUserSessionService;
import com.io7m.idstore.server.internal.command_exec.IdCommandContext;
import com.io7m.idstore.services.api.IdServiceDirectoryType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.Objects;
import java.util.UUID;

import static com.io7m.idstore.server.internal.IdRequests.requestUserAgent;
import static com.io7m.idstore.server.internal.IdServerRequestDecoration.requestIdFor;

/**
 * The command context for user API commands.
 */

public final class IdUCommandContext extends IdCommandContext<IdUResponseType>
{
  private final IdUser user;

  /**
   * @return The user executing the command.
   */

  public IdUser user()
  {
    return this.user;
  }

  /**
   * The context for execution of a command (or set of commands in a
   * transaction).
   *
   * @param inServices      The service directory
   * @param inStrings       The string resources
   * @param inRequestId     The request ID
   * @param inTransaction   The transaction
   * @param inClock         The clock
   * @param inUser          The user executing the command
   * @param inSession       The user session
   * @param remoteHost      The remote host
   * @param remoteUserAgent The remote user agent
   */

  public IdUCommandContext(
    final IdServiceDirectoryType inServices,
    final IdServerStrings inStrings,
    final UUID inRequestId,
    final IdDatabaseTransactionType inTransaction,
    final IdServerClock inClock,
    final IdUser inUser,
    final IdUserSession inSession,
    final String remoteHost,
    final String remoteUserAgent)
  {
    super(
      inServices,
      inStrings,
      inRequestId,
      inTransaction,
      inClock,
      inSession,
      remoteHost,
      remoteUserAgent
    );
    this.user = Objects.requireNonNull(inUser, "inUser");
  }

  /**
   * Create a new command context from the given objects.
   *
   * @param services    The service directory
   * @param transaction The database transaction
   * @param request     The request
   * @param session     The HTTP session
   * @param user        The current user
   *
   * @return A context
   */

  public static IdUCommandContext create(
    final IdServiceDirectoryType services,
    final IdDatabaseTransactionType transaction,
    final HttpServletRequest request,
    final HttpSession session,
    final IdUser user)
  {
    return new IdUCommandContext(
      services,
      services.requireService(IdServerStrings.class),
      requestIdFor(request),
      transaction,
      services.requireService(IdServerClock.class),
      user,
      services.requireService(IdUserSessionService.class)
        .createOrGet(user.id(), session.getId()),
      request.getRemoteHost(),
      requestUserAgent(request)
    );
  }

  @Override
  protected IdUResponseError error(
    final UUID id,
    final IdErrorCode errorCode,
    final String message)
  {
    return new IdUResponseError(id, errorCode.id(), message);
  }
}

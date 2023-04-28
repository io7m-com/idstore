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


package com.io7m.idstore.server.controller.user;

import com.io7m.idstore.database.api.IdDatabaseTransactionType;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.protocol.user.IdUResponseType;
import com.io7m.idstore.server.controller.command_exec.IdCommandContext;
import com.io7m.idstore.server.service.sessions.IdSessionUser;
import com.io7m.repetoir.core.RPServiceDirectoryType;

import java.util.Objects;
import java.util.UUID;

/**
 * The command context for user API commands.
 */

public final class IdUCommandContext
  extends IdCommandContext<IdUResponseType, IdSessionUser>
{
  private final IdUser user;

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
   * @param inUser            The user
   */

  public IdUCommandContext(
    final RPServiceDirectoryType inServices,
    final UUID inRequestId,
    final IdDatabaseTransactionType inTransaction,
    final IdSessionUser inSession,
    final IdUser inUser,
    final String inRemoteHost,
    final String inRemoteUserAgent)
  {
    super(
      inServices,
      inRequestId,
      inTransaction,
      inSession,
      inRemoteHost,
      inRemoteUserAgent
    );
    this.user = Objects.requireNonNull(inUser, "user");
  }

  /**
   * @return The user executing the command.
   */

  public IdUser user()
  {
    return this.user;
  }
}

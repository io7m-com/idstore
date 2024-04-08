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

package com.io7m.idstore.server.controller.admin;

import com.io7m.idstore.database.api.IdDatabaseTransactionType;
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.protocol.admin.IdAResponseType;
import com.io7m.idstore.server.controller.command_exec.IdCommandContext;
import com.io7m.idstore.server.service.sessions.IdSessionAdmin;
import com.io7m.repetoir.core.RPServiceDirectoryType;

import java.util.Objects;

/**
 * The command context for admin API commands.
 */

public final class IdACommandContext
  extends IdCommandContext<IdAResponseType, IdSessionAdmin>
{
  private final IdAdmin admin;

  /**
   * The context for execution of a command (or set of commands in a
   * transaction).
   *
   * @param inServices        The service directory
   * @param inTransaction     The transaction
   * @param inSession         The user session
   * @param inRemoteHost      The remote remoteHost
   * @param inRemoteUserAgent The remote user agent
   * @param inAdmin           The admin
   */

  public IdACommandContext(
    final RPServiceDirectoryType inServices,
    final IdDatabaseTransactionType inTransaction,
    final IdSessionAdmin inSession,
    final String inRemoteHost,
    final String inRemoteUserAgent,
    final IdAdmin inAdmin)
  {
    super(
      inServices,
      inTransaction,
      inSession,
      inRemoteHost,
      inRemoteUserAgent);

    this.admin =
      Objects.requireNonNull(inAdmin, "inAdmin");
  }

  /**
   * @return The admin executing the command.
   */

  public IdAdmin admin()
  {
    return this.admin;
  }
}

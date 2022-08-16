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

import com.io7m.idstore.database.api.IdDatabaseTransactionType;
import com.io7m.idstore.error_codes.IdErrorCode;
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseError;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseType;
import com.io7m.idstore.server.internal.IdServerClock;
import com.io7m.idstore.server.internal.IdServerStrings;
import com.io7m.idstore.server.internal.command_exec.IdCommandContext;
import com.io7m.idstore.services.api.IdServiceDirectoryType;

import java.util.Objects;
import java.util.UUID;

/**
 * The command context for public API commands.
 */

public final class IdA1CommandContext extends IdCommandContext<IdA1ResponseType>
{
  private final IdAdmin admin;

  /**
   * @return The admin executing the command.
   */

  public IdAdmin admin()
  {
    return this.admin;
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
   * @param inAdmin         The admin executing the command
   * @param remoteHost      The remote host
   * @param remoteUserAgent The remote user agent
   */

  public IdA1CommandContext(
    final IdServiceDirectoryType inServices,
    final IdServerStrings inStrings,
    final UUID inRequestId,
    final IdDatabaseTransactionType inTransaction,
    final IdServerClock inClock,
    final IdAdmin inAdmin,
    final String remoteHost,
    final String remoteUserAgent)
  {
    super(
      inServices,
      inStrings,
      inRequestId,
      inTransaction,
      inClock,
      remoteHost,
      remoteUserAgent
    );
    this.admin = Objects.requireNonNull(inAdmin, "inAdmin");
  }

  @Override
  protected IdA1ResponseError error(
    final UUID id,
    final IdErrorCode errorCode,
    final String message)
  {
    return new IdA1ResponseError(id, errorCode.id(), message);
  }
}

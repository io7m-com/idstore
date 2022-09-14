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

import com.io7m.idstore.database.api.IdDatabaseAdminsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdValidityException;
import com.io7m.idstore.protocol.admin_v1.IdA1Ban;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminBanGet;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminBanGet;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseType;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.security.IdSecAdminActionAdminBanGet;
import com.io7m.idstore.server.security.IdSecurityException;

/**
 * IdA1CmdAdminBanGet
 */

public final class IdA1CmdAdminBanGet
  extends IdA1CmdAbstract<
  IdA1CommandContext, IdA1CommandAdminBanGet, IdA1ResponseType>
{
  /**
   * IdA1CmdAdminBanGet
   */

  public IdA1CmdAdminBanGet()
  {

  }

  @Override
  protected IdA1ResponseType executeActual(
    final IdA1CommandContext context,
    final IdA1CommandAdminBanGet command)
    throws
    IdValidityException,
    IdSecurityException,
    IdDatabaseException,
    IdPasswordException,
    IdCommandExecutionFailure,
    IdProtocolException
  {
    final var transaction =
      context.transaction();
    final var admin =
      context.admin();

    context.securityCheck(new IdSecAdminActionAdminBanGet(admin));

    transaction.adminIdSet(admin.id());

    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);

    return new IdA1ResponseAdminBanGet(
      context.requestId(),
      admins.adminBanGet(command.admin()).map(IdA1Ban::ofBan)
    );
  }
}

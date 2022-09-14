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
import com.io7m.idstore.model.IdBan;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdValidityException;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminBanDelete;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminBanDelete;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseType;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.security.IdSecAdminActionAdminBanDelete;
import com.io7m.idstore.server.security.IdSecurityException;

import java.util.Optional;

/**
 * IdA1CmdAdminBanDelete
 */

public final class IdA1CmdAdminBanDelete
  extends IdA1CmdAbstract<
  IdA1CommandContext, IdA1CommandAdminBanDelete, IdA1ResponseType>
{
  /**
   * IdA1CmdAdminBanDelete
   */

  public IdA1CmdAdminBanDelete()
  {

  }

  @Override
  protected IdA1ResponseType executeActual(
    final IdA1CommandContext context,
    final IdA1CommandAdminBanDelete command)
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

    context.securityCheck(new IdSecAdminActionAdminBanDelete(admin));

    transaction.adminIdSet(admin.id());

    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);

    admins.adminBanDelete(new IdBan(command.admin(), "", Optional.empty()));

    return new IdA1ResponseAdminBanDelete(
      context.requestId()
    );
  }
}

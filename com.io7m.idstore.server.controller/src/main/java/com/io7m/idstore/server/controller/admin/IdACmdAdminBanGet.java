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

import com.io7m.idstore.database.api.IdDatabaseAdminsQueriesType;
import com.io7m.idstore.error_codes.IdException;
import com.io7m.idstore.protocol.admin.IdACommandAdminBanGet;
import com.io7m.idstore.protocol.admin.IdAResponseAdminBanGet;
import com.io7m.idstore.protocol.admin.IdAResponseType;
import com.io7m.idstore.server.security.IdSecAdminActionAdminBanGet;

import java.util.UUID;

/**
 * IdACmdAdminBanGet
 */

public final class IdACmdAdminBanGet
  extends IdACmdAbstract<
  IdACommandContext, IdACommandAdminBanGet, IdAResponseType>
{
  /**
   * IdACmdAdminBanGet
   */

  public IdACmdAdminBanGet()
  {

  }

  @Override
  protected IdAResponseType executeActual(
    final IdACommandContext context,
    final IdACommandAdminBanGet command)
    throws IdException
  {
    final var transaction =
      context.transaction();
    final var admin =
      context.admin();

    context.securityCheck(new IdSecAdminActionAdminBanGet(admin));

    transaction.adminIdSet(admin.id());

    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);

    return new IdAResponseAdminBanGet(
      UUID.randomUUID(),
      command.messageId(),
      admins.adminBanGet(command.admin())
    );
  }
}

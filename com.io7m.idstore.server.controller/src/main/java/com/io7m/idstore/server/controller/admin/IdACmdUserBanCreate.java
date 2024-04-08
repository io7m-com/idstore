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

import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.error_codes.IdException;
import com.io7m.idstore.protocol.admin.IdACommandUserBanCreate;
import com.io7m.idstore.protocol.admin.IdAResponseType;
import com.io7m.idstore.protocol.admin.IdAResponseUserBanCreate;
import com.io7m.idstore.server.security.IdSecAdminActionUserBanCreate;

import java.util.UUID;

/**
 * IdACmdUserBanCreate
 */

public final class IdACmdUserBanCreate
  extends IdACmdAbstract<
  IdACommandContext, IdACommandUserBanCreate, IdAResponseType>
{
  /**
   * IdACmdUserBanCreate
   */

  public IdACmdUserBanCreate()
  {

  }

  @Override
  protected IdAResponseType executeActual(
    final IdACommandContext context,
    final IdACommandUserBanCreate command)
    throws IdException
  {
    final var transaction =
      context.transaction();
    final var admin =
      context.admin();

    final var ban = command.ban();
    context.securityCheck(new IdSecAdminActionUserBanCreate(admin));

    transaction.adminIdSet(admin.id());

    final var users =
      transaction.queries(IdDatabaseUsersQueriesType.class);

    users.userBanCreate(ban);

    return new IdAResponseUserBanCreate(
      UUID.randomUUID(),
      command.messageId(),
      ban
    );
  }
}

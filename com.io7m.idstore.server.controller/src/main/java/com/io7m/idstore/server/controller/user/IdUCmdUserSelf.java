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


package com.io7m.idstore.server.controller.user;

import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.error_codes.IdException;
import com.io7m.idstore.model.IdValidityException;
import com.io7m.idstore.protocol.user.IdUCommandUserSelf;
import com.io7m.idstore.protocol.user.IdUResponseType;
import com.io7m.idstore.protocol.user.IdUResponseUserSelf;

/**
 * IdUCmdUserSelf
 */

public final class IdUCmdUserSelf
  extends IdUCmdAbstract<IdUCommandUserSelf>
{
  /**
   * IdUCmdUserSelf
   */

  public IdUCmdUserSelf()
  {

  }

  @Override
  protected IdUResponseType executeActual(
    final IdUCommandContext context,
    final IdUCommandUserSelf command)
    throws IdValidityException, IdException
  {
    final var transaction =
      context.transaction();
    final var users =
      transaction.queries(IdDatabaseUsersQueriesType.class);

    final var userId = context.user().id();
    transaction.userIdSet(userId);
    final var user = users.userGetRequire(userId);
    return new IdUResponseUserSelf(context.requestId(), user);
  }
}

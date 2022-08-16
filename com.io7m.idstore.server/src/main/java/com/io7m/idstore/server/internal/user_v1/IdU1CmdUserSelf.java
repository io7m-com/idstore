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


package com.io7m.idstore.server.internal.user_v1;

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.protocol.user_v1.IdU1CommandUserSelf;
import com.io7m.idstore.protocol.user_v1.IdU1ResponseType;
import com.io7m.idstore.protocol.user_v1.IdU1ResponseUserSelf;
import com.io7m.idstore.protocol.user_v1.IdU1User;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutorType;

import java.util.Objects;

/**
 * IdU1CmdUserSelf
 */

public final class IdU1CmdUserSelf
  implements IdCommandExecutorType<
  IdU1CommandContext, IdU1CommandUserSelf, IdU1ResponseType>
{
  /**
   * IdU1CmdUserSelf
   */

  public IdU1CmdUserSelf()
  {

  }

  @Override
  public IdU1ResponseType execute(
    final IdU1CommandContext context,
    final IdU1CommandUserSelf command)
    throws IdCommandExecutionFailure
  {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(command, "command");

    try {
      final var transaction =
        context.transaction();
      final var users =
        transaction.queries(IdDatabaseUsersQueriesType.class);

      final var userId = context.user().id();
      transaction.userIdSet(userId);
      final var user = users.userGetRequire(userId);

      return new IdU1ResponseUserSelf(context.requestId(), IdU1User.ofUser(user));

    } catch (final IdDatabaseException e) {
      throw context.failDatabase(e);
    }
  }
}

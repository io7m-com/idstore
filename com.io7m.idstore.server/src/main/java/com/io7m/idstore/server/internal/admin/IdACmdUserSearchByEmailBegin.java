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

package com.io7m.idstore.server.internal.admin;

import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.error_codes.IdException;
import com.io7m.idstore.model.IdUserSearchByEmailParameters;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchByEmailBegin;
import com.io7m.idstore.protocol.admin.IdAResponseType;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchByEmailBegin;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.security.IdSecAdminActionUserRead;

/**
 * IdACmdUserSearchByEmailBegin
 */

public final class IdACmdUserSearchByEmailBegin
  extends IdACmdAbstract<
  IdACommandContext, IdACommandUserSearchByEmailBegin, IdAResponseType>
{
  /**
   * IdACmdUserSearchByEmailBegin
   */

  public IdACmdUserSearchByEmailBegin()
  {

  }

  @Override
  protected IdAResponseType executeActual(
    final IdACommandContext context,
    final IdACommandUserSearchByEmailBegin command)
    throws IdException, IdCommandExecutionFailure
  {
    final var transaction =
      context.transaction();
    final var admin =
      context.admin();

    context.securityCheck(new IdSecAdminActionUserRead(admin));

    final var users =
      transaction.queries(IdDatabaseUsersQueriesType.class);
    final var search =
      users.userSearchByEmail(obtainListParameters(command));

    final var session = context.session();
    session.setUserSearchByEmail(search);

    return new IdAResponseUserSearchByEmailBegin(
      context.requestId(),
      search.pageCurrent(users)
    );
  }

  private static IdUserSearchByEmailParameters obtainListParameters(
    final IdACommandUserSearchByEmailBegin command)
  {
    final var model = command.parameters();
    if (model.limit() > 1000) {
      return new IdUserSearchByEmailParameters(
        model.timeCreatedRange(),
        model.timeUpdatedRange(),
        model.search(),
        model.ordering(),
        1000
      );
    }
    return model;
  }
}

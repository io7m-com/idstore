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

import com.io7m.idstore.database.api.IdDatabaseAdminsQueriesType;
import com.io7m.idstore.error_codes.IdException;
import com.io7m.idstore.model.IdAdminSearchParameters;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchBegin;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSearchBegin;
import com.io7m.idstore.protocol.admin.IdAResponseType;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.security.IdSecAdminActionAdminRead;

/**
 * IdACmdAdminSearchBegin
 */

public final class IdACmdAdminSearchBegin
  extends IdACmdAbstract<
  IdACommandContext, IdACommandAdminSearchBegin, IdAResponseType>
{
  /**
   * IdACmdAdminSearchBegin
   */

  public IdACmdAdminSearchBegin()
  {

  }

  @Override
  protected IdAResponseType executeActual(
    final IdACommandContext context,
    final IdACommandAdminSearchBegin command)
    throws IdException, IdCommandExecutionFailure
  {
    final var transaction =
      context.transaction();
    final var admin =
      context.admin();

    context.securityCheck(new IdSecAdminActionAdminRead(admin));

    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);
    final var search =
      admins.adminSearch(obtainListParameters(command));

    final var session = context.session();
    session.setAdminSearch(search);

    return new IdAResponseAdminSearchBegin(
      context.requestId(),
      search.pageCurrent(admins)
    );
  }

  private static IdAdminSearchParameters obtainListParameters(
    final IdACommandAdminSearchBegin command)
  {
    final var model = command.parameters();
    if (model.limit() > 1000) {
      return new IdAdminSearchParameters(
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

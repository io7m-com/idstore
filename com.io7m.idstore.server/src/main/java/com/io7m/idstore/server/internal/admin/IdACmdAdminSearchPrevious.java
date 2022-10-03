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
import com.io7m.idstore.model.IdPage;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchPrevious;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSearchPrevious;
import com.io7m.idstore.protocol.admin.IdAResponseType;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.security.IdSecAdminActionAdminRead;

/**
 * IdACmdAdminSearchPrevious
 */

public final class IdACmdAdminSearchPrevious
  extends IdACmdAbstract<
  IdACommandContext, IdACommandAdminSearchPrevious, IdAResponseType>
{
  /**
   * IdACmdAdminSearchPrevious
   */

  public IdACmdAdminSearchPrevious()
  {

  }

  @Override
  protected IdAResponseType executeActual(
    final IdACommandContext context,
    final IdACommandAdminSearchPrevious command)
    throws IdException, IdCommandExecutionFailure
  {
    final var transaction =
      context.transaction();
    final var admin =
      context.admin();

    context.securityCheck(new IdSecAdminActionAdminRead(admin));

    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);

    final var session = context.userSession();
    final var paging = session.adminPaging();
    final var data = paging.pagePrevious(admins);

    return new IdAResponseAdminSearchPrevious(
      context.requestId(),
      new IdPage<>(
        data,
        paging.pageNumber(),
        paging.pageCount(),
        paging.pageFirstOffset()
      )
    );
  }
}

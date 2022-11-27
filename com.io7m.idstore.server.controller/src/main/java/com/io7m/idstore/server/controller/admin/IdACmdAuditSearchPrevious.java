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


package com.io7m.idstore.server.controller.admin;

import com.io7m.idstore.database.api.IdDatabaseAuditQueriesType;
import com.io7m.idstore.error_codes.IdException;
import com.io7m.idstore.protocol.admin.IdACommandAuditSearchPrevious;
import com.io7m.idstore.protocol.admin.IdAResponseAuditSearchPrevious;
import com.io7m.idstore.protocol.admin.IdAResponseType;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.security.IdSecAdminActionAuditRead;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;

/**
 * IdACmdAuditSearchPrevious
 */

public final class IdACmdAuditSearchPrevious
  extends IdACmdAbstract<
  IdACommandContext, IdACommandAuditSearchPrevious, IdAResponseType>
{
  /**
   * IdACmdAuditSearchPrevious
   */

  public IdACmdAuditSearchPrevious()
  {

  }

  @Override
  protected IdAResponseType executeActual(
    final IdACommandContext context,
    final IdACommandAuditSearchPrevious command)
    throws IdException, IdCommandExecutionFailure
  {
    final var transaction =
      context.transaction();
    final var admin =
      context.admin();

    context.securityCheck(new IdSecAdminActionAuditRead(admin));

    final var audit =
      transaction.queries(IdDatabaseAuditQueriesType.class);
    final var session =
      context.session();
    final var searchOpt =
      session.auditSearch();

    if (searchOpt.isEmpty()) {
      throw context.failFormatted(
        400, PROTOCOL_ERROR, "errorSearchStart");
    }

    final var search = searchOpt.get();
    final var page = search.pagePrevious(audit);
    return new IdAResponseAuditSearchPrevious(context.requestId(), page);
  }
}

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

import com.io7m.idstore.database.api.IdDatabaseAuditQueriesType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.model.IdAuditSearchParameters;
import com.io7m.idstore.model.IdValidityException;
import com.io7m.idstore.protocol.admin_v1.IdA1AuditEvent;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAuditSearchBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1Page;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAuditSearchBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseType;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.security.IdSecAdminActionAuditRead;
import com.io7m.idstore.server.security.IdSecurityException;

/**
 * IdA1CmdAuditSearchBegin
 */

public final class IdA1CmdAuditSearchBegin
  extends IdA1CmdAbstract<
  IdA1CommandContext, IdA1CommandAuditSearchBegin, IdA1ResponseType>
{
  /**
   * IdA1CmdAuditSearchBegin
   */

  public IdA1CmdAuditSearchBegin()
  {

  }

  @Override
  protected IdA1ResponseType executeActual(
    final IdA1CommandContext context,
    final IdA1CommandAuditSearchBegin command)
    throws IdCommandExecutionFailure, IdSecurityException, IdDatabaseException
  {
    final var transaction =
      context.transaction();
    final var admin =
      context.admin();

    context.securityCheck(new IdSecAdminActionAuditRead(admin));

    final var audit =
      transaction.queries(IdDatabaseAuditQueriesType.class);

    final var session = context.userSession();
    session.setAuditListParameters(obtainListParameters(command));
    final var paging = session.auditPaging();
    final var data = paging.pageCurrent(audit);

    return new IdA1ResponseAuditSearchBegin(
      context.requestId(),
      new IdA1Page<>(
        data.stream()
          .map(IdA1AuditEvent::of)
          .toList(),
        paging.pageNumber(),
        paging.pageCount(),
        paging.pageFirstOffset()
      )
    );
  }

  private static IdAuditSearchParameters obtainListParameters(
    final IdA1CommandAuditSearchBegin command)
  {
    try {
      final var model = command.parameters().toModel();
      if (model.limit() > 1000) {
        return new IdAuditSearchParameters(
          model.timeRange(),
          model.owner(),
          model.type(),
          model.message(),
          1000
        );
      }
      return model;
    } catch (final IdProtocolException e) {
      throw new IdValidityException(e.getMessage());
    }
  }
}

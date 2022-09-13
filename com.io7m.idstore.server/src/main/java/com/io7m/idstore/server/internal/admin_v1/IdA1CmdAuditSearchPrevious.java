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
import com.io7m.idstore.protocol.admin_v1.IdA1AuditEvent;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAuditSearchPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1Page;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAuditSearchPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseType;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.security.IdSecAdminActionAuditRead;
import com.io7m.idstore.server.security.IdSecurityException;

/**
 * IdA1CmdAuditSearchPrevious
 */

public final class IdA1CmdAuditSearchPrevious
  extends IdA1CmdAbstract<
  IdA1CommandContext, IdA1CommandAuditSearchPrevious, IdA1ResponseType>
{
  /**
   * IdA1CmdAuditSearchPrevious
   */

  public IdA1CmdAuditSearchPrevious()
  {

  }

  @Override
  protected IdA1ResponseType executeActual(
    final IdA1CommandContext context,
    final IdA1CommandAuditSearchPrevious command)
    throws IdCommandExecutionFailure, IdSecurityException, IdDatabaseException
  {
    final var transaction =
      context.transaction();
    final var admin =
      context.admin();

    context.securityCheck(new IdSecAdminActionAuditRead(admin));

    final var users =
      transaction.queries(IdDatabaseAuditQueriesType.class);

    final var session = context.userSession();
    final var paging = session.auditPaging();
    final var data = paging.pagePrevious(users);

    return new IdA1ResponseAuditSearchPrevious(
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
}

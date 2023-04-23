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

import com.io7m.idstore.database.api.IdDatabaseAdminsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.error_codes.IdException;
import com.io7m.idstore.protocol.admin.IdACommandAdminUpdate;
import com.io7m.idstore.protocol.admin.IdAResponseAdminUpdate;
import com.io7m.idstore.protocol.admin.IdAResponseType;
import com.io7m.idstore.server.controller.IdServerStrings;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.security.IdSecAdminActionAdminUpdate;
import com.io7m.jaffirm.core.Invariants;

import java.util.Objects;
import java.util.Optional;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SQL_ERROR_UNIQUE;

/**
 * IdACmdAdminUpdate
 */

public final class IdACmdAdminUpdate
  extends IdACmdAbstract<
  IdACommandContext, IdACommandAdminUpdate, IdAResponseType>
{
  /**
   * IdACmdAdminUpdate
   */

  public IdACmdAdminUpdate()
  {

  }

  @Override
  protected IdAResponseType executeActual(
    final IdACommandContext context,
    final IdACommandAdminUpdate command)
    throws IdException, IdCommandExecutionFailure
  {
    final var transaction =
      context.transaction();
    final var admin =
      context.admin();
    final var newAdmin =
      command.admin();

    context.securityCheck(new IdSecAdminActionAdminUpdate(admin, newAdmin));

    transaction.adminIdSet(admin.id());

    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);

    final var oldAdmin =
      admins.adminGetRequire(newAdmin);

    Invariants.checkInvariantV(
      Objects.equals(newAdmin, oldAdmin.id()),
      "New admin ID %s must match old admin ID %s",
      newAdmin,
      oldAdmin.id()
    );

    final var strings =
      context.services().requireService(IdServerStrings.class);

    try {
      admins.adminUpdate(
        newAdmin,
        command.idName(),
        command.realName(),
        command.password(),
        Optional.empty()
      );
    } catch (final IdDatabaseException e) {
      if (Objects.equals(e.errorCode(), SQL_ERROR_UNIQUE)) {
        throw new IdDatabaseException(
          strings.format("adminIdNameDuplicate", command.idName()),
          e,
          SQL_ERROR_UNIQUE,
          e.attributes(),
          e.remediatingAction()
        );
      }
      throw e;
    }

    final var afterAdmin =
      admins.adminGetRequire(newAdmin)
        .withRedactedPassword();

    return new IdAResponseAdminUpdate(context.requestId(), afterAdmin);
  }
}

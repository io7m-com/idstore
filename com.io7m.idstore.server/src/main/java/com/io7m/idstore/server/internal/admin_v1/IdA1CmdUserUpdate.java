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

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdOptional;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserUpdate;
import com.io7m.idstore.protocol.admin_v1.IdA1Password;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseType;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserUpdate;
import com.io7m.idstore.protocol.admin_v1.IdA1User;
import com.io7m.idstore.server.internal.IdServerStrings;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.security.IdSecAdminActionUserUpdate;
import com.io7m.idstore.server.security.IdSecurityException;
import com.io7m.jaffirm.core.Invariants;

import java.util.Objects;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SQL_ERROR_UNIQUE;

/**
 * IdA1CmdUserUpdate
 */

public final class IdA1CmdUserUpdate
  extends IdA1CmdAbstract<
  IdA1CommandContext, IdA1CommandUserUpdate, IdA1ResponseType>
{
  /**
   * IdA1CmdUserUpdate
   */

  public IdA1CmdUserUpdate()
  {

  }

  @Override
  protected IdA1ResponseType executeActual(
    final IdA1CommandContext context,
    final IdA1CommandUserUpdate command)
    throws
    IdCommandExecutionFailure,
    IdDatabaseException,
    IdSecurityException,
    IdPasswordException
  {
    final var transaction =
      context.transaction();
    final var admin =
      context.admin();

    context.securityCheck(new IdSecAdminActionUserUpdate(admin));

    transaction.adminIdSet(admin.id());

    final var users =
      transaction.queries(IdDatabaseUsersQueriesType.class);

    final var oldUser =
      users.userGetRequire(command.user());

    Invariants.checkInvariantV(
      Objects.equals(command.user(), oldUser.id()),
      "New user ID %s must match old user ID %s",
      command.user(),
      oldUser.id()
    );

    final var strings =
      context.services().requireService(IdServerStrings.class);

    try {
      users.userUpdateAsAdmin(
        command.user(),
        IdOptional.mapPartial(command.idName(), IdName::new),
        IdOptional.mapPartial(command.realName(), IdRealName::new),
        IdOptional.mapPartial(command.password(), IdA1Password::toPassword)
      );
    } catch (final IdDatabaseException e) {
      if (Objects.equals(e.errorCode(), SQL_ERROR_UNIQUE)) {
        throw new IdDatabaseException(
          strings.format("userIdNameDuplicate", command.idName()),
          e,
          SQL_ERROR_UNIQUE
        );
      }
      throw e;
    }

    final var afterUser =
      users.userGetRequire(command.user());

    return new IdA1ResponseUserUpdate(
      context.requestId(),
      IdA1User.ofUser(afterUser)
    );
  }
}

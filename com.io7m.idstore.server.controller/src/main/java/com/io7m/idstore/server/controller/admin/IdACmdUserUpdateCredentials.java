/*
 * Copyright © 2023 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.error_codes.IdException;
import com.io7m.idstore.protocol.admin.IdACommandUserUpdateCredentials;
import com.io7m.idstore.protocol.admin.IdAResponseType;
import com.io7m.idstore.protocol.admin.IdAResponseUserUpdate;
import com.io7m.idstore.server.security.IdSecAdminActionUserUpdateCredentials;
import com.io7m.idstore.server.service.clock.IdServerClock;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationService;
import com.io7m.idstore.strings.IdStrings;
import com.io7m.jaffirm.core.Invariants;

import java.util.Objects;
import java.util.UUID;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SQL_ERROR_UNIQUE;
import static com.io7m.idstore.strings.IdStringConstants.USER_ID_NAME_DUPLICATE;

/**
 * IdACmdUserUpdate
 */

public final class IdACmdUserUpdateCredentials
  extends IdACmdAbstract<
  IdACommandContext, IdACommandUserUpdateCredentials, IdAResponseType>
{
  /**
   * IdACmdUserUpdate
   */

  public IdACmdUserUpdateCredentials()
  {

  }

  @Override
  protected IdAResponseType executeActual(
    final IdACommandContext context,
    final IdACommandUserUpdateCredentials command)
    throws IdException
  {
    final var services =
      context.services();
    final var expiration =
      services.requireService(IdServerConfigurationService.class)
        .configuration()
        .passwordExpiration();
    final var clock =
      services.requireService(IdServerClock.class);
    final var strings =
      services.requireService(IdStrings.class);

    final var transaction =
      context.transaction();
    final var admin =
      context.admin();

    context.securityCheck(new IdSecAdminActionUserUpdateCredentials(admin));

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

    final var password =
      command.password()
        .map(p -> expiration.expireUserPasswordIfNecessary(clock.clock(), p));

    try {
      users.userUpdateAsAdmin(
        command.user(),
        command.idName(),
        command.realName(),
        password
      );
    } catch (final IdDatabaseException e) {
      if (Objects.equals(e.errorCode(), SQL_ERROR_UNIQUE)) {
        throw new IdDatabaseException(
          strings.format(USER_ID_NAME_DUPLICATE, command.idName()),
          e,
          SQL_ERROR_UNIQUE,
          e.attributes(),
          e.remediatingAction()
        );
      }
      throw e;
    }

    final var afterUser = users.userGetRequire(command.user());
    return new IdAResponseUserUpdate(
      UUID.randomUUID(),
      command.messageId(),
      afterUser
    );
  }
}

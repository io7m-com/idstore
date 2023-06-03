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

import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.error_codes.IdException;
import com.io7m.idstore.model.IdPassword;
import com.io7m.idstore.protocol.admin.IdACommandUserUpdatePasswordExpiration;
import com.io7m.idstore.protocol.admin.IdAPasswordExpirationSetNever;
import com.io7m.idstore.protocol.admin.IdAPasswordExpirationSetRefresh;
import com.io7m.idstore.protocol.admin.IdAPasswordExpirationSetSpecific;
import com.io7m.idstore.protocol.admin.IdAPasswordExpirationSetType;
import com.io7m.idstore.protocol.admin.IdAResponseType;
import com.io7m.idstore.protocol.admin.IdAResponseUserUpdate;
import com.io7m.idstore.server.api.IdServerPasswordExpirationConfiguration;
import com.io7m.idstore.server.security.IdSecAdminActionUserUpdateCredentials;
import com.io7m.idstore.server.service.clock.IdServerClock;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationService;

import java.time.Clock;
import java.util.Optional;

/**
 * IdACmdUserUpdatePasswordExpiration
 */

public final class IdACmdUserUpdatePasswordExpiration
  extends IdACmdAbstract<
  IdACommandContext, IdACommandUserUpdatePasswordExpiration, IdAResponseType>
{
  /**
   * IdACmdUserUpdatePasswordExpiration
   */

  public IdACmdUserUpdatePasswordExpiration()
  {

  }

  @Override
  protected IdAResponseType executeActual(
    final IdACommandContext context,
    final IdACommandUserUpdatePasswordExpiration command)
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

    final var transaction =
      context.transaction();
    final var admin =
      context.admin();

    context.securityCheck(new IdSecAdminActionUserUpdateCredentials(admin));

    transaction.adminIdSet(admin.id());

    final var users =
      transaction.queries(IdDatabaseUsersQueriesType.class);
    final var user =
      users.userGetRequire(command.user());

    final var newPassword =
      handlePasswordExpirationSet(
        clock.clock(),
        expiration,
        user.password(),
        command.set()
      );

    users.userUpdateAsAdmin(
      user.id(),
      Optional.empty(),
      Optional.empty(),
      Optional.of(newPassword)
    );

    final var afterUser = users.userGetRequire(command.user());
    return new IdAResponseUserUpdate(context.requestId(), afterUser);
  }

  private static IdPassword handlePasswordExpirationSet(
    final Clock clock,
    final IdServerPasswordExpirationConfiguration configuration,
    final IdPassword password,
    final IdAPasswordExpirationSetType set)
  {
    if (set instanceof IdAPasswordExpirationSetNever) {
      return password.withoutExpirationDate();
    }
    if (set instanceof IdAPasswordExpirationSetRefresh) {
      return configuration.expireUserPasswordIfNecessary(clock, password);
    }
    if (set instanceof final IdAPasswordExpirationSetSpecific s) {
      return password.withExpirationDate(s.time());
    }

    throw new IllegalStateException(
      "Unrecognized set type: %s".formatted(set)
    );
  }
}

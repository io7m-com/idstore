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

import com.io7m.idstore.database.api.IdDatabaseAdminsQueriesType;
import com.io7m.idstore.error_codes.IdException;
import com.io7m.idstore.model.IdPassword;
import com.io7m.idstore.protocol.admin.IdACommandAdminUpdatePasswordExpiration;
import com.io7m.idstore.protocol.admin.IdAPasswordExpirationSetNever;
import com.io7m.idstore.protocol.admin.IdAPasswordExpirationSetRefresh;
import com.io7m.idstore.protocol.admin.IdAPasswordExpirationSetSpecific;
import com.io7m.idstore.protocol.admin.IdAPasswordExpirationSetType;
import com.io7m.idstore.protocol.admin.IdAResponseAdminUpdate;
import com.io7m.idstore.protocol.admin.IdAResponseType;
import com.io7m.idstore.server.api.IdServerPasswordExpirationConfiguration;
import com.io7m.idstore.server.security.IdSecAdminActionAdminUpdate;
import com.io7m.idstore.server.service.clock.IdServerClock;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationService;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

/**
 * IdACmdAdminUpdatePasswordExpiration
 */

public final class IdACmdAdminUpdatePasswordExpiration
  extends IdACmdAbstract<
  IdACommandContext, IdACommandAdminUpdatePasswordExpiration, IdAResponseType>
{
  /**
   * IdACmdAdminUpdatePasswordExpiration
   */

  public IdACmdAdminUpdatePasswordExpiration()
  {

  }

  @Override
  protected IdAResponseType executeActual(
    final IdACommandContext context,
    final IdACommandAdminUpdatePasswordExpiration command)
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
    final var targetAdminId =
      command.user();

    context.securityCheck(new IdSecAdminActionAdminUpdate(admin, targetAdminId));

    transaction.adminIdSet(admin.id());

    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);

    final var existingAdmin =
      admins.adminGetRequire(targetAdminId);

    final var newPasswordConfiguration =
      handlePasswordExpirationSet(
        clock.clock(),
        expiration,
        existingAdmin.password(),
        command.set()
      );

    admins.adminUpdate(
      targetAdminId,
      Optional.empty(),
      Optional.empty(),
      Optional.of(newPasswordConfiguration),
      Optional.empty()
    );

    final var afterAdmin =
      admins.adminGetRequire(targetAdminId)
        .withRedactedPassword();

    return new IdAResponseAdminUpdate(
      UUID.randomUUID(),
      command.messageId(),
      afterAdmin
    );
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
      return configuration.expireAdminPasswordIfNecessary(clock, password);
    }
    if (set instanceof final IdAPasswordExpirationSetSpecific s) {
      return password.withExpirationDate(s.time());
    }

    throw new IllegalStateException(
      "Unrecognized set type: %s".formatted(set)
    );
  }
}

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

package com.io7m.idstore.server.controller.user;

import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.error_codes.IdException;
import com.io7m.idstore.model.IdPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.idstore.protocol.user.IdUCommandPasswordUpdate;
import com.io7m.idstore.protocol.user.IdUResponseType;
import com.io7m.idstore.protocol.user.IdUResponseUserUpdate;
import com.io7m.idstore.server.security.IdSecUserActionPasswordUpdate;
import com.io7m.idstore.server.service.clock.IdServerClock;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationService;

import java.util.Objects;
import java.util.Optional;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PASSWORD_RESET_MISMATCH;

/**
 * IdUCmdPasswordUpdate
 */

public final class IdUCmdPasswordUpdate
  extends IdUCmdAbstract<IdUCommandPasswordUpdate>
{
  /**
   * IdUCmdPasswordUpdate
   */

  public IdUCmdPasswordUpdate()
  {

  }

  @Override
  protected IdUResponseType executeActual(
    final IdUCommandContext context,
    final IdUCommandPasswordUpdate command)
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

    final var user = context.user();
    context.securityCheck(new IdSecUserActionPasswordUpdate(user));

    final var transaction =
      context.transaction();

    if (!Objects.equals(command.password(), command.passwordConfirm())) {
      throw context.failFormatted(
        400,
        PASSWORD_RESET_MISMATCH,
        "passwordResetMismatch"
      );
    }

    final var users =
      transaction.queries(IdDatabaseUsersQueriesType.class);

    transaction.userIdSet(user.id());

    /*
     * Create a new hashed password based on the provided text, and then
     * set an expiration date on it if expiration is enabled.
     */

    final var newPassword =
      IdPasswordAlgorithmPBKDF2HmacSHA256.create()
        .createHashed(command.password());

    final var newPasswordExpiring =
      expiration.expireUserPasswordIfNecessary(clock.clock(), newPassword);

    users.userUpdate(
      user.id(),
      Optional.empty(),
      Optional.empty(),
      Optional.of(newPasswordExpiring)
    );

    return new IdUResponseUserUpdate(
      context.requestId(),
      users.userGetRequire(user.id())
    );
  }
}

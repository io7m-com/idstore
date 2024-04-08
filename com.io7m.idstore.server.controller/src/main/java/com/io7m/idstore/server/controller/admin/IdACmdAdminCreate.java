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
import com.io7m.idstore.protocol.admin.IdACommandAdminCreate;
import com.io7m.idstore.protocol.admin.IdAResponseAdminCreate;
import com.io7m.idstore.protocol.admin.IdAResponseType;
import com.io7m.idstore.server.security.IdSecAdminActionAdminCreate;
import com.io7m.idstore.server.service.clock.IdServerClock;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationService;

import java.util.UUID;

/**
 * IdACmdAdminCreate
 */

public final class IdACmdAdminCreate
  extends IdACmdAbstract<
  IdACommandContext, IdACommandAdminCreate, IdAResponseType>
{
  /**
   * IdACmdAdminCreate
   */

  public IdACmdAdminCreate()
  {

  }

  @Override
  protected IdAResponseType executeActual(
    final IdACommandContext context,
    final IdACommandAdminCreate command)
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

    final var permissions = command.permissions();
    context.securityCheck(new IdSecAdminActionAdminCreate(admin, permissions));

    transaction.adminIdSet(admin.id());

    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);

    final var id =
      command.id().orElseGet(UUID::randomUUID);
    final var idName =
      command.idName();
    final var realName =
      command.realName();
    final var email =
      command.email();

    final var password =
      expiration.expireAdminPasswordIfNecessary(
        clock.clock(),
        command.password()
      );

    final var newAdmin =
      admins.adminCreate(
        id,
        idName,
        realName,
        email,
        context.now(),
        password,
        permissions
      ).withRedactedPassword();

    return new IdAResponseAdminCreate(
      UUID.randomUUID(),
      command.messageId(),
      newAdmin
    );
  }

}

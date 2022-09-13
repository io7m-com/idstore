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

import com.io7m.idstore.database.api.IdDatabaseAdminsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.protocol.admin_v1.IdA1Admin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminPermissionGrant;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminUpdate;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseType;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.security.IdSecAdminActionAdminPermissionGrant;
import com.io7m.idstore.server.security.IdSecurityException;
import com.io7m.jaffirm.core.Invariants;

import java.util.Objects;
import java.util.Optional;

/**
 * IdA1CmdAdminPermissionGrant
 */

public final class IdA1CmdAdminPermissionGrant
  extends IdA1CmdAbstract<
  IdA1CommandContext, IdA1CommandAdminPermissionGrant, IdA1ResponseType>
{
  /**
   * IdA1CmdAdminPermissionGrant
   */

  public IdA1CmdAdminPermissionGrant()
  {

  }

  @Override
  public IdA1ResponseType executeActual(
    final IdA1CommandContext context,
    final IdA1CommandAdminPermissionGrant command)
    throws IdCommandExecutionFailure, IdDatabaseException, IdSecurityException
  {
    final var transaction =
      context.transaction();
    final var admin =
      context.admin();
    final var newAdmin =
      command.admin();
    final var newPerm =
      command.permission().toPermission();

    context.securityCheck(new IdSecAdminActionAdminPermissionGrant(admin, newAdmin, newPerm));

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

    final var newPermissions =
      oldAdmin.permissions()
        .plus(newPerm)
        .impliedPermissions();

    admins.adminUpdate(
      command.admin(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.of(newPermissions)
    );

    final var afterAdmin =
      admins.adminGetRequire(newAdmin);

    return new IdA1ResponseAdminUpdate(
      context.requestId(),
      IdA1Admin.ofAdmin(afterAdmin)
    );
  }
}

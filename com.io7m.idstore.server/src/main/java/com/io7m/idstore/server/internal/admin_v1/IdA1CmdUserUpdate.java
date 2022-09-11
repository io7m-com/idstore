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
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdValidityException;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserUpdate;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseType;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserUpdate;
import com.io7m.idstore.protocol.admin_v1.IdA1User;
import com.io7m.idstore.server.internal.IdServerStrings;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutorType;
import com.io7m.idstore.server.security.IdSecAdminActionUserUpdate;
import com.io7m.idstore.server.security.IdSecPolicyResultDenied;
import com.io7m.idstore.server.security.IdSecurity;
import com.io7m.idstore.server.security.IdSecurityException;
import com.io7m.jaffirm.core.Invariants;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SECURITY_POLICY_DENIED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SQL_ERROR_UNIQUE;
import static org.eclipse.jetty.http.HttpStatus.FORBIDDEN_403;

/**
 * IdA1CmdUserUpdate
 */

public final class IdA1CmdUserUpdate
  implements IdCommandExecutorType<
  IdA1CommandContext, IdA1CommandUserUpdate, IdA1ResponseType>
{
  /**
   * IdA1CmdUserUpdate
   */

  public IdA1CmdUserUpdate()
  {

  }

  @Override
  public IdA1ResponseType execute(
    final IdA1CommandContext context,
    final IdA1CommandUserUpdate command)
    throws IdCommandExecutionFailure
  {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(command, "command");

    try {
      final var transaction =
        context.transaction();
      final var admin =
        context.admin();

      if (IdSecurity.check(new IdSecAdminActionUserUpdate(admin))
        instanceof IdSecPolicyResultDenied denied) {
        throw context.fail(
          FORBIDDEN_403,
          SECURITY_POLICY_DENIED,
          denied.message()
        );
      }

      transaction.adminIdSet(admin.id());

      final var users =
        transaction.queries(IdDatabaseUsersQueriesType.class);

      final var newUser =
        command.user().toUser();
      final var oldUser =
        users.userGetRequire(newUser.id());

      Invariants.checkInvariantV(
        Objects.equals(newUser.id(), oldUser.id()),
        "New user ID %s must match old user ID %s",
        newUser.id(),
        oldUser.id()
      );

      final var strings =
        context.services().requireService(IdServerStrings.class);

      try {
        users.userUpdateAsAdmin(
          newUser.id(),
          Optional.of(newUser.idName()),
          Optional.of(newUser.realName()),
          Optional.of(newUser.password())
        );
      } catch (final IdDatabaseException e) {
        if (Objects.equals(e.errorCode(), SQL_ERROR_UNIQUE)) {
          throw new IdDatabaseException(
            strings.format("userIdNameDuplicate", newUser.idName()),
            e,
            SQL_ERROR_UNIQUE
          );
        }
        throw e;
      }

      final var newUserEmails =
        newUser.emails().toList();
      final var oldUserEmails =
        oldUser.emails().toList();

      final var emailsToAdd = new HashSet<>(newUserEmails);
      emailsToAdd.removeAll(oldUserEmails);
      final var emailsToRemove = new HashSet<>(oldUserEmails);
      emailsToRemove.removeAll(newUserEmails);

      for (final var email : emailsToAdd) {
        try {
          users.userEmailAdd(newUser.id(), email);
        } catch (IdDatabaseException e) {
          if (Objects.equals(e.errorCode(), SQL_ERROR_UNIQUE)) {
            throw new IdDatabaseException(
              strings.format("emailDuplicate"),
              e,
              SQL_ERROR_UNIQUE
            );
          }
          throw e;
        }
      }

      for (final var email : emailsToRemove) {
        users.userEmailRemove(newUser.id(), email);
      }

      final var afterUser =
        users.userGetRequire(newUser.id());

      return new IdA1ResponseUserUpdate(
        context.requestId(),
        IdA1User.ofUser(afterUser)
      );
    } catch (final IdValidityException e) {
      throw context.failValidity(e);
    } catch (final IdSecurityException e) {
      throw context.failSecurity(e);
    } catch (final IdDatabaseException e) {
      throw context.failDatabase(e);
    } catch (final IdPasswordException e) {
      throw context.failPassword(e);
    }
  }
}

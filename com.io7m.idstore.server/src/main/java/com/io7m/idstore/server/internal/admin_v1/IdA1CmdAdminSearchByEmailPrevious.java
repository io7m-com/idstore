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
import com.io7m.idstore.model.IdValidityException;
import com.io7m.idstore.protocol.admin_v1.IdA1AdminSummary;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1Page;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseType;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutorType;
import com.io7m.idstore.server.security.IdSecAdminActionAdminRead;
import com.io7m.idstore.server.security.IdSecPolicyResultDenied;
import com.io7m.idstore.server.security.IdSecurity;
import com.io7m.idstore.server.security.IdSecurityException;

import java.util.Objects;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SECURITY_POLICY_DENIED;
import static org.eclipse.jetty.http.HttpStatus.FORBIDDEN_403;

/**
 * IdA1CmdAdminSearchByEmailPrevious
 */

public final class IdA1CmdAdminSearchByEmailPrevious
  implements IdCommandExecutorType<
  IdA1CommandContext, IdA1CommandAdminSearchByEmailPrevious, IdA1ResponseType>
{
  /**
   * IdA1CmdAdminSearchByEmailPrevious
   */

  public IdA1CmdAdminSearchByEmailPrevious()
  {

  }

  @Override
  public IdA1ResponseType execute(
    final IdA1CommandContext context,
    final IdA1CommandAdminSearchByEmailPrevious command)
    throws IdCommandExecutionFailure
  {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(command, "command");

    try {
      final var transaction =
        context.transaction();
      final var admin =
        context.admin();

      if (IdSecurity.check(new IdSecAdminActionAdminRead(admin))
        instanceof IdSecPolicyResultDenied denied) {
        throw context.fail(
          FORBIDDEN_403,
          SECURITY_POLICY_DENIED,
          denied.message()
        );
      }

      final var admins =
        transaction.queries(IdDatabaseAdminsQueriesType.class);

      final var session = context.userSession();
      final var paging = session.adminByEmailPaging();
      final var data = paging.pagePrevious(admins);

      final var results =
        data.stream()
          .map(IdA1AdminSummary::of)
          .toList();

      return new IdA1ResponseAdminSearchByEmailPrevious(
        context.requestId(),
        new IdA1Page<>(
          results,
          paging.pageNumber(),
          paging.pageCount(),
          paging.pageFirstOffset()
        )
      );

    } catch (final IdValidityException e) {
      throw context.failValidity(e);
    } catch (final IdSecurityException e) {
      throw context.failSecurity(e);
    } catch (final IdDatabaseException e) {
      throw context.failDatabase(e);
    }
  }
}

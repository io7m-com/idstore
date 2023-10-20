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


import com.io7m.idstore.protocol.admin.IdACommandAdminBanCreate;
import com.io7m.idstore.protocol.admin.IdACommandAdminBanDelete;
import com.io7m.idstore.protocol.admin.IdACommandAdminBanGet;
import com.io7m.idstore.protocol.admin.IdACommandAdminCreate;
import com.io7m.idstore.protocol.admin.IdACommandAdminDelete;
import com.io7m.idstore.protocol.admin.IdACommandAdminEmailAdd;
import com.io7m.idstore.protocol.admin.IdACommandAdminEmailRemove;
import com.io7m.idstore.protocol.admin.IdACommandAdminGet;
import com.io7m.idstore.protocol.admin.IdACommandAdminGetByEmail;
import com.io7m.idstore.protocol.admin.IdACommandAdminPermissionGrant;
import com.io7m.idstore.protocol.admin.IdACommandAdminPermissionRevoke;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchBegin;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchByEmailBegin;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchByEmailNext;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchNext;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchPrevious;
import com.io7m.idstore.protocol.admin.IdACommandAdminSelf;
import com.io7m.idstore.protocol.admin.IdACommandAdminUpdateCredentials;
import com.io7m.idstore.protocol.admin.IdACommandAdminUpdatePasswordExpiration;
import com.io7m.idstore.protocol.admin.IdACommandAuditSearchBegin;
import com.io7m.idstore.protocol.admin.IdACommandAuditSearchNext;
import com.io7m.idstore.protocol.admin.IdACommandAuditSearchPrevious;
import com.io7m.idstore.protocol.admin.IdACommandLogin;
import com.io7m.idstore.protocol.admin.IdACommandMailTest;
import com.io7m.idstore.protocol.admin.IdACommandMaintenanceModeSet;
import com.io7m.idstore.protocol.admin.IdACommandType;
import com.io7m.idstore.protocol.admin.IdACommandUserBanCreate;
import com.io7m.idstore.protocol.admin.IdACommandUserBanDelete;
import com.io7m.idstore.protocol.admin.IdACommandUserBanGet;
import com.io7m.idstore.protocol.admin.IdACommandUserCreate;
import com.io7m.idstore.protocol.admin.IdACommandUserDelete;
import com.io7m.idstore.protocol.admin.IdACommandUserEmailAdd;
import com.io7m.idstore.protocol.admin.IdACommandUserEmailRemove;
import com.io7m.idstore.protocol.admin.IdACommandUserGet;
import com.io7m.idstore.protocol.admin.IdACommandUserGetByEmail;
import com.io7m.idstore.protocol.admin.IdACommandUserLoginHistory;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchBegin;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchByEmailBegin;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchByEmailNext;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchNext;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchPrevious;
import com.io7m.idstore.protocol.admin.IdACommandUserUpdateCredentials;
import com.io7m.idstore.protocol.admin.IdACommandUserUpdatePasswordExpiration;
import com.io7m.idstore.protocol.admin.IdAResponseType;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutorType;
import com.io7m.idstore.server.service.sessions.IdSessionAdmin;

import static com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType.recordSpanException;

/**
 * A command executor for public commands.
 */

public final class IdACommandExecutor
  implements IdCommandExecutorType<
  IdSessionAdmin,
  IdACommandContext,
  IdACommandType<? extends IdAResponseType>,
  IdAResponseType>
{
  /**
   * A command executor for public commands.
   */

  public IdACommandExecutor()
  {

  }

  @Override
  public IdAResponseType execute(
    final IdACommandContext context,
    final IdACommandType<? extends IdAResponseType> command)
    throws IdCommandExecutionFailure
  {
    final var span =
      context.tracer()
        .spanBuilder(command.getClass().getSimpleName())
        .startSpan();

    try (var ignored = span.makeCurrent()) {
      return executeCommand(context, command);
    } catch (final Throwable e) {
      recordSpanException(e);
      throw e;
    } finally {
      span.end();
    }
  }

  private static IdAResponseType executeCommand(
    final IdACommandContext context,
    final IdACommandType<? extends IdAResponseType> command)
    throws IdCommandExecutionFailure
  {
    return switch (command) {
      case final IdACommandAdminSelf c ->
        new IdACmdAdminSelf().execute(context, c);
      case final IdACommandLogin c ->
        new IdACmdAdminLogin().execute(context, c);
      case final IdACommandUserSearchBegin c ->
        new IdACmdUserSearchBegin().execute(context, c);
      case final IdACommandUserSearchPrevious c ->
        new IdACmdUserSearchPrevious().execute(context, c);
      case final IdACommandUserSearchNext c ->
        new IdACmdUserSearchNext().execute(context, c);
      case final IdACommandUserSearchByEmailBegin c ->
        new IdACmdUserSearchByEmailBegin().execute(context, c);
      case final IdACommandUserSearchByEmailPrevious c ->
        new IdACmdUserSearchByEmailPrevious().execute(context, c);
      case final IdACommandUserSearchByEmailNext c ->
        new IdACmdUserSearchByEmailNext().execute(context, c);
      case final IdACommandUserGet c -> new IdACmdUserGet().execute(context, c);
      case final IdACommandUserGetByEmail c ->
        new IdACmdUserGetByEmail().execute(context, c);
      case final IdACommandUserCreate c ->
        new IdACmdUserCreate().execute(context, c);
      case final IdACommandUserUpdateCredentials c ->
        new IdACmdUserUpdateCredentials().execute(context, c);
      case final IdACommandUserUpdatePasswordExpiration c ->
        new IdACmdUserUpdatePasswordExpiration().execute(context, c);
      case final IdACommandUserDelete c ->
        new IdACmdUserDelete().execute(context, c);
      case final IdACommandUserEmailAdd c ->
        new IdACmdUserEmailAdd().execute(context, c);
      case final IdACommandUserEmailRemove c ->
        new IdACmdUserEmailRemove().execute(context, c);
      case final IdACommandAuditSearchBegin c ->
        new IdACmdAuditSearchBegin().execute(context, c);
      case final IdACommandAuditSearchPrevious c ->
        new IdACmdAuditSearchPrevious().execute(context, c);
      case final IdACommandAuditSearchNext c ->
        new IdACmdAuditSearchNext().execute(context, c);
      case final IdACommandAdminGet c ->
        new IdACmdAdminGet().execute(context, c);
      case final IdACommandAdminGetByEmail c ->
        new IdACmdAdminGetByEmail().execute(context, c);
      case final IdACommandAdminCreate c ->
        new IdACmdAdminCreate().execute(context, c);
      case final IdACommandAdminUpdateCredentials c ->
        new IdACmdAdminUpdateCredentials().execute(context, c);
      case final IdACommandAdminUpdatePasswordExpiration c ->
        new IdACmdAdminUpdatePasswordExpiration().execute(context, c);
      case final IdACommandAdminDelete c ->
        new IdACmdAdminDelete().execute(context, c);
      case final IdACommandAdminSearchBegin c ->
        new IdACmdAdminSearchBegin().execute(context, c);
      case final IdACommandAdminSearchPrevious c ->
        new IdACmdAdminSearchPrevious().execute(context, c);
      case final IdACommandAdminSearchNext c ->
        new IdACmdAdminSearchNext().execute(context, c);
      case final IdACommandAdminSearchByEmailBegin c ->
        new IdACmdAdminSearchByEmailBegin().execute(context, c);
      case final IdACommandAdminSearchByEmailPrevious c ->
        new IdACmdAdminSearchByEmailPrevious().execute(context, c);
      case final IdACommandAdminSearchByEmailNext c ->
        new IdACmdAdminSearchByEmailNext().execute(context, c);
      case final IdACommandAdminEmailAdd c ->
        new IdACmdAdminEmailAdd().execute(context, c);
      case final IdACommandAdminEmailRemove c ->
        new IdACmdAdminEmailRemove().execute(context, c);
      case final IdACommandAdminPermissionRevoke c ->
        new IdACmdAdminPermissionRevoke().execute(context, c);
      case final IdACommandAdminPermissionGrant c ->
        new IdACmdAdminPermissionGrant().execute(context, c);
      case final IdACommandAdminBanCreate c ->
        new IdACmdAdminBanCreate().execute(context, c);
      case final IdACommandAdminBanDelete c ->
        new IdACmdAdminBanDelete().execute(context, c);
      case final IdACommandAdminBanGet c ->
        new IdACmdAdminBanGet().execute(context, c);
      case final IdACommandUserBanCreate c ->
        new IdACmdUserBanCreate().execute(context, c);
      case final IdACommandUserBanDelete c ->
        new IdACmdUserBanDelete().execute(context, c);
      case final IdACommandUserBanGet c ->
        new IdACmdUserBanGet().execute(context, c);
      case final IdACommandUserLoginHistory c ->
        new IdACmdUserLoginHistory().execute(context, c);
      case final IdACommandMailTest c ->
        new IdACmdMailTest().execute(context, c);
      case final IdACommandMaintenanceModeSet c ->
        new IdACmdMaintenanceModeSet().execute(context, c);
    };
  }
}

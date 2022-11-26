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
import com.io7m.idstore.protocol.admin.IdACommandAdminUpdate;
import com.io7m.idstore.protocol.admin.IdACommandAuditSearchBegin;
import com.io7m.idstore.protocol.admin.IdACommandAuditSearchNext;
import com.io7m.idstore.protocol.admin.IdACommandAuditSearchPrevious;
import com.io7m.idstore.protocol.admin.IdACommandLogin;
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
import com.io7m.idstore.protocol.admin.IdACommandUserUpdate;
import com.io7m.idstore.protocol.admin.IdAResponseType;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutorType;
import com.io7m.idstore.server.service.sessions.IdSessionAdmin;

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
      span.recordException(e);
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
    if (command instanceof IdACommandAdminSelf c) {
      return new IdACmdAdminSelf().execute(context, c);
    }
    if (command instanceof IdACommandLogin c) {
      return new IdACmdAdminLogin().execute(context, c);
    }

    if (command instanceof IdACommandUserSearchBegin c) {
      return new IdACmdUserSearchBegin().execute(context, c);
    }
    if (command instanceof IdACommandUserSearchPrevious c) {
      return new IdACmdUserSearchPrevious().execute(context, c);
    }
    if (command instanceof IdACommandUserSearchNext c) {
      return new IdACmdUserSearchNext().execute(context, c);
    }
    if (command instanceof IdACommandUserSearchByEmailBegin c) {
      return new IdACmdUserSearchByEmailBegin().execute(context, c);
    }
    if (command instanceof IdACommandUserSearchByEmailPrevious c) {
      return new IdACmdUserSearchByEmailPrevious().execute(context, c);
    }
    if (command instanceof IdACommandUserSearchByEmailNext c) {
      return new IdACmdUserSearchByEmailNext().execute(context, c);
    }

    if (command instanceof IdACommandUserGet c) {
      return new IdACmdUserGet().execute(context, c);
    }
    if (command instanceof IdACommandUserGetByEmail c) {
      return new IdACmdUserGetByEmail().execute(context, c);
    }
    if (command instanceof IdACommandUserCreate c) {
      return new IdACmdUserCreate().execute(context, c);
    }
    if (command instanceof IdACommandUserUpdate c) {
      return new IdACmdUserUpdate().execute(context, c);
    }
    if (command instanceof IdACommandUserDelete c) {
      return new IdACmdUserDelete().execute(context, c);
    }
    if (command instanceof IdACommandUserEmailAdd c) {
      return new IdACmdUserEmailAdd().execute(context, c);
    }
    if (command instanceof IdACommandUserEmailRemove c) {
      return new IdACmdUserEmailRemove().execute(context, c);
    }

    if (command instanceof IdACommandAuditSearchBegin c) {
      return new IdACmdAuditSearchBegin().execute(context, c);
    }
    if (command instanceof IdACommandAuditSearchPrevious c) {
      return new IdACmdAuditSearchPrevious().execute(context, c);
    }
    if (command instanceof IdACommandAuditSearchNext c) {
      return new IdACmdAuditSearchNext().execute(context, c);
    }

    if (command instanceof IdACommandAdminGet c) {
      return new IdACmdAdminGet().execute(context, c);
    }
    if (command instanceof IdACommandAdminGetByEmail c) {
      return new IdACmdAdminGetByEmail().execute(context, c);
    }
    if (command instanceof IdACommandAdminCreate c) {
      return new IdACmdAdminCreate().execute(context, c);
    }
    if (command instanceof IdACommandAdminUpdate c) {
      return new IdACmdAdminUpdate().execute(context, c);
    }
    if (command instanceof IdACommandAdminDelete c) {
      return new IdACmdAdminDelete().execute(context, c);
    }

    if (command instanceof IdACommandAdminSearchBegin c) {
      return new IdACmdAdminSearchBegin().execute(context, c);
    }
    if (command instanceof IdACommandAdminSearchPrevious c) {
      return new IdACmdAdminSearchPrevious().execute(context, c);
    }
    if (command instanceof IdACommandAdminSearchNext c) {
      return new IdACmdAdminSearchNext().execute(context, c);
    }
    if (command instanceof IdACommandAdminSearchByEmailBegin c) {
      return new IdACmdAdminSearchByEmailBegin().execute(context, c);
    }
    if (command instanceof IdACommandAdminSearchByEmailPrevious c) {
      return new IdACmdAdminSearchByEmailPrevious().execute(context, c);
    }
    if (command instanceof IdACommandAdminSearchByEmailNext c) {
      return new IdACmdAdminSearchByEmailNext().execute(context, c);
    }

    if (command instanceof IdACommandAdminEmailAdd c) {
      return new IdACmdAdminEmailAdd().execute(context, c);
    }
    if (command instanceof IdACommandAdminEmailRemove c) {
      return new IdACmdAdminEmailRemove().execute(context, c);
    }
    if (command instanceof IdACommandAdminPermissionRevoke c) {
      return new IdACmdAdminPermissionRevoke().execute(context, c);
    }
    if (command instanceof IdACommandAdminPermissionGrant c) {
      return new IdACmdAdminPermissionGrant().execute(context, c);
    }

    if (command instanceof IdACommandAdminBanCreate c) {
      return new IdACmdAdminBanCreate().execute(context, c);
    }
    if (command instanceof IdACommandAdminBanDelete c) {
      return new IdACmdAdminBanDelete().execute(context, c);
    }
    if (command instanceof IdACommandAdminBanGet c) {
      return new IdACmdAdminBanGet().execute(context, c);
    }

    if (command instanceof IdACommandUserBanCreate c) {
      return new IdACmdUserBanCreate().execute(context, c);
    }
    if (command instanceof IdACommandUserBanDelete c) {
      return new IdACmdUserBanDelete().execute(context, c);
    }
    if (command instanceof IdACommandUserBanGet c) {
      return new IdACmdUserBanGet().execute(context, c);
    }

    if (command instanceof IdACommandUserLoginHistory c) {
      return new IdACmdUserLoginHistory().execute(context, c);
    }

    throw new IllegalStateException();
  }
}

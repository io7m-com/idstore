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

import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminBanCreate;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminBanDelete;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminBanGet;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminCreate;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminDelete;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminEmailAdd;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminEmailRemove;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminGet;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminGetByEmail;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminPermissionGrant;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminPermissionRevoke;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSearchBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSearchByEmailBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSearchByEmailNext;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSearchNext;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSearchPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSelf;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminUpdate;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAuditSearchBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAuditSearchNext;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAuditSearchPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandLogin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandType;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserBanCreate;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserBanDelete;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserBanGet;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserCreate;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserDelete;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserEmailAdd;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserEmailRemove;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserGet;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserGetByEmail;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserLoginHistory;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserSearchBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserSearchByEmailBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserSearchByEmailNext;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserSearchNext;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserSearchPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserUpdate;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseType;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutorType;

import java.io.IOException;

/**
 * A command executor for public commands.
 */

public final class IdA1CommandExecutor
  implements IdCommandExecutorType<
  IdA1CommandContext,
  IdA1CommandType<? extends IdA1ResponseType>,
  IdA1ResponseType>
{
  /**
   * A command executor for public commands.
   */

  public IdA1CommandExecutor()
  {

  }

  @Override
  public IdA1ResponseType execute(
    final IdA1CommandContext context,
    final IdA1CommandType<? extends IdA1ResponseType> command)
    throws IdCommandExecutionFailure, IOException, InterruptedException
  {
    if (command instanceof IdA1CommandAdminSelf c) {
      return new IdA1CmdAdminSelf().execute(context, c);
    }
    if (command instanceof IdA1CommandLogin c) {
      return new IdA1CmdAdminLogin().execute(context, c);
    }

    if (command instanceof IdA1CommandUserSearchBegin c) {
      return new IdA1CmdUserSearchBegin().execute(context, c);
    }
    if (command instanceof IdA1CommandUserSearchPrevious c) {
      return new IdA1CmdUserSearchPrevious().execute(context, c);
    }
    if (command instanceof IdA1CommandUserSearchNext c) {
      return new IdA1CmdUserSearchNext().execute(context, c);
    }
    if (command instanceof IdA1CommandUserSearchByEmailBegin c) {
      return new IdA1CmdUserSearchByEmailBegin().execute(context, c);
    }
    if (command instanceof IdA1CommandUserSearchByEmailPrevious c) {
      return new IdA1CmdUserSearchByEmailPrevious().execute(context, c);
    }
    if (command instanceof IdA1CommandUserSearchByEmailNext c) {
      return new IdA1CmdUserSearchByEmailNext().execute(context, c);
    }

    if (command instanceof IdA1CommandUserGet c) {
      return new IdA1CmdUserGet().execute(context, c);
    }
    if (command instanceof IdA1CommandUserGetByEmail c) {
      return new IdA1CmdUserGetByEmail().execute(context, c);
    }
    if (command instanceof IdA1CommandUserCreate c) {
      return new IdA1CmdUserCreate().execute(context, c);
    }
    if (command instanceof IdA1CommandUserUpdate c) {
      return new IdA1CmdUserUpdate().execute(context, c);
    }
    if (command instanceof IdA1CommandUserDelete c) {
      return new IdA1CmdUserDelete().execute(context, c);
    }
    if (command instanceof IdA1CommandUserEmailAdd c) {
      return new IdA1CmdUserEmailAdd().execute(context, c);
    }
    if (command instanceof IdA1CommandUserEmailRemove c) {
      return new IdA1CmdUserEmailRemove().execute(context, c);
    }

    if (command instanceof IdA1CommandAuditSearchBegin c) {
      return new IdA1CmdAuditSearchBegin().execute(context, c);
    }
    if (command instanceof IdA1CommandAuditSearchPrevious c) {
      return new IdA1CmdAuditSearchPrevious().execute(context, c);
    }
    if (command instanceof IdA1CommandAuditSearchNext c) {
      return new IdA1CmdAuditSearchNext().execute(context, c);
    }

    if (command instanceof IdA1CommandAdminGet c) {
      return new IdA1CmdAdminGet().execute(context, c);
    }
    if (command instanceof IdA1CommandAdminGetByEmail c) {
      return new IdA1CmdAdminGetByEmail().execute(context, c);
    }
    if (command instanceof IdA1CommandAdminCreate c) {
      return new IdA1CmdAdminCreate().execute(context, c);
    }
    if (command instanceof IdA1CommandAdminUpdate c) {
      return new IdA1CmdAdminUpdate().execute(context, c);
    }
    if (command instanceof IdA1CommandAdminDelete c) {
      return new IdA1CmdAdminDelete().execute(context, c);
    }

    if (command instanceof IdA1CommandAdminSearchBegin c) {
      return new IdA1CmdAdminSearchBegin().execute(context, c);
    }
    if (command instanceof IdA1CommandAdminSearchPrevious c) {
      return new IdA1CmdAdminSearchPrevious().execute(context, c);
    }
    if (command instanceof IdA1CommandAdminSearchNext c) {
      return new IdA1CmdAdminSearchNext().execute(context, c);
    }
    if (command instanceof IdA1CommandAdminSearchByEmailBegin c) {
      return new IdA1CmdAdminSearchByEmailBegin().execute(context, c);
    }
    if (command instanceof IdA1CommandAdminSearchByEmailPrevious c) {
      return new IdA1CmdAdminSearchByEmailPrevious().execute(context, c);
    }
    if (command instanceof IdA1CommandAdminSearchByEmailNext c) {
      return new IdA1CmdAdminSearchByEmailNext().execute(context, c);
    }

    if (command instanceof IdA1CommandAdminEmailAdd c) {
      return new IdA1CmdAdminEmailAdd().execute(context, c);
    }
    if (command instanceof IdA1CommandAdminEmailRemove c) {
      return new IdA1CmdAdminEmailRemove().execute(context, c);
    }
    if (command instanceof IdA1CommandAdminPermissionRevoke c) {
      return new IdA1CmdAdminPermissionRevoke().execute(context, c);
    }
    if (command instanceof IdA1CommandAdminPermissionGrant c) {
      return new IdA1CmdAdminPermissionGrant().execute(context, c);
    }

    if (command instanceof IdA1CommandAdminBanCreate c) {
      return new IdA1CmdAdminBanCreate().execute(context, c);
    }
    if (command instanceof IdA1CommandAdminBanDelete c) {
      return new IdA1CmdAdminBanDelete().execute(context, c);
    }
    if (command instanceof IdA1CommandAdminBanGet c) {
      return new IdA1CmdAdminBanGet().execute(context, c);
    }

    if (command instanceof IdA1CommandUserBanCreate c) {
      return new IdA1CmdUserBanCreate().execute(context, c);
    }
    if (command instanceof IdA1CommandUserBanDelete c) {
      return new IdA1CmdUserBanDelete().execute(context, c);
    }
    if (command instanceof IdA1CommandUserBanGet c) {
      return new IdA1CmdUserBanGet().execute(context, c);
    }

    if (command instanceof IdA1CommandUserLoginHistory c) {
      return new IdA1CmdUserLoginHistory().execute(context, c);
    }

    throw new IllegalStateException();
  }
}

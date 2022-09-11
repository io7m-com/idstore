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

import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSelf;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAuditSearchBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAuditSearchNext;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAuditSearchPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandLogin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandType;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserCreate;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserGet;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserGetByEmail;
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
    throws IdCommandExecutionFailure
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

    if (command instanceof IdA1CommandAuditSearchBegin c) {
      return new IdA1CmdAuditSearchBegin().execute(context, c);
    }
    if (command instanceof IdA1CommandAuditSearchPrevious c) {
      return new IdA1CmdAuditSearchPrevious().execute(context, c);
    }
    if (command instanceof IdA1CommandAuditSearchNext c) {
      return new IdA1CmdAuditSearchNext().execute(context, c);
    }

    throw new IllegalStateException();
  }
}

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

package com.io7m.idstore.server.internal.user_v1;

import com.io7m.idstore.protocol.user_v1.IdU1CommandEmailAddBegin;
import com.io7m.idstore.protocol.user_v1.IdU1CommandEmailAddDeny;
import com.io7m.idstore.protocol.user_v1.IdU1CommandEmailAddPermit;
import com.io7m.idstore.protocol.user_v1.IdU1CommandEmailRemoveBegin;
import com.io7m.idstore.protocol.user_v1.IdU1CommandEmailRemoveDeny;
import com.io7m.idstore.protocol.user_v1.IdU1CommandEmailRemovePermit;
import com.io7m.idstore.protocol.user_v1.IdU1CommandRealnameUpdate;
import com.io7m.idstore.protocol.user_v1.IdU1CommandType;
import com.io7m.idstore.protocol.user_v1.IdU1CommandUserSelf;
import com.io7m.idstore.protocol.user_v1.IdU1ResponseType;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutorType;

import java.io.IOException;

/**
 * A command executor for public commands.
 */

public final class IdU1CommandExecutor
  implements IdCommandExecutorType<
  IdU1CommandContext,
  IdU1CommandType<? extends IdU1ResponseType>,
  IdU1ResponseType>
{
  /**
   * A command executor for public commands.
   */

  public IdU1CommandExecutor()
  {

  }

  @Override
  public IdU1ResponseType execute(
    final IdU1CommandContext context,
    final IdU1CommandType<? extends IdU1ResponseType> command)
    throws IdCommandExecutionFailure, IOException, InterruptedException
  {
    if (command instanceof IdU1CommandUserSelf c) {
      return new IdU1CmdUserSelf().execute(context, c);
    }
    if (command instanceof IdU1CommandEmailAddPermit c) {
      return new IdU1CmdEmailAddPermit().execute(context, c);
    }
    if (command instanceof IdU1CommandEmailAddDeny c) {
      return new IdU1CmdEmailAddDeny().execute(context, c);
    }
    if (command instanceof IdU1CommandEmailAddBegin c) {
      return new IdU1CmdEmailAddBegin().execute(context, c);
    }
    if (command instanceof IdU1CommandEmailRemovePermit c) {
      return new IdU1CmdEmailRemovePermit().execute(context, c);
    }
    if (command instanceof IdU1CommandEmailRemoveDeny c) {
      return new IdU1CmdEmailRemoveDeny().execute(context, c);
    }
    if (command instanceof IdU1CommandEmailRemoveBegin c) {
      return new IdU1CmdEmailRemoveBegin().execute(context, c);
    }
    if (command instanceof IdU1CommandRealnameUpdate c) {
      return new IdU1CmdRealNameUpdate().execute(context, c);
    }

    throw new IllegalStateException();
  }
}

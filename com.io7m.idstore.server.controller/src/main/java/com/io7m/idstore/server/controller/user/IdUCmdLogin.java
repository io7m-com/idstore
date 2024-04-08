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

import com.io7m.idstore.protocol.user.IdUCommandLogin;
import com.io7m.idstore.protocol.user.IdUResponseType;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutorType;
import com.io7m.idstore.server.service.sessions.IdSessionUser;

import java.util.Objects;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.API_MISUSE_ERROR;
import static com.io7m.idstore.strings.IdStringConstants.COMMAND_NOT_HERE;

/**
 * IdUCmdLogin
 */

public final class IdUCmdLogin
  implements IdCommandExecutorType<
  IdSessionUser, IdUCommandContext, IdUCommandLogin, IdUResponseType>
{
  /**
   * IdUCmdLogin
   */

  public IdUCmdLogin()
  {

  }

  @Override
  public IdUResponseType execute(
    final IdUCommandContext context,
    final IdUCommandLogin command)
    throws IdCommandExecutionFailure
  {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(command, "command");

    throw context.failFormatted(
      command,
      400,
      API_MISUSE_ERROR,
      COMMAND_NOT_HERE
    );
  }
}

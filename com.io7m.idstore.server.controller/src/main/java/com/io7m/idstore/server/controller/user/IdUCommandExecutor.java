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

import com.io7m.idstore.protocol.user.IdUCommandEmailAddBegin;
import com.io7m.idstore.protocol.user.IdUCommandEmailAddDeny;
import com.io7m.idstore.protocol.user.IdUCommandEmailAddPermit;
import com.io7m.idstore.protocol.user.IdUCommandEmailRemoveBegin;
import com.io7m.idstore.protocol.user.IdUCommandEmailRemoveDeny;
import com.io7m.idstore.protocol.user.IdUCommandEmailRemovePermit;
import com.io7m.idstore.protocol.user.IdUCommandLogin;
import com.io7m.idstore.protocol.user.IdUCommandPasswordUpdate;
import com.io7m.idstore.protocol.user.IdUCommandRealnameUpdate;
import com.io7m.idstore.protocol.user.IdUCommandType;
import com.io7m.idstore.protocol.user.IdUCommandUserSelf;
import com.io7m.idstore.protocol.user.IdUResponseType;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutorType;
import com.io7m.idstore.server.service.sessions.IdSessionUser;

/**
 * A command executor for public commands.
 */

public final class IdUCommandExecutor
  implements IdCommandExecutorType<
  IdSessionUser,
    IdUCommandContext,
    IdUCommandType<? extends IdUResponseType>,
    IdUResponseType>
{
  /**
   * A command executor for public commands.
   */

  public IdUCommandExecutor()
  {

  }

  @Override
  public IdUResponseType execute(
    final IdUCommandContext context,
    final IdUCommandType<? extends IdUResponseType> command)
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

  private static IdUResponseType executeCommand(
    final IdUCommandContext context,
    final IdUCommandType<? extends IdUResponseType> command)
    throws IdCommandExecutionFailure
  {
    if (command instanceof final IdUCommandLogin c) {
      return new IdUCmdLogin().execute(context, c);
    }
    if (command instanceof final IdUCommandUserSelf c) {
      return new IdUCmdUserSelf().execute(context, c);
    }
    if (command instanceof final IdUCommandEmailAddPermit c) {
      return new IdUCmdEmailAddPermit().execute(context, c);
    }
    if (command instanceof final IdUCommandEmailAddDeny c) {
      return new IdUCmdEmailAddDeny().execute(context, c);
    }
    if (command instanceof final IdUCommandEmailAddBegin c) {
      return new IdUCmdEmailAddBegin().execute(context, c);
    }
    if (command instanceof final IdUCommandEmailRemovePermit c) {
      return new IdUCmdEmailRemovePermit().execute(context, c);
    }
    if (command instanceof final IdUCommandEmailRemoveDeny c) {
      return new IdUCmdEmailRemoveDeny().execute(context, c);
    }
    if (command instanceof final IdUCommandEmailRemoveBegin c) {
      return new IdUCmdEmailRemoveBegin().execute(context, c);
    }
    if (command instanceof final IdUCommandRealnameUpdate c) {
      return new IdUCmdRealNameUpdate().execute(context, c);
    }
    if (command instanceof final IdUCommandPasswordUpdate c) {
      return new IdUCmdPasswordUpdate().execute(context, c);
    }

    throw new IllegalStateException();
  }
}

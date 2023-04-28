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

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.error_codes.IdException;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdValidityException;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.api.IdProtocolMessageType;
import com.io7m.idstore.server.controller.command_exec.IdCommandContext;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutorType;
import com.io7m.idstore.server.security.IdSecurityException;
import com.io7m.idstore.server.service.sessions.IdSessionAdmin;

import java.util.Objects;

/**
 * The abstract base command class.
 *
 * @param <T> The type of command contexts
 * @param <C> The type of accepted commands
 * @param <R> The type of responses
 */

public abstract class IdACmdAbstract<
  T extends IdCommandContext<R, IdSessionAdmin>,
  C extends IdProtocolMessageType,
  R extends IdProtocolMessageType>
  implements IdCommandExecutorType<IdSessionAdmin, T, C, R>
{
  protected IdACmdAbstract()
  {

  }

  @Override
  public final R execute(
    final T context,
    final C command)
    throws IdCommandExecutionFailure
  {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(command, "command");

    try {
      return this.executeActual(context, command);
    } catch (final IdValidityException e) {
      throw context.failValidity(e);
    } catch (final IdSecurityException e) {
      throw context.failSecurity(e);
    } catch (final IdDatabaseException e) {
      throw context.failDatabase(e);
    } catch (final IdPasswordException e) {
      throw context.failPassword(e);
    } catch (final IdProtocolException e) {
      throw context.failProtocol(e);
    } catch (final IdException e) {
      throw new IdCommandExecutionFailure(
        e.getMessage(),
        e,
        e.errorCode(),
        e.attributes(),
        e.remediatingAction(),
        context.requestId(),
        500
      );
    }
  }

  protected abstract R executeActual(
    T context,
    C command)
    throws IdValidityException, IdException, IdCommandExecutionFailure;
}

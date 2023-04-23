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

package com.io7m.idstore.user_client.internal;

import com.io7m.hibiscus.api.HBResultType;
import com.io7m.idstore.protocol.user.IdUCommandType;
import com.io7m.idstore.protocol.user.IdUResponseError;
import com.io7m.idstore.protocol.user.IdUResponseType;
import com.io7m.idstore.user_client.api.IdUClientCredentials;

/**
 * A versioned protocol handler.
 */

public interface IdUHandlerType
{
  /**
   * Poll the server for events.
   */

  void pollEvents();

  /**
   * Execute the given command.
   *
   * @param command The command
   * @param <R>     The response type
   *
   * @return The response
   *
   * @throws InterruptedException On interruption
   */

  <R extends IdUResponseType>
  HBResultType<R, IdUResponseError>
  executeCommand(IdUCommandType<R> command)
    throws InterruptedException;

  /**
   * @return {@code true} if this handler is connected (logged in)
   */

  boolean isConnected();

  /**
   * Execute the login process.
   *
   * @param credentials The credentials
   *
   * @return The result
   *
   * @throws InterruptedException On interruption
   */

  HBResultType<IdUNewHandler, IdUResponseError> login(
    IdUClientCredentials credentials)
    throws InterruptedException;
}

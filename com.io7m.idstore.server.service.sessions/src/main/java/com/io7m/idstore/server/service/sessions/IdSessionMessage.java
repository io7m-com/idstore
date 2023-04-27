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

package com.io7m.idstore.server.service.sessions;

import java.util.Objects;
import java.util.UUID;

/**
 * A message that will be displayed on the message screen.
 *
 * @param requestId     The request ID
 * @param isError       {@code true} if the message is an error
 * @param isServerError {@code true} if the error is a server-side error and not
 *                      the fault of the user
 * @param message       The message
 * @param messageTitle  The message title
 * @param returnTo      The path to which to return
 */

public record IdSessionMessage(
  UUID requestId,
  boolean isError,
  boolean isServerError,
  String messageTitle,
  String message,
  String returnTo)
{
  /**
   * A message that will be displayed on the message screen.
   *
   * @param requestId     The request ID
   * @param isError       {@code true} if the message is an error
   * @param isServerError {@code true} if the error is a server-side error and
   *                      not the fault of the user
   * @param message       The message
   * @param messageTitle  The message title
   * @param returnTo      The path to which to return
   */

  public IdSessionMessage
  {
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(messageTitle, "messageTitle");
    Objects.requireNonNull(message, "message");
    Objects.requireNonNull(returnTo, "returnTo");
  }
}

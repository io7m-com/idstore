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

package com.io7m.idstore.server.service.templating;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Data for the generic message screen template.
 *
 * @param htmlTitle       The page title
 * @param pageHeaderTitle The page header title
 * @param requestId       The request ID
 * @param isError         {@code true} if the message is an error
 * @param isServerError   {@code true} if the error is a server-side error and
 *                        not the fault of the user
 * @param message         The message
 * @param messageTitle    The message title
 * @param returnTo        The path to which to return
 */

public record IdFMMessageData(
  String htmlTitle,
  String pageHeaderTitle,
  UUID requestId,
  boolean isError,
  boolean isServerError,
  String messageTitle,
  String message,
  String returnTo)
  implements IdFMDataModelType
{
  /**
   * Data for the generic message screen template.
   *
   * @param htmlTitle       The page title
   * @param pageHeaderTitle The page header title
   * @param requestId       The request ID
   * @param isError         {@code true} if the message is an error
   * @param isServerError   {@code true} if the error is a server-side error and
   *                        not the fault of the user
   * @param message         The message
   * @param messageTitle    The message title
   * @param returnTo        The path to which to return
   */

  public IdFMMessageData
  {
    Objects.requireNonNull(htmlTitle, "htmlTitle");
    Objects.requireNonNull(pageHeaderTitle, "pageHeaderTitle");
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(messageTitle, "messageTitle");
    Objects.requireNonNull(message, "message");
    Objects.requireNonNull(returnTo, "returnTo");
  }

  @Override
  public Map<String, Object> toTemplateHash()
  {
    final var m = new HashMap<String, Object>();
    m.put("htmlTitle", this.htmlTitle);
    m.put("pageHeaderTitle", this.pageHeaderTitle);
    m.put("requestId", this.requestId);
    m.put("messageIsError", this.isError);
    m.put("messageIsServerError", this.isServerError);
    m.put("messageTitle", this.messageTitle);
    m.put("message", this.message);
    m.put("returnTo", this.returnTo);
    return m;
  }
}

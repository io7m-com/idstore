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


package com.io7m.idstore.protocol.user_v1;

import java.util.UUID;

/**
 * The type of API v1 responses.
 */

public sealed interface IdU1ResponseType
  extends IdU1MessageType
  permits IdU1ResponseEmailAddBegin,
  IdU1ResponseEmailAddDeny,
  IdU1ResponseEmailAddPermit,
  IdU1ResponseEmailRemoveBegin,
  IdU1ResponseEmailRemoveDeny,
  IdU1ResponseEmailRemovePermit,
  IdU1ResponseError,
  IdU1ResponseLogin,
  IdU1ResponseUserSelf
{
  /**
   * @return The server-assigned request ID
   */

  UUID requestId();
}

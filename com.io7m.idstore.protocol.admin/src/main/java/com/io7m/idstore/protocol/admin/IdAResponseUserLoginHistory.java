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

package com.io7m.idstore.protocol.admin;

import com.io7m.idstore.model.IdLogin;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A response to {@link IdACommandUserLoginHistory}.
 *
 * @param requestId The request ID
 * @param history   The history
 */

public record IdAResponseUserLoginHistory(
  UUID requestId,
  List<IdLogin> history)
  implements IdAResponseType
{
  /**
   * A response to {@link IdACommandUserLoginHistory}.
   */

  public IdAResponseUserLoginHistory
  {
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(history, "history");
  }
}

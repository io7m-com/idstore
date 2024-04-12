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


package com.io7m.idstore.server.service.reqlimit;

import com.io7m.idstore.error_codes.IdException;
import com.io7m.idstore.error_codes.IdStandardErrorCodes;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * An exception indicating that a request size limit was exceeded.
 */

public final class IdRequestLimitExceeded extends IdException
{
  private final long sizeLimit;
  private final long sizeProvided;

  /**
   * An exception indicating that a request size limit was exceeded.
   *
   * @param message        The message
   * @param inSizeLimit    The limit
   * @param inSizeProvided The provided size
   */

  public IdRequestLimitExceeded(
    final String message,
    final long inSizeLimit,
    final long inSizeProvided)
  {
    super(
      Objects.requireNonNull(message, "message"),
      IdStandardErrorCodes.HTTP_SIZE_LIMIT,
      Map.ofEntries(
        Map.entry("Size Limit", Long.toUnsignedString(inSizeLimit)),
        Map.entry("Size", Long.toUnsignedString(inSizeProvided))
      ),
      Optional.empty()
    );
    this.sizeLimit = inSizeLimit;
    this.sizeProvided = inSizeProvided;
  }

  /**
   * @return The size limit
   */

  public long sizeLimit()
  {
    return this.sizeLimit;
  }

  /**
   * @return The size of the request
   */

  public long sizeProvided()
  {
    return this.sizeProvided;
  }
}

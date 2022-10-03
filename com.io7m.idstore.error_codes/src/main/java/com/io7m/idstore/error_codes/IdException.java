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


package com.io7m.idstore.error_codes;

import java.util.Objects;

/**
 * The base type of exceptions.
 */

public class IdException extends Exception
{
  private final IdErrorCode errorCode;

  /**
   * Construct an exception.
   *
   * @param inErrorCode The error code
   * @param message     The message
   */

  public IdException(
    final IdErrorCode inErrorCode,
    final String message)
  {
    super(message);
    this.errorCode = Objects.requireNonNull(inErrorCode, "errorCode");
  }

  /**
   * Construct an exception.
   *
   * @param inErrorCode The error code
   * @param message     The message
   * @param cause       The cause
   */

  public IdException(
    final IdErrorCode inErrorCode,
    final String message,
    final Throwable cause)
  {
    super(message, cause);
    this.errorCode = Objects.requireNonNull(inErrorCode, "errorCode");
  }

  /**
   * Construct an exception.
   *
   * @param inErrorCode The error code
   * @param cause       The cause
   */

  public IdException(
    final IdErrorCode inErrorCode,
    final Throwable cause)
  {
    super(cause);
    this.errorCode = Objects.requireNonNull(inErrorCode, "errorCode");
  }

  /**
   * @return The error code
   */

  public final IdErrorCode errorCode()
  {
    return this.errorCode;
  }
}

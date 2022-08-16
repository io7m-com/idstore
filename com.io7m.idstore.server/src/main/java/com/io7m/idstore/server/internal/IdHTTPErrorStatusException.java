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


package com.io7m.idstore.server.internal;

import com.io7m.idstore.error_codes.IdErrorCode;

import java.util.Objects;

/**
 * An exception with an associated error code and HTTP status code.
 */

public final class IdHTTPErrorStatusException extends Exception
{
  private final int statusCode;
  private final IdErrorCode errorCode;

  /**
   * Construct an exception.
   *
   * @param inStatusCode The HTTP status code
   * @param inErrorCode  The error code
   * @param inMessage    The error message
   */

  public IdHTTPErrorStatusException(
    final int inStatusCode,
    final IdErrorCode inErrorCode,
    final String inMessage)
  {
    super(Objects.requireNonNull(inMessage, "message"));
    this.statusCode = inStatusCode;
    this.errorCode = Objects.requireNonNull(inErrorCode, "errorCode");
  }

  /**
   * Construct an exception.
   *
   * @param inStatusCode The HTTP status code
   * @param inErrorCode  The error code
   * @param inMessage    The error message
   * @param cause        The cause
   */


  public IdHTTPErrorStatusException(
    final int inStatusCode,
    final IdErrorCode inErrorCode,
    final String inMessage,
    final Throwable cause)
  {
    super(
      Objects.requireNonNull(inMessage, "message"),
      Objects.requireNonNull(cause, "cause"));
    this.statusCode = inStatusCode;
    this.errorCode = Objects.requireNonNull(inErrorCode, "errorCode");
  }

  /**
   * Construct an exception.
   *
   * @param inStatusCode The HTTP status code
   * @param inErrorCode  The error code
   * @param cause        The cause
   */

  public IdHTTPErrorStatusException(
    final int inStatusCode,
    final IdErrorCode inErrorCode,
    final Throwable cause)
  {
    super(Objects.requireNonNull(cause, "cause"));
    this.statusCode = inStatusCode;
    this.errorCode = Objects.requireNonNull(inErrorCode, "errorCode");
  }

  /**
   * @return The HTTP status code
   */

  public int statusCode()
  {
    return this.statusCode;
  }

  /**
   * @return The error code
   */

  public IdErrorCode errorCode()
  {
    return this.errorCode;
  }
}

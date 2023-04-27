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


package com.io7m.idstore.server.http;

import com.io7m.idstore.error_codes.IdErrorCode;
import com.io7m.idstore.error_codes.IdException;

import java.util.Map;
import java.util.Optional;

/**
 * An exception with an associated error code and HTTP status code.
 */

public final class IdHTTPErrorStatusException extends IdException
{
  private final int httpStatusCode;

  /**
   * Construct an exception.
   *
   * @param message             The message
   * @param inErrorCode         The error code
   * @param inAttributes        The error attributes
   * @param inRemediatingAction The remediating action, if any
   * @param inHttpStatusCode    The HTTP status code
   */

  public IdHTTPErrorStatusException(
    final String message,
    final IdErrorCode inErrorCode,
    final Map<String, String> inAttributes,
    final Optional<String> inRemediatingAction,
    final int inHttpStatusCode)
  {
    super(message, inErrorCode, inAttributes, inRemediatingAction);
    this.httpStatusCode = inHttpStatusCode;
  }

  /**
   * Construct an exception.
   *
   * @param message             The message
   * @param cause               The cause
   * @param inErrorCode         The error code
   * @param inAttributes        The error attributes
   * @param inRemediatingAction The remediating action, if any
   * @param inHttpStatusCode    The HTTP status code
   */

  public IdHTTPErrorStatusException(
    final String message,
    final Throwable cause,
    final IdErrorCode inErrorCode,
    final Map<String, String> inAttributes,
    final Optional<String> inRemediatingAction,
    final int inHttpStatusCode)
  {
    super(message, cause, inErrorCode, inAttributes, inRemediatingAction);
    this.httpStatusCode = inHttpStatusCode;
  }

  /**
   * @return The HTTP status code
   */

  public int httpStatusCode()
  {
    return this.httpStatusCode;
  }
}

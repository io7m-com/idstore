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

package com.io7m.idstore.server.controller.command_exec;

import com.io7m.idstore.error_codes.IdErrorCode;

import java.util.Objects;
import java.util.UUID;

/**
 * A failure to execute a command.
 */

public final class IdCommandExecutionFailure extends Exception
{
  private final UUID requestId;
  private final int httpStatusCode;
  private final IdErrorCode errorCode;

  /**
   * Construct an exception.
   *
   * @param message          The message
   * @param inRequestId      The request ID
   * @param inHttpStatusCode The HTTP status code
   * @param inErrorCode      The error code
   */

  public IdCommandExecutionFailure(
    final String message,
    final UUID inRequestId,
    final int inHttpStatusCode,
    final IdErrorCode inErrorCode)
  {
    super(Objects.requireNonNull(message, "message"));

    this.requestId =
      Objects.requireNonNull(inRequestId, "inRequestId");
    this.httpStatusCode =
      inHttpStatusCode;
    this.errorCode =
      Objects.requireNonNull(inErrorCode, "errorCode");
  }

  /**
   * @return The request ID
   */

  public UUID requestId()
  {
    return this.requestId;
  }

  /**
   * @return The HTTP status code
   */

  public int httpStatusCode()
  {
    return this.httpStatusCode;
  }

  /**
   * @return The error code
   */

  public IdErrorCode errorCode()
  {
    return this.errorCode;
  }

  /**
   * Construct an exception.
   *
   * @param message          The message
   * @param cause            The cause
   * @param inRequestId      The request ID
   * @param inHttpStatusCode The HTTP status code
   * @param inErrorCode      The error code
   */

  public IdCommandExecutionFailure(
    final String message,
    final Throwable cause,
    final UUID inRequestId,
    final int inHttpStatusCode,
    final IdErrorCode inErrorCode)
  {
    super(
      Objects.requireNonNull(message, "message"),
      Objects.requireNonNull(cause, "cause")
    );

    this.requestId =
      Objects.requireNonNull(inRequestId, "requestId");
    this.httpStatusCode =
      inHttpStatusCode;
    this.errorCode =
      Objects.requireNonNull(inErrorCode, "errorCode");
  }
}

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

package com.io7m.idstore.user_client.api;

import com.io7m.idstore.error_codes.IdErrorCode;
import com.io7m.idstore.error_codes.IdException;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The type of client exceptions.
 */

public final class IdUClientException extends IdException
{
  private final String reason;
  private final Optional<UUID> requestId;

  /**
   * Construct an exception.
   *
   * @param inRequestId The request ID
   * @param errorCode   The error code
   * @param message     The message
   * @param inReason    The error reason
   */

  public IdUClientException(
    final Optional<UUID> inRequestId,
    final IdErrorCode errorCode,
    final String message,
    final String inReason)
  {
    super(errorCode, Objects.requireNonNull(message, "message"));
    this.requestId =
      Objects.requireNonNull(inRequestId, "requestId");
    this.reason =
      Objects.requireNonNull(inReason, "reason");
  }

  /**
   * Construct an exception.
   *
   * @param inRequestId The request ID
   * @param errorCode   The error code
   * @param message     The message
   * @param cause       The cause
   * @param inReason    The error reason
   */

  public IdUClientException(
    final Optional<UUID> inRequestId,
    final IdErrorCode errorCode,
    final String message,
    final Throwable cause,
    final String inReason)
  {
    super(
      errorCode,
      Objects.requireNonNull(message, "message"),
      Objects.requireNonNull(cause, "cause")
    );
    this.requestId =
      Objects.requireNonNull(inRequestId, "requestId");
    this.reason =
      Objects.requireNonNull(inReason, "reason");
  }

  /**
   * Construct an exception.
   *
   * @param inRequestId The request ID
   * @param errorCode   The error code
   * @param cause       The cause
   * @param inReason    The error reason
   */

  public IdUClientException(
    final Optional<UUID> inRequestId,
    final IdErrorCode errorCode,
    final Throwable cause,
    final String inReason)
  {
    super(errorCode, Objects.requireNonNull(cause, "cause"));
    this.requestId =
      Objects.requireNonNull(inRequestId, "requestId");
    this.reason =
      Objects.requireNonNull(inReason, "reason");
  }

  /**
   * @return The ID associated with the request, if the server returned one
   */

  public Optional<UUID> requestId()
  {
    return this.requestId;
  }

  /**
   * @return The error reason
   */

  public String reason()
  {
    return this.reason;
  }
}

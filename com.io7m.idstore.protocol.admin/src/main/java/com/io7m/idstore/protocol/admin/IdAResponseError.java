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

package com.io7m.idstore.protocol.admin;

import com.io7m.idstore.error_codes.IdErrorCode;
import com.io7m.seltzer.api.SStructuredErrorType;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * An error response.
 *
 * @param messageId         The message ID
 * @param correlationId     The message correlation ID
 * @param errorCode         The error code
 * @param message           The humanly-readable error message
 * @param attributes        The error attributes
 * @param remediatingAction The remediating action, if any
 * @param blame             The blame assignment
 */

public record IdAResponseError(
  UUID messageId,
  UUID correlationId,
  String message,
  IdErrorCode errorCode,
  Map<String, String> attributes,
  Optional<String> remediatingAction,
  IdAResponseBlame blame)
  implements IdAResponseType, SStructuredErrorType<IdErrorCode>
{
  /**
   * An error response.
   *
   * @param messageId         The message ID
   * @param correlationId     The message correlation ID
   * @param errorCode         The error code
   * @param message           The humanly-readable error message
   * @param attributes        The error attributes
   * @param remediatingAction The remediating action, if any
   * @param blame             The blame assignment
   */

  public IdAResponseError
  {
    Objects.requireNonNull(messageId, "messageId");
    Objects.requireNonNull(correlationId, "correlationId");
    Objects.requireNonNull(message, "message");
    Objects.requireNonNull(errorCode, "errorCode");
    Objects.requireNonNull(attributes, "attributes");
    Objects.requireNonNull(remediatingAction, "remediatingAction");
    Objects.requireNonNull(blame, "blame");
  }

  /**
   * @return The associated exception, if any
   */

  @Override
  public Optional<Throwable> exception()
  {
    return Optional.empty();
  }
}

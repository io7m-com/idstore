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

import com.io7m.idstore.server.http.IdHTTPServerRequests;
import com.io7m.repetoir.core.RPServiceType;
import io.helidon.webserver.http.ServerRequest;
import org.apache.commons.io.input.BoundedInputStream;

import java.io.InputStream;
import java.util.Objects;
import java.util.function.Function;

/**
 * Methods to handle request size limits.
 */

public final class IdRequestLimits implements RPServiceType
{
  private final Function<Long, String> requestTooLargeMessage;

  /**
   * Methods to handle request size limits.
   *
   * @param inRequestTooLargeMessage A function that formats a message
   */

  public IdRequestLimits(
    final Function<Long, String> inRequestTooLargeMessage)
  {
    this.requestTooLargeMessage =
      Objects.requireNonNull(
        inRequestTooLargeMessage, "requestTooLargeMessage");
  }

  /**
   * Bound the given servlet request to the given maximum size, raising an
   * exception if the incoming Content-Length is larger than this size.
   *
   * @param request The request
   * @param maximum The maximum size
   *
   * @return A bounded input stream
   *
   * @throws IdRequestLimitExceeded On errors
   */

  public InputStream boundedMaximumInput(
    final ServerRequest request,
    final long maximum)
    throws IdRequestLimitExceeded
  {
    final long size;
    final var specifiedLength =
      IdHTTPServerRequests.contentLength(request);

    if (specifiedLength == -1L) {
      size = maximum;
    } else {
      if (Long.compareUnsigned(specifiedLength, maximum) > 0) {
        throw new IdRequestLimitExceeded(
          this.requestTooLargeMessage.apply(
            Long.valueOf(specifiedLength)
          ),
          maximum,
          specifiedLength
        );
      }
      size = specifiedLength;
    }

    return new BoundedInputStream(
      request.content().inputStream(),
      size
    );
  }

  @Override
  public String description()
  {
    return "Request limiting service.";
  }

  @Override
  public String toString()
  {
    return "[IdRequestLimits 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode(), 16));
  }
}

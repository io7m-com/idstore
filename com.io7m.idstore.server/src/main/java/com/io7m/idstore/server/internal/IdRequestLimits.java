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

import com.io7m.idstore.services.api.IdServiceType;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.io.input.BoundedInputStream;
import org.eclipse.jetty.http.HttpStatus;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.HTTP_SIZE_LIMIT;

/**
 * Methods to handle request size limits.
 */

public final class IdRequestLimits implements IdServiceType
{
  private final IdServerStrings strings;

  /**
   * Methods to handle request size limits.
   *
   * @param inStrings Server string resources
   */

  public IdRequestLimits(
    final IdServerStrings inStrings)
  {
    this.strings = Objects.requireNonNull(inStrings, "strings");
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
   * @throws IOException                On errors
   * @throws IdHTTPErrorStatusException On errors
   */

  public InputStream boundedMaximumInput(
    final HttpServletRequest request,
    final int maximum)
    throws IOException, IdHTTPErrorStatusException
  {
    final int size;
    final var specifiedLength = request.getContentLength();
    if (specifiedLength == -1) {
      size = maximum;
    } else {
      if (Integer.compareUnsigned(specifiedLength, maximum) > 0) {
        throw new IdHTTPErrorStatusException(
          HttpStatus.PAYLOAD_TOO_LARGE_413,
          HTTP_SIZE_LIMIT,
          this.strings.format(
            "requestTooLarge",
            Integer.toUnsignedString(specifiedLength))
        );
      }
      size = specifiedLength;
    }
    return new BoundedInputStream(
      request.getInputStream(),
      Integer.toUnsignedLong(size)
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
      .formatted(Long.toUnsignedString(this.hashCode()));
  }
}

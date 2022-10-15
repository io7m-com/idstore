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

package com.io7m.idstore.admin_client.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

/**
 * Functions to decompress responses.
 */

public final class IdACompression
{
  private IdACompression()
  {

  }

  /**
   * Decompress the response if necessary.
   *
   * @param response        The response
   * @param responseHeaders The response headers
   *
   * @return The decompressed bytes
   *
   * @throws IOException On errors
   */

  public static byte[] decompressResponse(
    final HttpResponse<byte[]> response,
    final HttpHeaders responseHeaders)
    throws IOException
  {
    final var encoding =
      responseHeaders.firstValue("Content-Encoding");

    final byte[] body;
    if (Objects.equals(encoding, Optional.of("gzip"))) {
      try (var s = new GZIPInputStream(new ByteArrayInputStream(response.body()))) {
        body = s.readAllBytes();
      }
    } else {
      body = response.body();
    }
    return body;
  }
}

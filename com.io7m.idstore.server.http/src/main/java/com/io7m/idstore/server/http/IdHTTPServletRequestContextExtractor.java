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

import io.opentelemetry.context.propagation.TextMapGetter;
import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;

/**
 * A propagator that can extract fields from a servlet request.
 */

public final class IdHTTPServletRequestContextExtractor
  implements TextMapGetter<HttpServletRequest>
{
  private static final TextMapGetter<HttpServletRequest> INSTANCE =
    new IdHTTPServletRequestContextExtractor();

  /**
   * @return A propagator that can extract fields from a servlet request.
   */

  public static TextMapGetter<HttpServletRequest> instance()
  {
    return INSTANCE;
  }

  private IdHTTPServletRequestContextExtractor()
  {

  }

  @Override
  public Iterable<String> keys(
    final HttpServletRequest request)
  {
    final var results = new ArrayList<String>();
    final var names = request.getHeaderNames();
    while (names.hasMoreElements()) {
      results.add(names.nextElement());
    }
    return results;
  }

  @Override
  public String get(
    final HttpServletRequest request,
    final String name)
  {
    return request.getHeader(name);
  }
}

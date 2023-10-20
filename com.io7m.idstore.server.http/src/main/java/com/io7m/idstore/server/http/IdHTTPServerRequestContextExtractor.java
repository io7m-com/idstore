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

import io.helidon.http.HeaderNames;
import io.helidon.webserver.http.ServerRequest;
import io.opentelemetry.context.propagation.TextMapGetter;

import java.util.ArrayList;

/**
 * A propagator that can extract fields from a servlet request.
 */

public final class IdHTTPServerRequestContextExtractor
  implements TextMapGetter<ServerRequest>
{
  private static final TextMapGetter<ServerRequest> INSTANCE =
    new IdHTTPServerRequestContextExtractor();

  /**
   * @return A propagator that can extract fields from a servlet request.
   */

  public static TextMapGetter<ServerRequest> instance()
  {
    return INSTANCE;
  }

  private IdHTTPServerRequestContextExtractor()
  {

  }

  @Override
  public Iterable<String> keys(
    final ServerRequest request)
  {
    final var results =
      new ArrayList<String>();
    final var headers =
      request.headers();
    final var iterator =
      headers.iterator();

    while (iterator.hasNext()) {
      final var header = iterator.next();
      results.add(header.name());
    }
    return results;
  }

  @Override
  public String get(
    final ServerRequest request,
    final String name)
  {
    return request.headers().get(HeaderNames.create(name)).get();
  }
}

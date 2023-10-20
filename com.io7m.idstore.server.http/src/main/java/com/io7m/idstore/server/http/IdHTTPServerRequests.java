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

import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Functions over server requests.
 */

public final class IdHTTPServerRequests
{
  private IdHTTPServerRequests()
  {

  }

  /**
   * Obtain the content length for the given request, or -1 if none exists.
   *
   * @param request The request
   *
   * @return The content length
   */

  public static long contentLength(
    final ServerRequest request)
  {
    try {
      final var headers = request.headers();
      return headers.get(HeaderNames.CONTENT_LENGTH).getLong();
    } catch (final NoSuchElementException | UnsupportedOperationException e) {
      return -1L;
    }
  }

  /**
   * Obtain the user agent for the given request, or the empty string if none exists.
   *
   * @param request The request
   *
   * @return The user agent
   */

  public static String userAgent(
    final ServerRequest request)
  {
    try {
      final var headers = request.headers();
      return headers.get(HeaderNames.USER_AGENT).getString();
    } catch (final NoSuchElementException | UnsupportedOperationException e) {
      return "";
    }
  }

  /**
   * Obtain the remote address for the given request. The function takes into
   * account headers such as "X-Forwarded-For".
   *
   * @param request The request
   *
   * @return The remote address
   */

  public static String remoteAddress(
    final ServerRequest request)
  {
    try {
      final var headers = request.headers();
      return headers.get(HeaderNames.X_FORWARDED_HOST).getString();
    } catch (final NoSuchElementException | UnsupportedOperationException e) {
      final var peer = request.remotePeer();
      return "%s:%d".formatted(peer.host(), Integer.valueOf(peer.port()));
    }
  }

  /**
   * Obtain the value of the given query parameter.
   *
   * @param request The request
   * @param name    The parameter name
   *
   * @return The parameter value (or null)
   */

  public static String parameter(
    final ServerRequest request,
    final String name)
  {
    try {
      return request.query().get(name);
    } catch (final NoSuchElementException | UnsupportedOperationException e) {
      return null;
    }
  }

  /**
   * Obtain the value of the given query parameter.
   *
   * @param request The request
   * @param name    The parameter name
   *
   * @return The parameter value
   */

  public static Optional<String> parameterOrEmpty(
    final ServerRequest request,
    final String name)
  {
    return Optional.ofNullable(parameter(request, name))
      .flatMap(x -> {
        if (x.trim().isEmpty()) {
          return Optional.empty();
        }
        return Optional.of(x);
      });
  }
}

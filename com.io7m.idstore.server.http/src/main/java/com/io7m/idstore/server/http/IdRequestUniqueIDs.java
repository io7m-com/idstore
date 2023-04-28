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

import com.io7m.idstore.server.service.clock.IdServerClock;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.Request;

import java.util.Objects;
import java.util.UUID;

/**
 * A request listener that appends a probably-unique ID to every request.
 */

public final class IdRequestUniqueIDs implements HttpChannel.Listener
{
  private final IdServerClock clock;

  /**
   * A request listener that appends a probably-unique ID to every request.
   *
   * @param services A service directory
   */

  public IdRequestUniqueIDs(
    final RPServiceDirectoryType services)
  {
    Objects.requireNonNull(services, "services");

    this.clock = services.requireService(IdServerClock.class);
  }

  /**
   * @return The attribute name used to hold a unique ID
   */

  public static String uniqueRequestIDKey()
  {
    return "com.io7m.idstore.server.requestId";
  }

  /**
   * @return The attribute name used to hold a start time
   */

  public static String uniqueRequestTimeKey()
  {
    return "com.io7m.idstore.server.requestTimeStart";
  }

  /**
   * Get the unique ID for the given request.
   *
   * @param request The request
   *
   * @return The unique ID
   */

  public static UUID requestIdFor(
    final HttpServletRequest request)
  {
    final var id = (UUID) request.getAttribute(uniqueRequestIDKey());
    if (id == null) {
      throw new IllegalStateException("Missing request ID");
    }
    return id;
  }

  @Override
  public void onRequestBegin(
    final Request request)
  {
    request.setAttribute(uniqueRequestIDKey(), UUID.randomUUID());
    request.setAttribute(uniqueRequestTimeKey(), this.clock.nowPrecise());
  }
}

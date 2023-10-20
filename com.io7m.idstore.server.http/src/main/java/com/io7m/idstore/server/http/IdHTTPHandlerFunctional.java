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
import io.helidon.http.Status;
import io.helidon.webserver.http.Handler;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;

import static com.io7m.idstore.model.IdVersion.MAIN_BUILD;
import static com.io7m.idstore.model.IdVersion.MAIN_VERSION;

/**
 * A functional servlet.
 */

public abstract class IdHTTPHandlerFunctional implements Handler
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdHTTPHandlerFunctional.class);

  private final IdHTTPHandlerFunctionalCoreType core;

  /**
   * A functional servlet.
   *
   * @param inCore The functional core
   */

  public IdHTTPHandlerFunctional(
    final IdHTTPHandlerFunctionalCoreType inCore)
  {
    this.core = Objects.requireNonNull(inCore, "core");
  }

  @Override
  public final void handle(
    final ServerRequest request,
    final ServerResponse response)
  {
    try {
      final var userAgent =
        IdHTTPServerRequests.userAgent(request);
      final var remoteAddr =
        IdHTTPServerRequests.remoteAddress(request);

      final var information =
        new IdHTTPRequestInformation(
          UUID.randomUUID(),
          userAgent,
          remoteAddr
        );

      final var output =
        this.core.execute(request, information);

      response.header(
        "Server",
        String.format("idstore %s %s", MAIN_VERSION, MAIN_BUILD)
      );

      for (final var cookie : output.cookies()) {
        response.headers()
          .addCookie(
            cookie.name(),
            cookie.value(),
            cookie.validity()
          );
      }

      switch (output) {
        case final IdHTTPResponseFixedSize fixed -> {
          response.status(fixed.statusCode());
          response.header(HeaderNames.CONTENT_TYPE, fixed.contentType());
          response.send(fixed.data());
        }
        case final IdHTTPResponseRedirect target -> {
          response.header(HeaderNames.LOCATION, target.path());
          response.status(Status.FOUND_302);
          response.send();
        }
      }
    } catch (final Throwable e) {
      LOG.debug("Uncaught exception: ", e);
      throw e;
    }
  }
}

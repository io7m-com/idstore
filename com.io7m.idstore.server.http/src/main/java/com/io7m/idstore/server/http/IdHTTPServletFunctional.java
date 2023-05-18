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

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

/**
 * A functional servlet.
 */

public abstract class IdHTTPServletFunctional extends HttpServlet
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdHTTPServletFunctional.class);

  private final IdHTTPServletFunctionalCoreType core;

  /**
   * A functional servlet.
   *
   * @param inCore The functional core
   */

  public IdHTTPServletFunctional(
    final IdHTTPServletFunctionalCoreType inCore)
  {
    this.core = Objects.requireNonNull(inCore, "core");
  }

  @Override
  protected final void service(
    final HttpServletRequest request,
    final HttpServletResponse response)
    throws ServletException, IOException
  {
    try {
      final var userAgent =
        IdRequestUserAgents.requestUserAgent(request);

      final var information =
        new IdHTTPServletRequestInformation(
          UUID.randomUUID(),
          userAgent,
          request.getRemoteAddr()
        );

      final var output =
        this.core.execute(request, information);

      if (output instanceof final IdHTTPServletResponseFixedSize fixed) {
        response.setStatus(fixed.statusCode());
        response.setHeader("Content-Type", fixed.contentType());
        response.setContentLength(fixed.data().length);
        try (var stream = response.getOutputStream()) {
          stream.write(fixed.data());
        }
      } else if (output instanceof final IdHTTPServletResponseRedirect target) {
        response.sendRedirect(target.path());
      } else {
        throw new IllegalStateException(
          "Unrecognized response type: %s".formatted(output)
        );
      }
    } catch (final Throwable e) {
      LOG.debug("uncaught exception: ", e);
      throw e;
    }
  }

  @Override
  public final void service(
    final ServletRequest request,
    final ServletResponse response)
    throws ServletException, IOException
  {
    if (request instanceof final HttpServletRequest httpRequest
        && response instanceof final HttpServletResponse httpResponse) {
      this.service(httpRequest, httpResponse);
      return;
    }

    throw new ServletException("Non-HTTP request or response");
  }
}

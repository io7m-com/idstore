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


package com.io7m.idstore.server.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.jetty.server.Request;
import org.slf4j.MDC;

import java.util.Objects;
import java.util.Optional;

import static com.io7m.idstore.server.internal.IdServerRequestDecoration.requestIdFor;
import static com.io7m.idstore.server.logging.IdLoggingHTTPRequestLayout.MDC_REQUEST_DOMAIN;
import static com.io7m.idstore.server.logging.IdLoggingHTTPRequestLayout.MDC_REQUEST_ID;
import static com.io7m.idstore.server.logging.IdLoggingHTTPRequestLayout.MDC_REQUEST_METHOD;
import static com.io7m.idstore.server.logging.IdLoggingHTTPRequestLayout.MDC_REQUEST_REMOTE_HOST;
import static com.io7m.idstore.server.logging.IdLoggingHTTPRequestLayout.MDC_REQUEST_REMOTE_PORT;
import static com.io7m.idstore.server.logging.IdLoggingHTTPRequestLayout.MDC_REQUEST_URI;
import static com.io7m.idstore.server.logging.IdLoggingHTTPRequestLayout.MDC_REQUEST_USER_AGENT;

/**
 * An MDC processor that adds MDC properties for an HTTP request.
 */

public final class IdServerMDCRequestProcessor
  implements IdServerMDCProcessorType
{
  private final Request request;

  private IdServerMDCRequestProcessor(
    final Request inRequest)
  {
    this.request = Objects.requireNonNull(inRequest, "request");
  }

  /**
   * An MDC processor that adds MDC properties for an HTTP request.
   *
   * @param inRequest The request
   *
   * @return The processor
   */

  public static IdServerMDCProcessorType mdcForRequest(
    final Request inRequest)
  {
    final var p = new IdServerMDCRequestProcessor(inRequest);
    p.process();
    return p;
  }

  /**
   * An MDC processor that adds MDC properties for an HTTP request.
   *
   * @param inRequest The request
   *
   * @return The processor
   */

  public static IdServerMDCProcessorType mdcForRequest(
    final HttpServletRequest inRequest)
  {
    return mdcForRequest((Request) inRequest);
  }

  private void process()
  {
    final var ec = this.request.getErrorContext();
    if (ec != null) {
      final var host = ec.getVirtualServerName();
      if (host != null) {
        MDC.put(MDC_REQUEST_DOMAIN, host);
      }
    }

    Optional.ofNullable(this.request.getHeader("User-Agent"))
      .ifPresent(agent -> MDC.put(MDC_REQUEST_USER_AGENT, agent));

    MDC.put(
      MDC_REQUEST_METHOD,
      this.request.getMethod());
    MDC.put(
      MDC_REQUEST_REMOTE_HOST,
      this.request.getRemoteHost());
    MDC.put(
      MDC_REQUEST_REMOTE_PORT,
      String.valueOf(this.request.getRemotePort()));
    MDC.put(
      MDC_REQUEST_ID,
      requestIdFor(this.request).toString());
    MDC.put(
      MDC_REQUEST_URI,
      this.request.getOriginalURI());
  }

  @Override
  public void close()
    throws RuntimeException
  {
    MDC.remove(MDC_REQUEST_DOMAIN);
    MDC.remove(MDC_REQUEST_METHOD);
    MDC.remove(MDC_REQUEST_REMOTE_HOST);
    MDC.remove(MDC_REQUEST_REMOTE_PORT);
    MDC.remove(MDC_REQUEST_ID);
    MDC.remove(MDC_REQUEST_URI);
    MDC.remove(MDC_REQUEST_USER_AGENT);
  }
}

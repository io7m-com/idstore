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


package com.io7m.idstore.server.internal.common;

import com.io7m.idstore.server.internal.IdRequests;
import com.io7m.idstore.server.internal.IdServerTelemetryService;
import com.io7m.idstore.services.api.IdServiceDirectoryType;
import io.opentelemetry.api.trace.SpanKind;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.Instant;

import static com.io7m.idstore.server.internal.IdServerRequestDecoration.requestIdFor;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_CLIENT_IP;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_METHOD;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_REQUEST_CONTENT_LENGTH;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_RESPONSE_CONTENT_LENGTH;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_STATUS_CODE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_URL;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_USER_AGENT;

/**
 * The base instrumented servlet. The purpose of this servlet is
 * to collect metrics for telemetry.
 */

public abstract class IdCommonInstrumentedServlet extends HttpServlet
{
  private final IdServerTelemetryService telemetry;

  protected IdCommonInstrumentedServlet(
    final IdServiceDirectoryType inServices)
  {
    this.telemetry =
      inServices.requireService(IdServerTelemetryService.class);
  }

  @Override
  public final void service(
    final ServletRequest req,
    final ServletResponse res)
    throws ServletException, IOException
  {
    if (req instanceof HttpServletRequest sr
        && res instanceof HttpServletResponse response) {

      final var tracer =
        this.telemetry.tracer();

      final var span =
        tracer.spanBuilder(sr.getServletPath())
          .setStartTimestamp(Instant.now())
          .setSpanKind(SpanKind.SERVER)
          .setAttribute(HTTP_CLIENT_IP, sr.getRemoteAddr())
          .setAttribute(HTTP_METHOD, sr.getMethod())
          .setAttribute(HTTP_REQUEST_CONTENT_LENGTH, sr.getContentLengthLong())
          .setAttribute(HTTP_USER_AGENT, IdRequests.requestUserAgent(sr))
          .setAttribute(HTTP_URL, sr.getRequestURI())
          .setAttribute("http.request_id", requestIdFor(sr).toString())
          .startSpan();

      try (var ignored = span.makeCurrent()) {
        this.service(sr, response);
        span.setAttribute(HTTP_STATUS_CODE, response.getStatus());
        span.setAttribute(HTTP_RESPONSE_CONTENT_LENGTH, contentLength(response));
      } catch (final Throwable e) {
        span.recordException(e);
        throw e;
      } finally {
        span.end();
      }
      return;
    }

    throw new ServletException("non-HTTP request or response");
  }

  private static long contentLength(
    final HttpServletResponse response)
  {
    try {
      return Long.parseLong(response.getHeader("content-length"));
    } catch (final Exception e) {
      return -1L;
    }
  }
}

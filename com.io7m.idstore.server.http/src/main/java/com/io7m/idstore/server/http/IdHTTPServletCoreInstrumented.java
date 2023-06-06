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

import com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import io.opentelemetry.api.trace.SpanKind;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;
import java.util.Objects;

import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_CLIENT_IP;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_METHOD;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_REQUEST_CONTENT_LENGTH;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_RESPONSE_CONTENT_LENGTH;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_STATUS_CODE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_URL;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_USER_AGENT;

/**
 * A servlet core that executes the given core with instrumentation.
 */

public final class IdHTTPServletCoreInstrumented
  implements IdHTTPServletFunctionalCoreType
{
  private final IdHTTPServletFunctionalCoreType core;
  private final IdServerTelemetryServiceType telemetry;

  private IdHTTPServletCoreInstrumented(
    final RPServiceDirectoryType inServices,
    final IdHTTPServletFunctionalCoreType inCore)
  {
    this.telemetry =
      inServices.requireService(IdServerTelemetryServiceType.class);
    this.core =
      Objects.requireNonNull(inCore, "core");
  }

  /**
   * @param inServices The services
   * @param inCore     The core
   *
   * @return A servlet core that executes the given core with instrumentation
   */

  public static IdHTTPServletFunctionalCoreType withInstrumentation(
    final RPServiceDirectoryType inServices,
    final IdHTTPServletFunctionalCoreType inCore)
  {
    return new IdHTTPServletCoreInstrumented(inServices, inCore);
  }

  @Override
  public IdHTTPServletResponseType execute(
    final HttpServletRequest request,
    final IdHTTPServletRequestInformation information)
  {
    final var tracer =
      this.telemetry.tracer();

    final var span =
      tracer.spanBuilder(request.getServletPath())
        .setStartTimestamp(Instant.now())
        .setSpanKind(SpanKind.SERVER)
        .setAttribute(HTTP_CLIENT_IP, information.remoteAddress())
        .setAttribute(HTTP_METHOD, request.getMethod())
        .setAttribute(
          HTTP_REQUEST_CONTENT_LENGTH,
          request.getContentLengthLong()
        )
        .setAttribute(HTTP_USER_AGENT, information.userAgent())
        .setAttribute(HTTP_URL, request.getRequestURI())
        .setAttribute("http.request_id", information.requestId().toString())
        .startSpan();

    try (var ignored = span.makeCurrent()) {
      final var response = this.core.execute(request, information);
      span.setAttribute(HTTP_STATUS_CODE, response.statusCode());
      response.contentLengthOptional().ifPresent(size -> {
        span.setAttribute(HTTP_RESPONSE_CONTENT_LENGTH, Long.valueOf(size));
      });
      return response;
    } catch (final Throwable e) {
      span.recordException(e);
      throw e;
    } finally {
      span.end();
    }
  }
}

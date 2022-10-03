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


package com.io7m.idstore.server.internal;

import com.io7m.idstore.server.api.IdServerConfiguration;
import com.io7m.idstore.services.api.IdServiceType;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SERVICE_NAME;

/**
 * An OpenTelemetry service.
 */

public final class IdServerTelemetryService
  implements IdServiceType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdServerTelemetryService.class);

  private final OpenTelemetry openTelemetry;
  private final Tracer tracer;

  private IdServerTelemetryService(
    final OpenTelemetry inOpenTelemetry,
    final Tracer inTracer)
  {
    this.openTelemetry =
      Objects.requireNonNull(inOpenTelemetry, "openTelemetry");
    this.tracer =
      Objects.requireNonNull(inTracer, "tracer");
  }

  /**
   * @return A completely no-op service
   */

  public static IdServerTelemetryService noop()
  {
    final var noop = OpenTelemetry.noop();
    return new IdServerTelemetryService(
      noop,
      noop.getTracer("noop")
    );
  }

  /**
   * @return The main tracer
   */

  public Tracer tracer()
  {
    return this.tracer;
  }

  /**
   * @return The OpenTelemetry instance
   */

  public OpenTelemetry openTelemetry()
  {
    return this.openTelemetry;
  }

  /**
   * Create a telemetry service.
   *
   * @param configuration The server configuration
   *
   * @return The service
   */

  public static IdServerTelemetryService create(
    final IdServerConfiguration configuration)
  {
    Objects.requireNonNull(configuration, "configuration");

    final var scopeName = "com.io7m.idstore";

    final var telemetryConfigurationOpt = configuration.openTelemetry();
    if (telemetryConfigurationOpt.isEmpty()) {
      LOG.debug("OpenTelemetry not configured; telemetry disabled.");
      final var openTelemetry =
        OpenTelemetry.noop();
      final var tracer =
        openTelemetry.getTracer(scopeName, version());

      return new IdServerTelemetryService(openTelemetry, tracer);
    }

    final var telemetryConfiguration =
      telemetryConfigurationOpt.get();

    final var telemetryEndpoint =
      telemetryConfiguration.collectorAddress().toString();

    LOG.debug("sending telemetry to {}", telemetryEndpoint);

    final var resource =
      Resource.getDefault()
        .merge(Resource.create(
          Attributes.of(
            SERVICE_NAME,
            telemetryConfiguration.logicalServiceName()))
        );

    final var spanExporter =
      OtlpGrpcSpanExporter.builder()
        .setEndpoint(telemetryEndpoint)
        .build();

    final var batchSpanProcessor =
      BatchSpanProcessor.builder(spanExporter)
        .build();

    final var sdkTracerProvider =
      SdkTracerProvider.builder()
        .addSpanProcessor(batchSpanProcessor)
        .setResource(resource)
        .build();

    final var metricExporter =
      OtlpGrpcMetricExporter.builder()
        .setEndpoint(telemetryEndpoint)
        .build();

    final var periodicMetricReader =
      PeriodicMetricReader.builder(metricExporter)
        .build();

    final var sdkMeterProvider =
      SdkMeterProvider.builder()
        .registerMetricReader(periodicMetricReader)
        .setResource(resource)
        .build();

    final var contextPropagators =
      ContextPropagators.create(W3CTraceContextPropagator.getInstance());

    final OpenTelemetry openTelemetry =
      OpenTelemetrySdk.builder()
        .setTracerProvider(sdkTracerProvider)
        .setMeterProvider(sdkMeterProvider)
        .setPropagators(contextPropagators)
        .buildAndRegisterGlobal();

    final var tracer =
      openTelemetry.getTracer(scopeName, version());

    return new IdServerTelemetryService(
      openTelemetry,
      tracer
    );
  }

  private static String version()
  {
    final var p =
      IdServerTelemetryService.class.getPackage();
    final var v =
      p.getImplementationVersion();

    if (v == null) {
      return "0.0.0";
    }
    return v;
  }

  @Override
  public String toString()
  {
    return "[IdServerTelemetryService 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode()));
  }

  @Override
  public String description()
  {
    return "Server OpenTelemetry service.";
  }
}

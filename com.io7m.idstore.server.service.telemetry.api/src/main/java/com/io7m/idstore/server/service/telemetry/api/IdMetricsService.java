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


package com.io7m.idstore.server.service.telemetry.api;

import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdUserDomain;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.jmulticlose.core.ClosingResourceFailedException;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.io7m.idstore.model.IdUserDomain.USER;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

/**
 * The metrics service.
 */

public final class IdMetricsService implements IdMetricsServiceType
{
  private final CloseableCollectionType<ClosingResourceFailedException> resources;
  private final LongCounter httpCount;
  private final LongCounter http2xx;
  private final LongCounter http4xx;
  private final LongCounter http5xx;
  private final LongCounter httpSizeRequest;
  private final LongCounter httpSizeResponse;
  private final LongCounter mailOK;
  private final LongCounter mailFail;
  private final LongCounter rateLimitTrigger;
  private final ConcurrentHashMap<IdUserDomain, Long> loginCountsNow;
  private final EnumMap<IdUserDomain, ConcurrentLinkedQueue<TimeSample>> mailTimeNow;
  private final EnumMap<IdUserDomain, ConcurrentLinkedQueue<TimeSample>> httpTimeNow;
  private final boolean isNoOp;
  private volatile long loginPauseTimeUser;
  private volatile long loginPauseTimeAdmin;

  private record TimeSample(
    IdUserDomain type,
    long nanos)
  {

  }

  private static final List<IdUserDomain> DOMAINS =
    List.of(IdUserDomain.values());

  /**
   * The metrics service.
   *
   * @param telemetry The underlying telemetry system
   */

  public IdMetricsService(
    final IdServerTelemetryServiceType telemetry)
  {
    Objects.requireNonNull(telemetry, "telemetry");

    this.isNoOp =
      telemetry.isNoOp();
    this.resources =
      CloseableCollection.create();

    this.httpTimeNow = new EnumMap<>(IdUserDomain.class);
    for (final var domain : DOMAINS) {
      this.httpTimeNow.put(domain, new ConcurrentLinkedQueue<>());
    }

    this.resources.add(
      telemetry.meter()
        .gaugeBuilder("idstore_http_time")
        .setDescription(
          "The length of time requests are taking to process (nanoseconds).")
        .ofLongs()
        .buildWithCallback(measurement -> {
          for (final var domain : DOMAINS) {
            measurement.record(
              maxOf(this.httpTimeNow.get(domain)),
              typeAttributesFor(domain)
            );
          }
        })
    );

    this.httpCount =
      telemetry.meter()
        .counterBuilder("idstore_http_requests")
        .setDescription("The number of HTTP requests.")
        .build();

    this.httpSizeRequest =
      telemetry.meter()
        .counterBuilder("idstore_http_requests_size")
        .setDescription("The total size of all HTTP requests so far.")
        .build();

    this.httpSizeResponse =
      telemetry.meter()
        .counterBuilder("idstore_http_responses_size")
        .setDescription("The total size of all HTTP responses so far.")
        .build();

    this.http2xx =
      telemetry.meter()
        .counterBuilder("idstore_http_responses_2xx")
        .setDescription(
          "The number of HTTP requests that resulted in 2xx successes.")
        .build();

    this.http4xx =
      telemetry.meter()
        .counterBuilder("idstore_http_responses_4xx")
        .setDescription(
          "The number of HTTP requests that resulted in 4xx failures.")
        .build();

    this.http5xx =
      telemetry.meter()
        .counterBuilder("idstore_http_responses_5xx")
        .setDescription(
          "The number of HTTP requests that resulted in 5xx failures.")
        .build();

    this.mailOK =
      telemetry.meter()
        .counterBuilder("idstore_mail_ok")
        .setDescription("The number of successful mail sends.")
        .build();

    this.mailFail =
      telemetry.meter()
        .counterBuilder("idstore_mail_failed")
        .setDescription("The number of failed mail sends.")
        .build();

    this.mailTimeNow = new EnumMap<>(IdUserDomain.class);
    for (final var domain : DOMAINS) {
      this.mailTimeNow.put(domain, new ConcurrentLinkedQueue<>());
    }

    this.resources.add(
      telemetry.meter()
        .gaugeBuilder("idstore_mail_time")
        .setDescription("The time it is taking to send mail.")
        .ofLongs()
        .buildWithCallback(measurement -> {
          for (final var domain : DOMAINS) {
            measurement.record(
              maxOf(this.mailTimeNow.get(domain)),
              typeAttributesFor(domain)
            );
          }
        })
    );

    this.rateLimitTrigger =
      telemetry.meter()
        .counterBuilder("idstore_ratelimit_triggers")
        .setDescription("The number of times a rate limit has been triggered.")
        .build();

    this.resources.add(
      telemetry.meter()
        .gaugeBuilder("idstore_ratelimit_login_delay")
        .setDescription("The delay applied to each login attempt.")
        .ofLongs()
        .buildWithCallback(measurement -> {
          measurement.record(this.loginPauseTimeAdmin, ADMIN_ATTRIBUTES);
          measurement.record(this.loginPauseTimeUser, USER_ATTRIBUTES);
        })
    );

    this.loginCountsNow =
      new ConcurrentHashMap<>();

    this.resources.add(
      telemetry.meter()
        .gaugeBuilder("idstore_sessions")
        .setDescription(
          "The number of active sessions.")
        .ofLongs()
        .buildWithCallback(this::reportLoginCounts)
    );
  }

  private static long maxOf(
    final ConcurrentLinkedQueue<TimeSample> timeSamples)
  {
    var time = 0L;
    while (!timeSamples.isEmpty()) {
      time = Math.max(time, timeSamples.poll().nanos);
    }
    return time;
  }

  private void reportLoginCounts(
    final ObservableLongMeasurement m)
  {
    for (final var entry : this.loginCountsNow.entrySet()) {
      final var sessionType =
        entry.getKey();
      final var sessionCount =
        entry.getValue();
      m.record(sessionCount.longValue(), typeAttributesFor(sessionType));
    }
  }

  @Override
  public String toString()
  {
    return "[IdMetricsService %s]"
      .formatted(Integer.toUnsignedString(this.hashCode(), 16));
  }

  @Override
  public String description()
  {
    return "Metrics service.";
  }

  @Override
  public void close()
    throws Exception
  {
    this.resources.close();
  }

  private static final Attributes USER_ATTRIBUTES =
    Attributes.of(stringKey("type"), "user");

  private static final Attributes ADMIN_ATTRIBUTES =
    Attributes.of(stringKey("type"), "admin");

  private static Attributes typeAttributesFor(
    final IdUserDomain domain)
  {
    return switch (domain) {
      case USER -> USER_ATTRIBUTES;
      case ADMIN -> ADMIN_ATTRIBUTES;
    };
  }

  @Override
  public void onHttpRequested(
    final IdUserDomain type)
  {
    if (this.isNoOp) {
      return;
    }

    this.httpCount.add(1L, typeAttributesFor(type));
  }

  @Override
  public void onHttp5xx(
    final IdUserDomain type)
  {
    if (this.isNoOp) {
      return;
    }

    this.http5xx.add(1L, typeAttributesFor(type));
  }

  @Override
  public void onHttp2xx(
    final IdUserDomain type)
  {
    if (this.isNoOp) {
      return;
    }

    this.http2xx.add(1L, typeAttributesFor(type));
  }

  @Override
  public void onHttp4xx(
    final IdUserDomain type)
  {
    if (this.isNoOp) {
      return;
    }

    this.http4xx.add(1L, typeAttributesFor(type));
  }

  @Override
  public void onHttpRequestSize(
    final IdUserDomain type,
    final long size)
  {
    if (this.isNoOp) {
      return;
    }
    if (size == -1L) {
      return;
    }
    this.httpSizeRequest.add(size, typeAttributesFor(type));
  }

  @Override
  public void onHttpResponseSize(
    final IdUserDomain type,
    final long size)
  {
    if (this.isNoOp) {
      return;
    }
    if (size == -1L) {
      return;
    }
    this.httpSizeResponse.add(size, typeAttributesFor(type));
  }

  @Override
  public void onMailSent(
    final IdEmail address,
    final Duration time)
  {
    if (this.isNoOp) {
      return;
    }

    this.mailOK.add(
      1L,
      Attributes.of(stringKey("to"), address.value())
    );

    this.mailTimeNow.get(USER)
      .add(new TimeSample(USER, time.toNanos()));
  }

  @Override
  public void onMailFailed(
    final IdEmail address,
    final Duration time)
  {
    if (this.isNoOp) {
      return;
    }

    this.mailFail.add(
      1L,
      Attributes.of(stringKey("to"), address.value())
    );

    this.mailTimeNow.get(USER)
      .add(new TimeSample(USER, time.toNanos()));
  }

  @Override
  public void onRateLimitTriggered(
    final String name,
    final String host,
    final String user,
    final String operation)
  {
    if (this.isNoOp) {
      return;
    }

    final var attributes =
      Attributes.builder()
        .put("name", name)
        .put("host", host)
        .put("user", user)
        .put("operation", operation)
        .build();

    this.rateLimitTrigger.add(1L, attributes);
  }

  @Override
  public void onHttpResponseTime(
    final IdUserDomain type,
    final Duration time)
  {
    if (this.isNoOp) {
      return;
    }

    this.httpTimeNow.get(type)
      .add(new TimeSample(type, time.toNanos()));
  }

  @Override
  public void onLogin(
    final IdUserDomain type,
    final long countNow)
  {
    if (this.isNoOp) {
      return;
    }

    this.loginCountsNow.put(type, Long.valueOf(countNow));
  }

  @Override
  public void onLoginClosed(
    final IdUserDomain type,
    final long countNow)
  {
    if (this.isNoOp) {
      return;
    }

    this.loginCountsNow.put(type, Long.valueOf(countNow));
  }

  @Override
  public void onLoginPauseTime(
    final IdUserDomain type,
    final Duration duration)
  {
    if (this.isNoOp) {
      return;
    }

    switch (type) {
      case USER -> {
        this.loginPauseTimeUser = duration.toNanos();
      }
      case ADMIN -> {
        this.loginPauseTimeAdmin = duration.toNanos();
      }
    }
  }
}

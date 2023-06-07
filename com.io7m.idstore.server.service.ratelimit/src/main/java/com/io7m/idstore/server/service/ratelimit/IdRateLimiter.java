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


package com.io7m.idstore.server.service.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongUpDownCounter;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * A trivial rate limiter.
 */

public final class IdRateLimiter implements IdRateLimiterType
{
  private final Cache<Operation, Operation> cache;
  private final LongUpDownCounter sizeGauge;
  private final LongCounter tripCounter;
  private final Duration waitTime;

  private IdRateLimiter(
    final Cache<Operation, Operation> inCache,
    final LongUpDownCounter inSizeCounter,
    final LongCounter inTripCounter,
    final Duration inWaitTime)
  {
    this.cache =
      Objects.requireNonNull(inCache, "cache");
    this.sizeGauge =
      Objects.requireNonNull(inSizeCounter, "sizeCounter");
    this.tripCounter =
      Objects.requireNonNull(inTripCounter, "tripCounter");
    this.waitTime =
      Objects.requireNonNull(inWaitTime, "inWaitTime");
  }

  /**
   * Create a rate limiter.
   *
   * @param telemetry  The telemetry service
   * @param name       The rate name
   * @param expiration The expiration for tokens
   * @param timeUnit   The time unit for expirations
   *
   * @return A rate limiter
   */

  public static IdRateLimiter create(
    final IdServerTelemetryServiceType telemetry,
    final String name,
    final long expiration,
    final TimeUnit timeUnit)
  {
    final var sizeGauge =
      telemetry.meter()
        .upDownCounterBuilder("idstore_ratelimit_%s_size".formatted(name))
        .setDescription("The size of the rate limit cache for '%s' operations.".formatted(
          name))
        .build();

    final var tripCounter =
      telemetry.meter()
        .counterBuilder("idstore_ratelimit_%s_triggers".formatted(name))
        .setDescription(
          "The number of times the rate limit has been triggered for '%s' operations.".formatted(
            name))
        .build();

    final Cache<Operation, Operation> cache =
      Caffeine.newBuilder()
        .expireAfterWrite(expiration, timeUnit)
        .evictionListener((key, value, cause) -> sizeGauge.add(-1L))
        .build();

    return new IdRateLimiter(
      cache,
      sizeGauge,
      tripCounter,
      Duration.of(expiration, timeUnit.toChronoUnit())
    );
  }

  /**
   * @param host      The host performing the action
   * @param user      The user performing the action
   * @param operation The operation
   *
   * @return {@code true} if the given operation is allowed by rate limiting
   */

  public boolean isAllowedByRateLimit(
    final String host,
    final String user,
    final String operation)
  {
    final var attributes =
      Attributes.builder()
        .put("host", host)
        .put("user", user)
        .put("operation", operation)
        .build();

    final var op =
      new Operation(host, user, operation);
    final var existing =
      this.cache.getIfPresent(op);

    if (existing != null) {
      this.tripCounter.add(1L, attributes);
      return false;
    }

    this.cache.put(op, op);
    this.sizeGauge.add(1L, attributes);
    return true;
  }

  @Override
  public String toString()
  {
    return "[IdRateLimiter %s]"
      .formatted(Integer.toUnsignedString(this.hashCode(), 16));
  }

  @Override
  public Duration waitTime()
  {
    return this.waitTime;
  }

  @Override
  public String description()
  {
    return "A basic rate limiter.";
  }

  private record Operation(
    String host,
    String user,
    String operation)
  {
    private Operation
    {
      Objects.requireNonNull(host, "host");
      Objects.requireNonNull(user, "user");
      Objects.requireNonNull(operation, "operation");
    }
  }
}

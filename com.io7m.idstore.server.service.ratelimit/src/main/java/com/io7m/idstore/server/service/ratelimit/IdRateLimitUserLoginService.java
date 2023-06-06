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

import com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * A rate limiting service for logins.
 */

public final class IdRateLimitUserLoginService
  implements IdRateLimitUserLoginServiceType
{
  private final IdRateLimiter limiter;

  private IdRateLimitUserLoginService(
    final IdRateLimiter inLimiter)
  {
    this.limiter = Objects.requireNonNull(inLimiter, "limiter");
  }

  /**
   * Create a rate limit service.
   *
   * @param telemetry  The telemetry service
   * @param expiration The expiration for tokens
   * @param timeUnit   The time unit for expirations
   *
   * @return A rate limiter
   */

  public static IdRateLimitUserLoginServiceType create(
    final IdServerTelemetryServiceType telemetry,
    final long expiration,
    final TimeUnit timeUnit)
  {
    return new IdRateLimitUserLoginService(
      IdRateLimiter.create(
        telemetry,
        "user_login",
        expiration,
        timeUnit)
    );
  }

  @Override
  public boolean isAllowedByRateLimit(
    final String host)
  {
    return this.limiter.isAllowedByRateLimit(host, "", "LOGIN");
  }

  @Override
  public String description()
  {
    return "User login rate limiting service.";
  }

  @Override
  public String toString()
  {
    return "[IdRateLimitUserLoginService 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode(), 16));
  }
}

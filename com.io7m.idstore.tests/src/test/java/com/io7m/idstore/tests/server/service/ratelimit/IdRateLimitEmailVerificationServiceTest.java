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

package com.io7m.idstore.tests.server.service.ratelimit;

import com.io7m.idstore.server.service.ratelimit.IdRateLimitEmailVerificationService;
import com.io7m.idstore.server.service.ratelimit.IdRateLimitEmailVerificationServiceType;
import com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryNoOp;
import com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType;
import com.io7m.idstore.tests.server.service.IdServiceContract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class IdRateLimitEmailVerificationServiceTest
  extends IdServiceContract<IdRateLimitEmailVerificationServiceType>
{
  private IdServerTelemetryServiceType telemetry;

  @BeforeEach
  public void setup()
  {
    this.telemetry = IdServerTelemetryNoOp.noop();
  }

  @Test
  public void testOK()
    throws Exception
  {
    final var service =
      IdRateLimitEmailVerificationService.create(
        this.telemetry,
        100L,
        MILLISECONDS
      );

    final var userId = UUID.randomUUID();
    assertTrue(service.isAllowedByRateLimit(userId));
    assertFalse(service.isAllowedByRateLimit(userId));

    Thread.sleep(150L);
    assertTrue(service.isAllowedByRateLimit(userId));
  }

  @Override
  protected IdRateLimitEmailVerificationServiceType createInstanceA()
  {
    return IdRateLimitEmailVerificationService.create(
      this.telemetry,
      100L,
      MILLISECONDS
    );
  }

  @Override
  protected IdRateLimitEmailVerificationServiceType createInstanceB()
  {
    return IdRateLimitEmailVerificationService.create(
      this.telemetry,
      200L,
      MILLISECONDS
    );
  }
}

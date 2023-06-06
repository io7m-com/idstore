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

package com.io7m.idstore.tests.server.service.sessions;

import com.io7m.idstore.server.service.sessions.IdSessionAdmin;
import com.io7m.idstore.server.service.sessions.IdSessionAdminService;
import io.opentelemetry.api.OpenTelemetry;

import java.time.Duration;

public final class IdSessionAdminServiceTest
  extends IdSessionServiceContract<IdSessionAdmin, IdSessionAdminService>
{
  @Override
  protected IdSessionAdminService createInstanceA()
  {
    return this.createWithExpiration(Duration.ofSeconds(5L));
  }

  @Override
  protected IdSessionAdminService createInstanceB()
  {
    return this.createWithExpiration(Duration.ofSeconds(10L));
  }

  @Override
  protected IdSessionAdminService createWithExpiration(
    final Duration expiration)
  {
    return new IdSessionAdminService(
      OpenTelemetry.noop().getMeter("com.io7m.idstore"),
      expiration
    );
  }
}

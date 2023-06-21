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

package com.io7m.idstore.server.user_v1;

import com.io7m.idstore.server.http.IdHTTPServletFunctional;
import com.io7m.idstore.server.http.IdHTTPServletFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPServletResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPServletResponseType;
import com.io7m.idstore.server.service.health.IdServerHealth;
import com.io7m.idstore.server.service.maintenance.IdClosedForMaintenanceService;
import com.io7m.repetoir.core.RPServiceDirectoryType;

import java.util.Objects;

import static com.io7m.idstore.model.IdUserDomain.USER;
import static com.io7m.idstore.server.http.IdHTTPServletCoreInstrumented.withInstrumentation;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The v1 health servlet.
 */

public final class IdU1ServletHealth
  extends IdHTTPServletFunctional
{
  /**
   * The v1 health servlet.
   *
   * @param services The services
   */

  public IdU1ServletHealth(
    final RPServiceDirectoryType services)
  {
    super(createCore(services));
  }

  private static IdHTTPServletFunctionalCoreType createCore(
    final RPServiceDirectoryType services)
  {
    final var health =
      services.requireService(IdServerHealth.class);
    final var maintenance =
      services.requireService(IdClosedForMaintenanceService.class);

    final IdHTTPServletFunctionalCoreType main =
      (request, information) -> execute(health, maintenance);

    return withInstrumentation(services, USER, main);
  }

  private static IdHTTPServletResponseType execute(
    final IdServerHealth health,
    final IdClosedForMaintenanceService maintenance)
  {
    final var closed = maintenance.isClosed();
    if (closed.isPresent()) {
      return new IdHTTPServletResponseFixedSize(
        503,
        "text/plain",
        closed.get().getBytes(UTF_8)
      );
    }

    final var status =
      health.status();
    final var statusCode =
      Objects.equals(status, IdServerHealth.statusOKText()) ? 200 : 500;

    return new IdHTTPServletResponseFixedSize(
      statusCode,
      "text/plain",
      status.getBytes(UTF_8)
    );
  }
}

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


package com.io7m.idstore.server.admin_v1;

import com.io7m.idstore.server.http.IdHTTPHandlerFunctional;
import com.io7m.idstore.server.http.IdHTTPHandlerFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPResponseType;
import com.io7m.idstore.server.service.health.IdServerHealth;
import com.io7m.idstore.server.service.maintenance.IdClosedForMaintenanceService;
import com.io7m.repetoir.core.RPServiceDirectoryType;

import java.util.Objects;
import java.util.Set;

import static com.io7m.idstore.model.IdUserDomain.ADMIN;
import static com.io7m.idstore.server.http.IdHTTPHandlerCoreInstrumented.withInstrumentation;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The v1 health servlet.
 */

public final class IdA1HandlerHealth
  extends IdHTTPHandlerFunctional
{
  /**
   * The v1 health servlet.
   *
   * @param services The services
   */

  public IdA1HandlerHealth(
    final RPServiceDirectoryType services)
  {
    super(createCore(services));
  }

  private static IdHTTPHandlerFunctionalCoreType createCore(
    final RPServiceDirectoryType services)
  {
    final var health =
      services.requireService(IdServerHealth.class);
    final var maintenance =
      services.requireService(IdClosedForMaintenanceService.class);

    final IdHTTPHandlerFunctionalCoreType main =
      (request, information) -> execute(health, maintenance);

    return withInstrumentation(services, ADMIN, main);
  }

  private static IdHTTPResponseType execute(
    final IdServerHealth health,
    final IdClosedForMaintenanceService maintenance)
  {
    final var closed = maintenance.isClosed();
    if (closed.isPresent()) {
      return new IdHTTPResponseFixedSize(
        503,
        Set.of(),
        "text/plain",
        closed.get().getBytes(UTF_8)
      );
    }

    final var status =
      health.status();
    final var statusCode =
      Objects.equals(status, IdServerHealth.statusOKText()) ? 200 : 500;

    return new IdHTTPResponseFixedSize(
      statusCode,
      Set.of(),
      "text/plain",
      status.getBytes(UTF_8)
    );
  }
}

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

import com.io7m.idstore.protocol.user.IdUResponseError;
import com.io7m.idstore.protocol.user.cb.IdUCB1Messages;
import com.io7m.idstore.server.http.IdHTTPServletFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPServletRequestInformation;
import com.io7m.idstore.server.http.IdHTTPServletResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPServletResponseType;
import com.io7m.idstore.server.service.maintenance.IdClosedForMaintenanceService;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.CLOSED_FOR_MAINTENANCE;
import static com.io7m.idstore.protocol.user.IdUResponseBlame.BLAME_SERVER;

/**
 * A core that executes the given core if the server is not closed for maintenance.
 */

public final class IdU1ServletCoreMaintenanceAware
  implements IdHTTPServletFunctionalCoreType
{
  private final IdHTTPServletFunctionalCoreType core;
  private final IdUCB1Messages messages;
  private final IdClosedForMaintenanceService maintenance;

  private IdU1ServletCoreMaintenanceAware(
    final RPServiceDirectoryType services,
    final IdHTTPServletFunctionalCoreType inCore)
  {
    Objects.requireNonNull(services, "services");

    this.core =
      Objects.requireNonNull(inCore, "core");
    this.maintenance =
      services.requireService(IdClosedForMaintenanceService.class);
    this.messages =
      services.requireService(IdUCB1Messages.class);
  }

  /**
   * @param services The services
   * @param inCore   The executed core
   *
   * @return A core that executes the given core if the server is not closed for maintenance
   */

  public static IdHTTPServletFunctionalCoreType withMaintenanceAwareness(
    final RPServiceDirectoryType services,
    final IdHTTPServletFunctionalCoreType inCore)
  {
    return new IdU1ServletCoreMaintenanceAware(services, inCore);
  }

  @Override
  public IdHTTPServletResponseType execute(
    final HttpServletRequest request,
    final IdHTTPServletRequestInformation information)
  {
    final var closed = this.maintenance.isClosed();
    if (closed.isPresent()) {
      return new IdHTTPServletResponseFixedSize(
        503,
        IdUCB1Messages.contentType(),
        this.messages.serialize(
          new IdUResponseError(
            information.requestId(),
            closed.get(),
            CLOSED_FOR_MAINTENANCE,
            Map.of(),
            Optional.empty(),
            BLAME_SERVER
          )
        )
      );
    }
    return this.core.execute(request, information);
  }
}

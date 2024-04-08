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


package com.io7m.idstore.server.user_v2;

import com.io7m.idstore.protocol.user.IdUResponseError;
import com.io7m.idstore.protocol.user.cb.IdUCB2Messages;
import com.io7m.idstore.server.http.IdHTTPHandlerFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPRequestInformation;
import com.io7m.idstore.server.http.IdHTTPResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPResponseType;
import com.io7m.idstore.server.service.maintenance.IdClosedForMaintenanceService;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import io.helidon.webserver.http.ServerRequest;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.CLOSED_FOR_MAINTENANCE;
import static com.io7m.idstore.protocol.user.IdUResponseBlame.BLAME_SERVER;

/**
 * A core that executes the given core if the server is not closed for maintenance.
 */

public final class IdU2HandlerCoreMaintenanceAware
  implements IdHTTPHandlerFunctionalCoreType
{
  private final IdHTTPHandlerFunctionalCoreType core;
  private final IdUCB2Messages messages;
  private final IdClosedForMaintenanceService maintenance;

  private IdU2HandlerCoreMaintenanceAware(
    final RPServiceDirectoryType services,
    final IdHTTPHandlerFunctionalCoreType inCore)
  {
    Objects.requireNonNull(services, "services");

    this.core =
      Objects.requireNonNull(inCore, "core");
    this.maintenance =
      services.requireService(IdClosedForMaintenanceService.class);
    this.messages =
      services.requireService(IdUCB2Messages.class);
  }

  /**
   * @param services The services
   * @param inCore   The executed core
   *
   * @return A core that executes the given core if the server is not closed for maintenance
   */

  public static IdHTTPHandlerFunctionalCoreType withMaintenanceAwareness(
    final RPServiceDirectoryType services,
    final IdHTTPHandlerFunctionalCoreType inCore)
  {
    return new IdU2HandlerCoreMaintenanceAware(services, inCore);
  }

  @Override
  public IdHTTPResponseType execute(
    final ServerRequest request,
    final IdHTTPRequestInformation information)
  {
    final var closed = this.maintenance.isClosed();
    if (closed.isPresent()) {
      return new IdHTTPResponseFixedSize(
        503,
        Set.of(),
        IdUCB2Messages.contentType(),
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

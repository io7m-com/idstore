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

import com.io7m.idstore.database.api.IdDatabaseAdminsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.error_codes.IdStandardErrorCodes;
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.protocol.admin.IdAResponseBlame;
import com.io7m.idstore.protocol.admin.IdAResponseError;
import com.io7m.idstore.protocol.admin.cb.IdACB1Messages;
import com.io7m.idstore.server.controller.IdServerStrings;
import com.io7m.idstore.server.http.IdHTTPServletFunctionalCoreAuthenticatedType;
import com.io7m.idstore.server.http.IdHTTPServletFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPServletRequestInformation;
import com.io7m.idstore.server.http.IdHTTPServletResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPServletResponseType;
import com.io7m.idstore.server.service.sessions.IdSessionAdmin;
import com.io7m.idstore.server.service.sessions.IdSessionAdminService;
import com.io7m.idstore.server.service.sessions.IdSessionSecretIdentifier;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.protocol.admin.IdAResponseBlame.BLAME_SERVER;
import static com.io7m.idstore.server.admin_v1.IdA1Errors.errorResponseOf;
import static com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType.setSpanErrorCode;

/**
 * A core that executes the given core under authentication.
 */

public final class IdA1ServletCoreAuthenticated
  implements IdHTTPServletFunctionalCoreType
{
  private final IdHTTPServletFunctionalCoreAuthenticatedType<IdSessionAdmin, IdAdmin> core;
  private final IdDatabaseType database;
  private final IdSessionAdminService adminSessions;
  private final IdACB1Messages messages;
  private final IdServerStrings strings;

  private IdA1ServletCoreAuthenticated(
    final RPServiceDirectoryType services,
    final IdHTTPServletFunctionalCoreAuthenticatedType<IdSessionAdmin, IdAdmin> inCore)
  {
    Objects.requireNonNull(services, "services");

    this.core =
      Objects.requireNonNull(inCore, "core");
    this.strings =
      services.requireService(IdServerStrings.class);
    this.database =
      services.requireService(IdDatabaseType.class);
    this.adminSessions =
      services.requireService(IdSessionAdminService.class);
    this.messages =
      services.requireService(IdACB1Messages.class);
  }

  /**
   * @param services The services
   * @param inCore   The executed core
   *
   * @return A core that executes the given core under authentication
   */

  public static IdHTTPServletFunctionalCoreType withAuthentication(
    final RPServiceDirectoryType services,
    final IdHTTPServletFunctionalCoreAuthenticatedType<IdSessionAdmin, IdAdmin> inCore)
  {
    return new IdA1ServletCoreAuthenticated(services, inCore);
  }

  @Override
  public IdHTTPServletResponseType execute(
    final HttpServletRequest request,
    final IdHTTPServletRequestInformation information)
  {
    final var httpSession =
      request.getSession(true);
    final var adminSessionId =
      (IdSessionSecretIdentifier) httpSession.getAttribute("ID");

    if (adminSessionId == null) {
      return this.notAuthenticated(information);
    }

    final var adminSessionOpt =
      this.adminSessions.findSession(adminSessionId);

    if (adminSessionOpt.isEmpty()) {
      return this.notAuthenticated(information);
    }

    final var adminSession =
      adminSessionOpt.get();

    final Optional<IdAdmin> adminOpt;
    try {
      adminOpt = this.adminGet(adminSession.adminId());
    } catch (final IdDatabaseException e) {
      setSpanErrorCode(e.errorCode());
      return errorResponseOf(this.messages, information, BLAME_SERVER, e);
    }

    if (adminOpt.isEmpty()) {
      return this.notAuthenticated(information);
    }

    return this.core.executeAuthenticated(
      request,
      information,
      adminSession,
      adminOpt.get()
    );
  }

  private IdHTTPServletResponseType notAuthenticated(
    final IdHTTPServletRequestInformation information)
  {
    return new IdHTTPServletResponseFixedSize(
      401,
      IdACB1Messages.contentType(),
      this.messages.serialize(
        new IdAResponseError(
          information.requestId(),
          this.strings.format("unauthorized"),
          IdStandardErrorCodes.AUTHENTICATION_ERROR,
          Map.of(),
          Optional.empty(),
          IdAResponseBlame.BLAME_CLIENT
        )
      )
    );
  }

  private Optional<IdAdmin> adminGet(
    final UUID id)
    throws IdDatabaseException
  {
    try (var c = this.database.openConnection(IDSTORE)) {
      try (var t = c.openTransaction()) {
        final var q =
          t.queries(IdDatabaseAdminsQueriesType.class);
        return q.adminGet(id);
      }
    }
  }
}

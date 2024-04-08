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


package com.io7m.idstore.server.admin_v2;

import com.io7m.idstore.database.api.IdDatabaseAdminsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.error_codes.IdStandardErrorCodes;
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdValidityException;
import com.io7m.idstore.protocol.admin.IdAResponseBlame;
import com.io7m.idstore.protocol.admin.IdAResponseError;
import com.io7m.idstore.protocol.admin.cb.IdACB2Messages;
import com.io7m.idstore.server.http.IdHTTPHandlerFunctionalCoreAuthenticatedType;
import com.io7m.idstore.server.http.IdHTTPHandlerFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPRequestInformation;
import com.io7m.idstore.server.http.IdHTTPResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPResponseType;
import com.io7m.idstore.server.service.sessions.IdSessionAdmin;
import com.io7m.idstore.server.service.sessions.IdSessionAdminService;
import com.io7m.idstore.server.service.sessions.IdSessionSecretIdentifier;
import com.io7m.idstore.strings.IdStrings;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import io.helidon.webserver.http.ServerRequest;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.protocol.admin.IdAResponseBlame.BLAME_SERVER;
import static com.io7m.idstore.server.admin_v2.IdA2Errors.errorResponseOf;
import static com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType.setSpanErrorCode;
import static com.io7m.idstore.strings.IdStringConstants.UNAUTHORIZED;

/**
 * A core that executes the given core under authentication.
 */

public final class IdA2HandlerCoreAuthenticated
  implements IdHTTPHandlerFunctionalCoreType
{
  private final IdHTTPHandlerFunctionalCoreAuthenticatedType<IdSessionAdmin, IdAdmin> core;
  private final IdDatabaseType database;
  private final IdSessionAdminService adminSessions;
  private final IdACB2Messages messages;
  private final IdStrings strings;

  private IdA2HandlerCoreAuthenticated(
    final RPServiceDirectoryType services,
    final IdHTTPHandlerFunctionalCoreAuthenticatedType<IdSessionAdmin, IdAdmin> inCore)
  {
    Objects.requireNonNull(services, "services");

    this.core =
      Objects.requireNonNull(inCore, "core");
    this.strings =
      services.requireService(IdStrings.class);
    this.database =
      services.requireService(IdDatabaseType.class);
    this.adminSessions =
      services.requireService(IdSessionAdminService.class);
    this.messages =
      services.requireService(IdACB2Messages.class);
  }

  /**
   * @param services The services
   * @param inCore   The executed core
   *
   * @return A core that executes the given core under authentication
   */

  public static IdHTTPHandlerFunctionalCoreType withAuthentication(
    final RPServiceDirectoryType services,
    final IdHTTPHandlerFunctionalCoreAuthenticatedType<IdSessionAdmin, IdAdmin> inCore)
  {
    return new IdA2HandlerCoreAuthenticated(services, inCore);
  }

  @Override
  public IdHTTPResponseType execute(
    final ServerRequest request,
    final IdHTTPRequestInformation information)
  {
    final var headers =
      request.headers();
    final var cookies =
      headers.cookies();

    final String cookie;
    try {
      cookie = cookies.get("IDSTORE_ADMIN_API_SESSION");
    } catch (final NoSuchElementException e) {
      return this.notAuthenticated(information);
    }

    final IdSessionSecretIdentifier adminSessionId;
    try {
      adminSessionId = new IdSessionSecretIdentifier(cookie);
    } catch (final IdValidityException e) {
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

  private IdHTTPResponseType notAuthenticated(
    final IdHTTPRequestInformation information)
  {
    return new IdHTTPResponseFixedSize(
      401,
      Set.of(),
      IdACB2Messages.contentType(),
      this.messages.serialize(
        new IdAResponseError(
          information.requestId(),
          this.strings.format(UNAUTHORIZED),
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

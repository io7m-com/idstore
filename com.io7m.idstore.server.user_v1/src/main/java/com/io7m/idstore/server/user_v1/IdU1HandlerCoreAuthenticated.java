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

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.error_codes.IdStandardErrorCodes;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.model.IdValidityException;
import com.io7m.idstore.protocol.user.IdUResponseError;
import com.io7m.idstore.protocol.user.cb.IdUCB1Messages;
import com.io7m.idstore.server.http.IdHTTPHandlerFunctionalCoreAuthenticatedType;
import com.io7m.idstore.server.http.IdHTTPHandlerFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPRequestInformation;
import com.io7m.idstore.server.http.IdHTTPResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPResponseType;
import com.io7m.idstore.server.service.sessions.IdSessionSecretIdentifier;
import com.io7m.idstore.server.service.sessions.IdSessionUser;
import com.io7m.idstore.server.service.sessions.IdSessionUserService;
import com.io7m.idstore.strings.IdStrings;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import io.helidon.webserver.http.ServerRequest;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.protocol.user.IdUResponseBlame.BLAME_CLIENT;
import static com.io7m.idstore.protocol.user.IdUResponseBlame.BLAME_SERVER;
import static com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType.setSpanErrorCode;
import static com.io7m.idstore.server.user_v1.IdU1Errors.errorResponseOf;
import static com.io7m.idstore.strings.IdStringConstants.UNAUTHORIZED;

/**
 * A core that executes the given core under authentication.
 */

public final class IdU1HandlerCoreAuthenticated
  implements IdHTTPHandlerFunctionalCoreType
{
  private final IdHTTPHandlerFunctionalCoreAuthenticatedType<IdSessionUser, IdUser> core;
  private final IdDatabaseType database;
  private final IdSessionUserService userSessions;
  private final IdUCB1Messages messages;
  private final IdStrings strings;

  private IdU1HandlerCoreAuthenticated(
    final RPServiceDirectoryType services,
    final IdHTTPHandlerFunctionalCoreAuthenticatedType<IdSessionUser, IdUser> inCore)
  {
    Objects.requireNonNull(services, "services");

    this.core =
      Objects.requireNonNull(inCore, "core");
    this.strings =
      services.requireService(IdStrings.class);
    this.database =
      services.requireService(IdDatabaseType.class);
    this.userSessions =
      services.requireService(IdSessionUserService.class);
    this.messages =
      services.requireService(IdUCB1Messages.class);
  }

  /**
   * @param services The services
   * @param inCore   The executed core
   *
   * @return A core that executes the given core under authentication
   */

  public static IdHTTPHandlerFunctionalCoreType withAuthentication(
    final RPServiceDirectoryType services,
    final IdHTTPHandlerFunctionalCoreAuthenticatedType<IdSessionUser, IdUser> inCore)
  {
    return new IdU1HandlerCoreAuthenticated(services, inCore);
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
    final var cookie =
      cookies.get("IDSTORE_USER_API_SESSION");

    if (cookie == null) {
      return this.notAuthenticated(information);
    }

    final IdSessionSecretIdentifier userSessionId;
    try {
      userSessionId = new IdSessionSecretIdentifier(cookie);
    } catch (final IdValidityException e) {
      return this.notAuthenticated(information);
    }

    final var userSessionOpt =
      this.userSessions.findSession(userSessionId);

    if (userSessionOpt.isEmpty()) {
      return this.notAuthenticated(information);
    }

    final var userSession =
      userSessionOpt.get();

    final Optional<IdUser> userOpt;
    try {
      userOpt = this.userGet(userSession.userId());
    } catch (final IdDatabaseException e) {
      setSpanErrorCode(e.errorCode());
      return errorResponseOf(this.messages, information, BLAME_SERVER, e);
    }

    if (userOpt.isEmpty()) {
      return this.notAuthenticated(information);
    }

    return this.core.executeAuthenticated(
      request,
      information,
      userSession,
      userOpt.get()
    );
  }

  private IdHTTPResponseType notAuthenticated(
    final IdHTTPRequestInformation information)
  {
    return new IdHTTPResponseFixedSize(
      401,
      Set.of(),
      IdUCB1Messages.contentType(),
      this.messages.serialize(
        new IdUResponseError(
          information.requestId(),
          this.strings.format(UNAUTHORIZED),
          IdStandardErrorCodes.AUTHENTICATION_ERROR,
          Map.of(),
          Optional.empty(),
          BLAME_CLIENT
        )
      )
    );
  }

  private Optional<IdUser> userGet(
    final UUID id)
    throws IdDatabaseException
  {
    try (var c = this.database.openConnection(IDSTORE)) {
      try (var t = c.openTransaction()) {
        final var q =
          t.queries(IdDatabaseUsersQueriesType.class);
        return q.userGet(id);
      }
    }
  }
}

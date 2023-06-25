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
import com.io7m.idstore.protocol.user.IdUResponseError;
import com.io7m.idstore.protocol.user.cb.IdUCB1Messages;
import com.io7m.idstore.server.http.IdHTTPServletFunctionalCoreAuthenticatedType;
import com.io7m.idstore.server.http.IdHTTPServletFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPServletRequestInformation;
import com.io7m.idstore.server.http.IdHTTPServletResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPServletResponseType;
import com.io7m.idstore.server.service.sessions.IdSessionSecretIdentifier;
import com.io7m.idstore.server.service.sessions.IdSessionUser;
import com.io7m.idstore.server.service.sessions.IdSessionUserService;
import com.io7m.idstore.strings.IdStrings;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

public final class IdU1ServletCoreAuthenticated
  implements IdHTTPServletFunctionalCoreType
{
  private final IdHTTPServletFunctionalCoreAuthenticatedType<IdSessionUser, IdUser> core;
  private final IdDatabaseType database;
  private final IdSessionUserService userSessions;
  private final IdUCB1Messages messages;
  private final IdStrings strings;

  private IdU1ServletCoreAuthenticated(
    final RPServiceDirectoryType services,
    final IdHTTPServletFunctionalCoreAuthenticatedType<IdSessionUser, IdUser> inCore)
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

  public static IdHTTPServletFunctionalCoreType withAuthentication(
    final RPServiceDirectoryType services,
    final IdHTTPServletFunctionalCoreAuthenticatedType<IdSessionUser, IdUser> inCore)
  {
    return new IdU1ServletCoreAuthenticated(services, inCore);
  }

  @Override
  public IdHTTPServletResponseType execute(
    final HttpServletRequest request,
    final IdHTTPServletRequestInformation information)
  {
    final var httpSession =
      request.getSession(true);
    final var userSessionId =
      (IdSessionSecretIdentifier) httpSession.getAttribute("ID");

    if (userSessionId == null) {
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

  private IdHTTPServletResponseType notAuthenticated(
    final IdHTTPServletRequestInformation information)
  {
    return new IdHTTPServletResponseFixedSize(
      401,
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

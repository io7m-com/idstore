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

package com.io7m.idstore.server.user_view;

import com.io7m.idstore.model.IdValidityException;
import com.io7m.idstore.server.http.IdHTTPHandlerFunctional;
import com.io7m.idstore.server.http.IdHTTPHandlerFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPResponseRedirect;
import com.io7m.idstore.server.http.IdHTTPResponseType;
import com.io7m.idstore.server.service.sessions.IdSessionSecretIdentifier;
import com.io7m.idstore.server.service.sessions.IdSessionUserService;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import io.helidon.webserver.http.ServerRequest;

import java.util.Set;

import static com.io7m.idstore.model.IdUserDomain.USER;
import static com.io7m.idstore.server.http.IdHTTPHandlerCoreInstrumented.withInstrumentation;
import static com.io7m.idstore.server.user_view.IdUVHandlerCoreMaintenanceAware.withMaintenanceAwareness;

/**
 * The page that logs out.
 */

public final class IdUVLogout extends IdHTTPHandlerFunctional
{
  /**
   * The page that logs out.
   *
   * @param services The services
   */

  public IdUVLogout(
    final RPServiceDirectoryType services)
  {
    super(createCore(services));
  }

  private static IdHTTPHandlerFunctionalCoreType createCore(
    final RPServiceDirectoryType services)
  {
    final var userSessions =
      services.requireService(IdSessionUserService.class);

    final IdHTTPHandlerFunctionalCoreType main =
      (request, information) -> execute(userSessions, request);

    final var maintenanceAware = withMaintenanceAwareness(services, main);
    return withInstrumentation(services, USER, maintenanceAware);
  }

  private static IdHTTPResponseType execute(
    final IdSessionUserService userSessions,
    final ServerRequest request)
  {
    final var headers =
      request.headers();
    final var cookies =
      headers.cookies();
    final var cookie =
      cookies.get("IDSTORE_USER_VIEW_SESSION");

    if (cookie == null) {
      return new IdHTTPResponseRedirect(Set.of(), "/");
    }

    final IdSessionSecretIdentifier userSessionId;
    try {
      userSessionId = new IdSessionSecretIdentifier(cookie);
    } catch (final IdValidityException e) {
      return new IdHTTPResponseRedirect(Set.of(), "/");
    }

    final var userSessionOpt =
      userSessions.findSession(userSessionId);

    if (userSessionOpt.isEmpty()) {
      return new IdHTTPResponseRedirect(Set.of(), "/");
    }

    userSessions.deleteSession(userSessionId);
    return new IdHTTPResponseRedirect(Set.of(), "/");
  }
}

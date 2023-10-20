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

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.model.IdValidityException;
import com.io7m.idstore.server.http.IdHTTPHandlerFunctionalCoreAuthenticatedType;
import com.io7m.idstore.server.http.IdHTTPHandlerFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPRequestInformation;
import com.io7m.idstore.server.http.IdHTTPResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPResponseType;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.sessions.IdSessionSecretIdentifier;
import com.io7m.idstore.server.service.sessions.IdSessionUser;
import com.io7m.idstore.server.service.sessions.IdSessionUserService;
import com.io7m.idstore.server.service.templating.IdFMLoginData;
import com.io7m.idstore.server.service.templating.IdFMMessageData;
import com.io7m.idstore.server.service.templating.IdFMTemplateServiceType;
import com.io7m.idstore.server.service.templating.IdFMTemplateType;
import com.io7m.idstore.strings.IdStrings;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import freemarker.template.TemplateException;
import io.helidon.webserver.http.ServerRequest;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.strings.IdStringConstants.ERROR;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A core that executes the given core under authentication.
 */

public final class IdUVHandlerCoreAuthenticated
  implements IdHTTPHandlerFunctionalCoreType
{
  private final IdHTTPHandlerFunctionalCoreAuthenticatedType<IdSessionUser, IdUser> core;
  private final IdDatabaseType database;
  private final IdSessionUserService userSessions;
  private final IdStrings strings;
  private final IdFMTemplateType<IdFMMessageData> template;
  private final IdServerBrandingServiceType branding;
  private final IdFMTemplateType<IdFMLoginData> loginTemplate;

  private IdUVHandlerCoreAuthenticated(
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
    this.branding =
      services.requireService(IdServerBrandingServiceType.class);

    final var templates =
      services.requireService(IdFMTemplateServiceType.class);
    this.template =
      templates.pageMessage();
    this.loginTemplate =
      templates.pageLoginTemplate();
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
    return new IdUVHandlerCoreAuthenticated(services, inCore);
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
      cookie = cookies.get("IDSTORE_USER_VIEW_SESSION");
    } catch (NoSuchElementException | UnsupportedOperationException e) {
      return this.notAuthenticated();
    }

    if (cookie == null) {
      return this.notAuthenticated();
    }

    final IdSessionSecretIdentifier userSessionId;
    try {
      userSessionId = new IdSessionSecretIdentifier(cookie);
    } catch (final IdValidityException e) {
      return this.notAuthenticated();
    }

    final var userSessionOpt =
      this.userSessions.findSession(userSessionId);

    if (userSessionOpt.isEmpty()) {
      return this.notAuthenticated();
    }

    final var userSession =
      userSessionOpt.get();

    final Optional<IdUser> userOpt;
    try {
      userOpt = this.userGet(userSession.userId());
    } catch (final IdDatabaseException e) {
      return this.errorOf(information, e);
    }

    if (userOpt.isEmpty()) {
      return this.notAuthenticated();
    }

    return this.core.executeAuthenticated(
      request,
      information,
      userSession,
      userOpt.get()
    );
  }

  private IdHTTPResponseType errorOf(
    final IdHTTPRequestInformation information,
    final IdDatabaseException e)
  {
    try (var writer = new StringWriter()) {
      this.template.process(
        new IdFMMessageData(
          this.branding.htmlTitle(this.strings.format(ERROR)),
          this.branding.title(),
          information.requestId(),
          true,
          true,
          this.strings.format(ERROR),
          e.getMessage(),
          "/"
        ),
        writer
      );
      return new IdHTTPResponseFixedSize(
        500,
        Set.of(),
        "application/xhtml+xml",
        writer.toString().getBytes(UTF_8)
      );
    } catch (final IOException ex) {
      throw new UncheckedIOException(ex);
    } catch (final TemplateException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private IdHTTPResponseType notAuthenticated()
  {
    return IdUVLogin.showLoginForm(
      this.branding,
      this.loginTemplate,
      Optional.empty(),
      401
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

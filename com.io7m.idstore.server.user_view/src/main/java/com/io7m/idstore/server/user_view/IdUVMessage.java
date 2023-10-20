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

import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.server.http.IdHTTPHandlerFunctional;
import com.io7m.idstore.server.http.IdHTTPHandlerFunctionalCoreAuthenticatedType;
import com.io7m.idstore.server.http.IdHTTPHandlerFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPRequestInformation;
import com.io7m.idstore.server.http.IdHTTPResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPResponseRedirect;
import com.io7m.idstore.server.http.IdHTTPResponseType;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.sessions.IdSessionUser;
import com.io7m.idstore.server.service.templating.IdFMMessageData;
import com.io7m.idstore.server.service.templating.IdFMTemplateServiceType;
import com.io7m.idstore.server.service.templating.IdFMTemplateType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.Set;

import static com.io7m.idstore.model.IdUserDomain.USER;
import static com.io7m.idstore.server.http.IdHTTPHandlerCoreInstrumented.withInstrumentation;
import static com.io7m.idstore.server.user_view.IdUVHandlerCoreAuthenticated.withAuthentication;
import static com.io7m.idstore.server.user_view.IdUVHandlerCoreMaintenanceAware.withMaintenanceAwareness;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Display a message logged to the user's (authenticated) session.
 */

public final class IdUVMessage extends IdHTTPHandlerFunctional
{
  /**
   * Display a message logged to the user's (authenticated) session.
   *
   * @param services The services
   */

  public IdUVMessage(
    final RPServiceDirectoryType services)
  {
    super(createCore(services));
  }

  private static IdHTTPHandlerFunctionalCoreType createCore(
    final RPServiceDirectoryType services)
  {
    final var branding =
      services.requireService(IdServerBrandingServiceType.class);
    final var template =
      services.requireService(IdFMTemplateServiceType.class)
        .pageMessage();

    final IdHTTPHandlerFunctionalCoreAuthenticatedType<IdSessionUser, IdUser> main =
      (request, information, session, user) -> {
        return showMessage(information, session, branding, template);
      };

    final var authenticated =
      withAuthentication(services, main);
    final var maintenanceAware =
      withMaintenanceAwareness(services, authenticated);

    return withInstrumentation(services, USER, maintenanceAware);
  }

  /**
   * Show a message from the given session.
   *
   * @param information The request information
   * @param session     The user session
   * @param branding    The branding resources
   * @param template    The message template
   *
   * @return A formatted response
   */

  public static IdHTTPResponseType showMessage(
    final IdHTTPRequestInformation information,
    final IdSessionUser session,
    final IdServerBrandingServiceType branding,
    final IdFMTemplateType<IdFMMessageData> template)
  {
    Objects.requireNonNull(information, "information");
    Objects.requireNonNull(session, "session");
    Objects.requireNonNull(branding, "branding");
    Objects.requireNonNull(template, "template");

    final var messageOpt = session.messageCurrent();
    if (messageOpt.isEmpty()) {
      return new IdHTTPResponseRedirect(Set.of(), "/");
    }

    try (var writer = new StringWriter()) {
      final var message = messageOpt.get();

      final int statusCode;
      if (message.isError()) {
        if (message.isServerError()) {
          statusCode = 500;
        } else {
          statusCode = 400;
        }
      } else {
        statusCode = 200;
      }

      template.process(
        new IdFMMessageData(
          branding.htmlTitle(message.messageTitle()),
          message.messageTitle(),
          information.requestId(),
          message.isError(),
          message.isServerError(),
          message.messageTitle(),
          message.message(),
          message.returnTo()
        ),
        writer
      );
      writer.flush();
      return new IdHTTPResponseFixedSize(
        statusCode,
        Set.of(),
        IdUVContentTypes.xhtml(),
        writer.toString().getBytes(UTF_8)
      );
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    } catch (final TemplateException e) {
      throw new IllegalStateException(e);
    }
  }
}

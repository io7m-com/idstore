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
import com.io7m.idstore.server.controller.IdServerStrings;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.controller.user.IdUserLoggedIn;
import com.io7m.idstore.server.controller.user.IdUserLoginService;
import com.io7m.idstore.server.http.IdHTTPServletFunctional;
import com.io7m.idstore.server.http.IdHTTPServletFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPServletRequestInformation;
import com.io7m.idstore.server.http.IdHTTPServletResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPServletResponseRedirect;
import com.io7m.idstore.server.http.IdHTTPServletResponseType;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.templating.IdFMLoginData;
import com.io7m.idstore.server.service.templating.IdFMTemplateServiceType;
import com.io7m.idstore.server.service.templating.IdFMTemplateType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import freemarker.template.TemplateException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Optional;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.model.IdLoginMetadataStandard.remoteHost;
import static com.io7m.idstore.model.IdLoginMetadataStandard.userAgent;

/**
 * The page that displays the login form, or executes the login if a username
 * and password is provided.
 */

public final class IdUVLogin extends IdHTTPServletFunctional
{
  /**
   * The page that displays the login form, or executes the login if a username
   * and password is provided.
   *
   * @param services The services
   */

  public IdUVLogin(
    final RPServiceDirectoryType services)
  {
    super(createCore(services));
  }

  private static IdHTTPServletFunctionalCoreType createCore(
    final RPServiceDirectoryType services)
  {
    final var database =
      services.requireService(IdDatabaseType.class);
    final var branding =
      services.requireService(IdServerBrandingServiceType.class);
    final var logins =
      services.requireService(IdUserLoginService.class);
    final var strings =
      services.requireService(IdServerStrings.class);
    final var template =
      services.requireService(IdFMTemplateServiceType.class)
        .pageLoginTemplate();

    return (request, information) -> {
      return execute(
        database,
        branding,
        logins,
        strings,
        template,
        request,
        information
      );
    };
  }

  private static IdHTTPServletResponseType execute(
    final IdDatabaseType database,
    final IdServerBrandingServiceType branding,
    final IdUserLoginService logins,
    final IdServerStrings strings,
    final IdFMTemplateType<IdFMLoginData> template,
    final HttpServletRequest request,
    final IdHTTPServletRequestInformation information)
  {
    final var username =
      request.getParameter("username");
    final var password =
      request.getParameter("password");
    final var session =
      request.getSession(true);

    if (username == null || password == null) {
      return showLoginForm(branding, template, session, 200);
    }

    try (var connection = database.openConnection(IDSTORE)) {
      try (var transaction = connection.openTransaction()) {
        final var metadata = new HashMap<String, String>(2);
        metadata.put(userAgent(), information.userAgent());
        metadata.put(remoteHost(), information.remoteAddress());

        final IdUserLoggedIn loggedIn;
        try {
          loggedIn = logins.userLogin(
            transaction,
            information.requestId(),
            username,
            password,
            metadata
          );
        } catch (final IdCommandExecutionFailure e) {
          session.setAttribute(
            "ErrorMessage",
            strings.format("errorInvalidUsernamePassword")
          );
          return showLoginForm(branding, template, session, 401);
        }

        transaction.commit();
        session.setAttribute("ID", loggedIn.session().id());
        return new IdHTTPServletResponseRedirect("/");
      }
    } catch (final IdDatabaseException e) {
      session.setAttribute("ErrorMessage", e.getMessage());
      return showLoginForm(branding, template, session, 401);
    }
  }

  /**
   * Display a login form.
   *
   * @param branding   The branding resources
   * @param template   The page template
   * @param session    The HTTP session (for error message displays)
   * @param statusCode The status code
   *
   * @return A login form
   */

  public static IdHTTPServletResponseType showLoginForm(
    final IdServerBrandingServiceType branding,
    final IdFMTemplateType<IdFMLoginData> template,
    final HttpSession session,
    final int statusCode)
  {
    try (var writer = new StringWriter()) {
      template.process(
        new IdFMLoginData(
          branding.htmlTitle("Login"),
          branding.title(),
          true,
          Optional.empty(),
          Optional.ofNullable((String) session.getAttribute("ErrorMessage")),
          branding.loginExtraText()
        ),
        writer
      );

      writer.flush();
      return new IdHTTPServletResponseFixedSize(
        statusCode,
        IdUVContentTypes.xhtml(),
        writer.toString().getBytes(StandardCharsets.UTF_8)
      );
    } catch (final TemplateException e) {
      throw new IllegalStateException(e);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}

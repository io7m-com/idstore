/*
 * Copyright © 2022 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.server.controller.IdServerStrings;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.controller.user.IdUserLoginService;
import com.io7m.idstore.server.http.IdCommonInstrumentedServlet;
import com.io7m.idstore.server.http.IdRequestUniqueIDs;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.templating.IdFMLoginData;
import com.io7m.idstore.server.service.templating.IdFMTemplateServiceType;
import com.io7m.idstore.server.service.templating.IdFMTemplateType;
import com.io7m.idstore.services.api.IdServiceDirectoryType;
import freemarker.template.TemplateException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.model.IdLoginMetadataStandard.remoteHost;
import static com.io7m.idstore.model.IdLoginMetadataStandard.userAgent;
import static com.io7m.idstore.server.http.IdRequestUserAgents.requestUserAgent;

/**
 * The login form.
 */

public final class IdUViewLogin extends IdCommonInstrumentedServlet
{
  private final IdFMTemplateType<IdFMLoginData> template;
  private final IdDatabaseType database;
  private final IdServerBrandingServiceType branding;
  private final IdUserLoginService logins;
  private final IdServerStrings strings;

  /**
   * The login form.
   *
   * @param inServices The service directory
   */

  public IdUViewLogin(
    final IdServiceDirectoryType inServices)
  {
    super(Objects.requireNonNull(inServices, "inServices"));

    this.database =
      inServices.requireService(IdDatabaseType.class);
    this.branding =
      inServices.requireService(IdServerBrandingServiceType.class);
    this.logins =
      inServices.requireService(IdUserLoginService.class);
    this.strings =
      inServices.requireService(IdServerStrings.class);

    this.template =
      inServices.requireService(IdFMTemplateServiceType.class)
        .pageLoginTemplate();
  }

  @Override
  protected void service(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse)
    throws ServletException, IOException
  {
    final var username =
      request.getParameter("username");
    final var password =
      request.getParameter("password");
    final var session =
      request.getSession(true);

    if (username == null || password == null) {
      this.showForm(servletResponse, session);
      return;
    }

    try (var connection =
           this.database.openConnection(IDSTORE)) {
      try (var transaction =
             connection.openTransaction()) {

        final var metadata = new HashMap<String, String>(2);
        metadata.put(userAgent(), requestUserAgent(request));
        metadata.put(remoteHost(), request.getRemoteAddr());

        final IdUserLoginService.IdUserLoggedIn loggedIn;
        try {
          loggedIn = this.logins.userLogin(
            transaction,
            IdRequestUniqueIDs.requestIdFor(request),
            username,
            password,
            metadata
          );
        } catch (final IdCommandExecutionFailure e) {
          session.setAttribute(
            "ErrorMessage",
            this.strings.format("errorInvalidUsernamePassword")
          );

          servletResponse.setStatus(401);
          this.showForm(servletResponse, session);
          return;
        }

        transaction.commit();
        session.setAttribute("ID", loggedIn.session().id());
        servletResponse.sendRedirect("/");
      }
    } catch (final Exception e) {
      throw new ServletException(e);
    }
  }

  private void showForm(
    final HttpServletResponse servletResponse,
    final HttpSession session)
    throws IOException
  {
    servletResponse.setContentType("application/xhtml+xml");

    try (var writer = servletResponse.getWriter()) {
      this.template.process(
        new IdFMLoginData(
          this.branding.htmlTitle("Login"),
          this.branding.title(),
          true,
          Optional.empty(),
          Optional.ofNullable((String) session.getAttribute("ErrorMessage")),
          this.branding.loginExtraText()
        ),
        writer
      );
    } catch (final TemplateException e) {
      throw new IOException(e);
    }
  }
}

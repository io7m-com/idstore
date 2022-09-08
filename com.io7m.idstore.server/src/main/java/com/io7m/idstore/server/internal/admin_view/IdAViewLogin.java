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


package com.io7m.idstore.server.internal.admin_view;

import com.io7m.idstore.database.api.IdDatabaseAdminsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdValidityException;
import com.io7m.idstore.server.internal.IdRequests;
import com.io7m.idstore.server.internal.IdServerBrandingService;
import com.io7m.idstore.server.internal.IdServerStrings;
import com.io7m.idstore.server.internal.freemarker.IdFMLoginData;
import com.io7m.idstore.server.internal.freemarker.IdFMTemplateService;
import com.io7m.idstore.server.internal.freemarker.IdFMTemplateType;
import com.io7m.idstore.services.api.IdServiceDirectoryType;
import freemarker.template.TemplateException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_NONEXISTENT;

/**
 * The login form.
 */

public final class IdAViewLogin extends HttpServlet
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdAViewLogin.class);

  private final IdFMTemplateType<IdFMLoginData> template;
  private final IdDatabaseType database;
  private final IdServerStrings strings;
  private final IdServerBrandingService branding;

  /**
   * The login form.
   *
   * @param inServices The service directory
   */

  public IdAViewLogin(
    final IdServiceDirectoryType inServices)
  {
    Objects.requireNonNull(inServices, "inServices");

    this.database =
      inServices.requireService(IdDatabaseType.class);
    this.strings =
      inServices.requireService(IdServerStrings.class);
    this.branding =
      inServices.requireService(IdServerBrandingService.class);

    this.template =
      inServices.requireService(IdFMTemplateService.class)
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
        final var admins =
          transaction.queries(IdDatabaseAdminsQueriesType.class);
        this.tryLogin(
          request,
          servletResponse,
          session,
          admins,
          username,
          password
        );
        transaction.commit();
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
          false,
          Optional.of(this.strings.format("adminConsole")),
          Optional.ofNullable((String) session.getAttribute("ErrorMessage"))
        ),
        writer
      );
    } catch (final TemplateException e) {
      throw new IOException(e);
    }
  }

  private void tryLogin(
    final HttpServletRequest request,
    final HttpServletResponse response,
    final HttpSession session,
    final IdDatabaseAdminsQueriesType admins,
    final String username,
    final String password)
    throws
    IOException,
    IdPasswordException,
    IdDatabaseException
  {
    if (username == null) {
      this.fail(response, session);
      return;
    }

    if (password == null) {
      this.fail(response, session);
      return;
    }

    final IdAdmin admin;

    try {
      admin = admins.adminGetForNameRequire(new IdName(username));
      final var ok =
        admin.password().check(password);

      if (!ok) {
        this.fail(response, session);
        return;
      }
    } catch (final IdDatabaseException e) {
      if (Objects.equals(e.errorCode(), ADMIN_NONEXISTENT)) {
        this.fail(response, session);
        return;
      }
      throw e;
    } catch (final IdValidityException e) {
      this.fail(response, session);
      return;
    }

    LOG.info("admin '{}' logged in", username);
    session.setAttribute("AdminID", admin.id());

    admins.adminLogin(
      admin.id(),
      IdRequests.requestUserAgent(request),
      request.getRemoteAddr()
    );
    response.sendRedirect("/");
  }

  private void fail(
    final HttpServletResponse response,
    final HttpSession session)
    throws IOException
  {
    session.setAttribute(
      "ErrorMessage",
      this.strings.format("errorInvalidUsernamePassword")
    );

    response.setStatus(401);
    this.showForm(response, session);
  }
}

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

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.model.IdLogin;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.server.internal.IdServerBrandingService;
import com.io7m.idstore.server.internal.IdSessionMessage;
import com.io7m.idstore.server.internal.freemarker.IdFMAdminUserData;
import com.io7m.idstore.server.internal.freemarker.IdFMTemplateService;
import com.io7m.idstore.server.internal.freemarker.IdFMTemplateType;
import com.io7m.idstore.services.api.IdServiceDirectoryType;
import freemarker.template.TemplateException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.server.internal.IdServerRequestDecoration.requestIdFor;

/**
 * The user profile view.
 */

public final class IdAViewUser extends IdAViewAuthenticatedServlet
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdAViewUser.class);

  private final IdFMTemplateType<IdFMAdminUserData> template;
  private final IdServerBrandingService branding;

  /**
   * The users list view.
   *
   * @param inServices The service directory
   */

  public IdAViewUser(
    final IdServiceDirectoryType inServices)
  {
    super(inServices);

    this.branding =
      inServices.requireService(IdServerBrandingService.class);
    this.template =
      inServices.requireService(IdFMTemplateService.class)
        .pageAdminUserTemplate();
  }

  @Override
  protected Logger logger()
  {
    return LOG;
  }

  @Override
  protected void serviceAuthenticated(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse,
    final HttpSession session)
    throws Exception
  {
    final var userController =
      this.userController();
    final var strings =
      this.strings();
    final var messageServlet =
      new IdAViewMessage(this.services());

    final var id = request.getParameter("id");
    if (id == null) {
      userController.messageCurrentSet(
        new IdSessionMessage(
          requestIdFor(request),
          true,
          false,
          strings.format("error"),
          strings.format("missingParameter", "id"),
          "/"
        )
      );
      messageServlet.service(request, servletResponse);
      return;
    }

    final UUID userId;

    try {
      userId = UUID.fromString(id);
    } catch (final Exception e) {
      userController.messageCurrentSet(
        new IdSessionMessage(
          requestIdFor(request),
          true,
          false,
          strings.format("error"),
          strings.format("invalidParameter", "id"),
          "/"
        )
      );
      messageServlet.service(request, servletResponse);
      return;
    }

    final var userAndLogins =
      this.fetchUser(userId);

    servletResponse.setContentType("application/xhtml+xml");

    try (var writer = servletResponse.getWriter()) {
      this.template.process(
        new IdFMAdminUserData(
          this.branding.htmlTitle("User"),
          this.branding.title(),
          userAndLogins.user(),
          userAndLogins.logins()
        ),
        writer
      );
    } catch (final TemplateException e) {
      throw new IOException(e);
    }
  }

  private record UserAndLogin(
    IdUser user,
    List<IdLogin> logins)
  {

  }

  private UserAndLogin fetchUser(
    final UUID userId)
    throws IdDatabaseException
  {
    final var database = this.database();
    try (var connection =
           database.openConnection(IDSTORE)) {
      try (var transaction =
             connection.openTransaction()) {
        final var queries =
          transaction.queries(IdDatabaseUsersQueriesType.class);
        final var user =
          queries.userGetRequire(userId);
        final var logins =
          queries.userLoginHistory(userId, 180);
        return new UserAndLogin(user, logins);
      }
    }
  }
}

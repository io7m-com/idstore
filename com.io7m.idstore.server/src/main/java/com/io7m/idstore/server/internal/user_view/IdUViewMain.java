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

package com.io7m.idstore.server.internal.user_view;

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.model.IdLogin;
import com.io7m.idstore.server.internal.freemarker.IdFMTemplateService;
import com.io7m.idstore.server.internal.freemarker.IdFMTemplateType;
import com.io7m.idstore.server.internal.freemarker.IdFMUserSelfData;
import com.io7m.idstore.services.api.IdServiceDirectoryType;
import freemarker.template.TemplateException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;

/**
 * The main view.
 */

public final class IdUViewMain extends IdUViewAuthenticatedServlet
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdUViewMain.class);

  private final IdFMTemplateType<IdFMUserSelfData> template;

  /**
   * The main view.
   *
   * @param inServices The service directory
   */

  public IdUViewMain(
    final IdServiceDirectoryType inServices)
  {
    super(inServices);

    this.template =
      inServices.requireService(IdFMTemplateService.class)
        .pageUserSelfTemplate();
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
    servletResponse.setContentType("application/xhtml+xml");

    try (var writer = servletResponse.getWriter()) {
      this.template.process(
        new IdFMUserSelfData(
          "User Profile",
          "User Profile",
          this.user(),
          this.loginHistory()
        ),
        writer
      );
    } catch (final TemplateException e) {
      throw new IOException(e);
    }
  }

  private List<IdLogin> loginHistory()
    throws IdDatabaseException
  {
    try (var c = this.database().openConnection(IDSTORE)) {
      try (var t = c.openTransaction()) {
        final var u = t.queries(IdDatabaseUsersQueriesType.class);
        return u.userLoginHistory(this.userId(), 30);
      }
    }
  }
}

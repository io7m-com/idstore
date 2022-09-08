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
import com.io7m.idstore.database.api.IdDatabaseUserListPagingType;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.model.IdUserListParameters;
import com.io7m.idstore.model.IdUserSummary;
import com.io7m.idstore.server.internal.IdServerBrandingService;
import com.io7m.idstore.server.internal.IdServerUserController;
import com.io7m.idstore.server.internal.freemarker.IdFMAdminUsersData;
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
import java.util.Objects;
import java.util.Optional;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;

/**
 * The users list view.
 */

public final class IdAViewUsers extends IdAViewAuthenticatedServlet
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdAViewUsers.class);

  private final IdFMTemplateType<IdFMAdminUsersData> template;
  private final IdServerBrandingService branding;

  /**
   * The users list view.
   *
   * @param inServices The service directory
   */

  public IdAViewUsers(
    final IdServiceDirectoryType inServices)
  {
    super(inServices);

    this.branding =
      inServices.requireService(IdServerBrandingService.class);
    this.template =
      inServices.requireService(IdFMTemplateService.class)
        .pageAdminUsersTemplate();
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

    updateFilterIfNecessary(request, userController);

    final var paging =
      userController.userPaging();
    final var users =
      this.fetchUsers(request, paging);

    servletResponse.setContentType("application/xhtml+xml");

    try (var writer = servletResponse.getWriter()) {
      this.template.process(
        new IdFMAdminUsersData(
          this.branding.htmlTitle("Users"),
          this.branding.title(),
          users,
          paging.pagePreviousAvailable(),
          paging.pageNextAvailable(),
          paging.pageNumber(),
          paging.pageCount(),
          paging.pageParameters().search()
        ),
        writer
      );
    } catch (final TemplateException e) {
      throw new IOException(e);
    }
  }

  private static void updateFilterIfNecessary(
    final HttpServletRequest request,
    final IdServerUserController userController)
  {
    final var parameters =
      userController.userListParameters();

    final var filterGivenRaw =
      request.getParameter("filter");

    /*
     * If no particular filter parameter was given, then leave the current
     * set of parameters unchanged.
     */

    if (filterGivenRaw == null) {
      return;
    }

    /*
     * If the filter is an empty string, then unset the filter.
     */

    if (Objects.equals(filterGivenRaw, "")) {
      userController.setUserListParameters(
        new IdUserListParameters(
          parameters.timeCreatedRange(),
          parameters.timeUpdatedRange(),
          Optional.empty(),
          parameters.ordering(),
          parameters.limit()
        )
      );
      return;
    }

    /*
     * Otherwise, set the filter to the new value.
     */

    userController.setUserListParameters(
      new IdUserListParameters(
        parameters.timeCreatedRange(),
        parameters.timeUpdatedRange(),
        Optional.of(filterGivenRaw),
        parameters.ordering(),
        parameters.limit()
      )
    );
  }

  private List<IdUserSummary> fetchUsers(
    final HttpServletRequest request,
    final IdDatabaseUserListPagingType paging)
    throws IdDatabaseException
  {
    final var database = this.database();
    try (var connection =
           database.openConnection(IDSTORE)) {
      try (var transaction =
             connection.openTransaction()) {
        final var queries =
          transaction.queries(IdDatabaseUsersQueriesType.class);

        if (pageNextRequested(request)) {
          return paging.pageNext(queries);
        }
        if (pagePreviousRequested(request)) {
          return paging.pagePrevious(queries);
        }
        return paging.pageCurrent(queries);
      }
    }
  }

  private static boolean pagePreviousRequested(
    final HttpServletRequest request)
  {
    return Objects.equals("true", request.getParameter("prev"));
  }

  private static boolean pageNextRequested(
    final HttpServletRequest request)
  {
    return Objects.equals("true", request.getParameter("next"));
  }
}

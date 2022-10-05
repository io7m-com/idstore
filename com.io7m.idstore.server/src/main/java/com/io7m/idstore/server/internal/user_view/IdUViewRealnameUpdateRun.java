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

import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdValidityException;
import com.io7m.idstore.protocol.user.IdUCommandRealnameUpdate;
import com.io7m.idstore.server.internal.IdSessionMessage;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.internal.user.IdUCmdRealNameUpdate;
import com.io7m.idstore.server.internal.user.IdUCommandContext;
import com.io7m.idstore.services.api.IdServiceDirectoryType;
import com.io7m.jvindicator.core.Vindication;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.server.internal.IdServerRequestDecoration.requestIdFor;

/**
 * The page that executes the realname update.
 */

public final class IdUViewRealnameUpdateRun extends IdUViewAuthenticatedServlet
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdUViewRealnameUpdateRun.class);

  /**
   * The page that executes the realname update.
   *
   * @param inServices The service directory
   */

  public IdUViewRealnameUpdateRun(
    final IdServiceDirectoryType inServices)
  {
    super(inServices);
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
    throws IOException, ServletException
  {
    final var strings =
      this.strings();
    final var messageServlet =
      new IdUViewMessage(this.services());

    try {
      final var vindicator =
        Vindication.startWithExceptions(IdValidityException::new);
      final var realnameParameter =
        vindicator.addRequiredParameter("realname", IdRealName::new);

      vindicator.check(request.getParameterMap());

      final var database = this.database();
      try (var connection = database.openConnection(IDSTORE)) {
        try (var transaction = connection.openTransaction()) {
          final var context =
            IdUCommandContext.create(
              this.services(),
              transaction,
              request,
              this.user(),
              this.userSession()
            );

          final var command =
            new IdUCommandRealnameUpdate(realnameParameter.get());
          new IdUCmdRealNameUpdate()
            .execute(context, command);

          transaction.commit();
          servletResponse.sendRedirect("/");
        }
      }
    } catch (final IdCommandExecutionFailure e) {
      this.userSession().messageCurrentSet(
        new IdSessionMessage(
          requestIdFor(request),
          true,
          e.httpStatusCode() >= 500,
          strings.format("error"),
          e.getMessage(),
          "/"
        )
      );
      messageServlet.service(request, servletResponse);
    } catch (final IdValidityException e) {
      this.userSession().messageCurrentSet(
        new IdSessionMessage(
          requestIdFor(request),
          true,
          false,
          strings.format("error"),
          e.getMessage(),
          "/realname-update"
        )
      );
      messageServlet.service(request, servletResponse);
    } catch (final Exception e) {
      this.userSession().messageCurrentSet(
        new IdSessionMessage(
          requestIdFor(request),
          true,
          true,
          strings.format("error"),
          e.getMessage(),
          "/realname-update"
        )
      );
      messageServlet.service(request, servletResponse);
    }
  }
}

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

import com.io7m.idstore.protocol.user_v1.IdU1CommandEmailRemoveBegin;
import com.io7m.idstore.server.internal.IdSessionMessage;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.internal.user_v1.IdU1CmdEmailRemoveBegin;
import com.io7m.idstore.server.internal.user_v1.IdU1CommandContext;
import com.io7m.idstore.services.api.IdServiceDirectoryType;
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
 * The page that executes the email removal.
 */

public final class IdUViewEmailRemoveRun extends IdUViewAuthenticatedServlet
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdUViewEmailRemoveRun.class);

  /**
   * The page that executes the email removal.
   *
   * @param inServices The service directory
   */

  public IdUViewEmailRemoveRun(
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
    final var userController =
      this.userController();
    final var strings =
      this.strings();
    final var messageServlet =
      new IdUViewMessage(this.services());

    final var emailParameter = request.getParameter("email");
    if (emailParameter == null) {
      userController.messageCurrentSet(
        new IdSessionMessage(
          requestIdFor(request),
          true,
          false,
          strings.format("error"),
          strings.format("missingParameter", "email"),
          "/"
        )
      );
      messageServlet.service(request, servletResponse);
      return;
    }

    try {
      final var database = this.database();
      try (var connection = database.openConnection(IDSTORE)) {
        try (var transaction = connection.openTransaction()) {
          final var context =
            IdU1CommandContext.create(
              this.services(),
              transaction,
              request,
              this.user()
            );

          new IdU1CmdEmailRemoveBegin()
            .execute(context, new IdU1CommandEmailRemoveBegin(emailParameter));

          transaction.commit();

          userController.messageCurrentSet(
            new IdSessionMessage(
              requestIdFor(request),
              false,
              false,
              strings.format("emailVerificationTitle"),
              strings.format("emailVerificationSent", emailParameter),
              "/"
            )
          );

          messageServlet.service(request, servletResponse);
        }
      }
    } catch (final IdCommandExecutionFailure e) {
      userController.messageCurrentSet(
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
    } catch (final Exception e) {
      userController.messageCurrentSet(
        new IdSessionMessage(
          requestIdFor(request),
          true,
          true,
          strings.format("error"),
          e.getMessage(),
          "/"
        )
      );
      messageServlet.service(request, servletResponse);
    }
  }
}

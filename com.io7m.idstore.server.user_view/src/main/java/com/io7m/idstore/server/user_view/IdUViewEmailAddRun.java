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

import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdValidityException;
import com.io7m.idstore.protocol.user.IdUCommandEmailAddBegin;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.controller.user.IdUCmdEmailAddBegin;
import com.io7m.idstore.server.controller.user.IdUCommandContext;
import com.io7m.idstore.server.http.IdRequestUniqueIDs;
import com.io7m.idstore.server.http.IdRequestUserAgents;
import com.io7m.idstore.server.service.sessions.IdSessionMessage;
import com.io7m.idstore.server.service.sessions.IdSessionUser;
import com.io7m.jvindicator.core.Vindication;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;

/**
 * The page that executes the email addition.
 */

public final class IdUViewEmailAddRun extends IdUViewAuthenticatedServlet
{
  /**
   * The page that executes the email addition.
   *
   * @param inServices The service directory
   */

  public IdUViewEmailAddRun(
    final RPServiceDirectoryType inServices)
  {
    super(inServices);
  }

  @Override
  protected void serviceAuthenticated(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse,
    final IdSessionUser session)
    throws IOException, ServletException
  {
    final var strings =
      this.strings();
    final var messageServlet =
      new IdUViewMessage(this.services());

    try {
      final var vindicator =
        Vindication.startWithExceptions(IdValidityException::new);
      final var emailParameter =
        vindicator.addRequiredParameter("email", IdEmail::new);

      vindicator.check(request.getParameterMap());

      final var database = this.database();
      try (var connection = database.openConnection(IDSTORE)) {
        try (var transaction = connection.openTransaction()) {
          final var context =
            new IdUCommandContext(
              this.services(),
              IdRequestUniqueIDs.requestIdFor(request),
              transaction,
              session,
              this.user(),
              request.getRemoteAddr(),
              IdRequestUserAgents.requestUserAgent(request)
            );

          final var email =
            emailParameter.get();
          final var command =
            new IdUCommandEmailAddBegin(email);
          new IdUCmdEmailAddBegin()
            .execute(context, command);

          transaction.commit();

          session.messageCurrentSet(
            new IdSessionMessage(
              IdRequestUniqueIDs.requestIdFor(request),
              false,
              false,
              strings.format("emailVerificationTitle"),
              strings.format("emailVerificationSent", email),
              "/"
            )
          );

          messageServlet.service(request, servletResponse);
        }
      }
    } catch (final IdCommandExecutionFailure e) {
      session.messageCurrentSet(
        new IdSessionMessage(
          IdRequestUniqueIDs.requestIdFor(request),
          true,
          e.httpStatusCode() >= 500,
          strings.format("error"),
          e.getMessage(),
          "/"
        )
      );
      messageServlet.service(request, servletResponse);
    } catch (final IdValidityException e) {
      session.messageCurrentSet(
        new IdSessionMessage(
          IdRequestUniqueIDs.requestIdFor(request),
          true,
          false,
          strings.format("error"),
          e.getMessage(),
          "/email-add"
        )
      );
      messageServlet.service(request, servletResponse);
    } catch (final Exception e) {
      session.messageCurrentSet(
        new IdSessionMessage(
          IdRequestUniqueIDs.requestIdFor(request),
          true,
          true,
          strings.format("error"),
          e.getMessage(),
          "/email-add"
        )
      );
      messageServlet.service(request, servletResponse);
    }
  }
}

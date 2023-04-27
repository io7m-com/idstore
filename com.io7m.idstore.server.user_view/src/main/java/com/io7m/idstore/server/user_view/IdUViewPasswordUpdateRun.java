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

import com.io7m.idstore.model.IdValidityException;
import com.io7m.idstore.protocol.user.IdUCommandPasswordUpdate;
import com.io7m.idstore.server.controller.IdServerStrings;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.controller.user.IdUCmdPasswordUpdate;
import com.io7m.idstore.server.controller.user.IdUCommandContext;
import com.io7m.idstore.server.http.IdRequestUniqueIDs;
import com.io7m.idstore.server.http.IdRequestUserAgents;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.sessions.IdSessionMessage;
import com.io7m.idstore.server.service.sessions.IdSessionUser;
import com.io7m.idstore.server.service.templating.IdFMMessageData;
import com.io7m.idstore.server.service.templating.IdFMTemplateServiceType;
import com.io7m.idstore.server.service.templating.IdFMTemplateType;
import com.io7m.jvindicator.core.Vindication;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import freemarker.template.TemplateException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;

/**
 * The page that executes the password update.
 */

public final class IdUViewPasswordUpdateRun extends IdUViewAuthenticatedServlet
{
  private final IdServerBrandingServiceType branding;
  private final IdFMTemplateType<IdFMMessageData> msgTemplate;

  /**
   * The page that executes the password update.
   *
   * @param inServices The service directory
   */

  public IdUViewPasswordUpdateRun(
    final RPServiceDirectoryType inServices)
  {
    super(inServices);

    this.branding =
      inServices.requireService(IdServerBrandingServiceType.class);
    this.msgTemplate =
      inServices.requireService(IdFMTemplateServiceType.class)
        .pageMessage();
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
      final var password0Parameter =
        vindicator.addRequiredParameter("password0", Vindication.strings());
      final var password1Parameter =
        vindicator.addRequiredParameter("password1", Vindication.strings());

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

          final var command =
            new IdUCommandPasswordUpdate(
              password0Parameter.get(),
              password1Parameter.get()
            );

          new IdUCmdPasswordUpdate()
            .execute(context, command);

          transaction.commit();
          this.showConfirmed(strings, request, servletResponse);
        }
      }
    } catch (final IdCommandExecutionFailure e) {
      this.userSession().messageCurrentSet(
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
      this.userSession().messageCurrentSet(
        new IdSessionMessage(
          IdRequestUniqueIDs.requestIdFor(request),
          true,
          false,
          strings.format("error"),
          e.getMessage(),
          "/password-update"
        )
      );
      messageServlet.service(request, servletResponse);
    } catch (final Exception e) {
      this.userSession().messageCurrentSet(
        new IdSessionMessage(
          IdRequestUniqueIDs.requestIdFor(request),
          true,
          true,
          strings.format("error"),
          e.getMessage(),
          "/password-update"
        )
      );
      messageServlet.service(request, servletResponse);
    }
  }

  private void showConfirmed(
    final IdServerStrings strings,
    final HttpServletRequest request,
    final HttpServletResponse response)
    throws IOException
  {
    response.setStatus(200);

    try (var writer = response.getWriter()) {
      this.msgTemplate.process(
        new IdFMMessageData(
          this.branding.htmlTitle(strings.format("passwordUpdateSuccessTitle")),
          this.branding.title(),
          IdRequestUniqueIDs.requestIdFor(request),
          false,
          false,
          strings.format("passwordUpdateSuccessTitle"),
          strings.format("passwordUpdateSuccess"),
          "/"
        ),
        writer
      );
    } catch (final TemplateException e) {
      throw new IOException(e);
    }
  }
}

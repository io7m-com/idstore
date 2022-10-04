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

import com.io7m.idstore.model.IdValidityException;
import com.io7m.idstore.protocol.user.IdUCommandPasswordUpdate;
import com.io7m.idstore.server.internal.IdServerBrandingService;
import com.io7m.idstore.server.internal.IdServerStrings;
import com.io7m.idstore.server.internal.IdSessionMessage;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.internal.freemarker.IdFMMessageData;
import com.io7m.idstore.server.internal.freemarker.IdFMTemplateService;
import com.io7m.idstore.server.internal.freemarker.IdFMTemplateType;
import com.io7m.idstore.server.internal.user.IdUCmdPasswordUpdate;
import com.io7m.idstore.server.internal.user.IdUCommandContext;
import com.io7m.idstore.services.api.IdServiceDirectoryType;
import com.io7m.jvindicator.core.Vindication;
import freemarker.template.TemplateException;
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
 * The page that executes the password update.
 */

public final class IdUViewPasswordUpdateRun extends IdUViewAuthenticatedServlet
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdUViewPasswordUpdateRun.class);

  private final IdServerBrandingService branding;
  private final IdFMTemplateType<IdFMMessageData> msgTemplate;

  /**
   * The page that executes the password update.
   *
   * @param inServices The service directory
   */

  public IdUViewPasswordUpdateRun(
    final IdServiceDirectoryType inServices)
  {
    super(inServices);

    this.branding =
      inServices.requireService(IdServerBrandingService.class);
    this.msgTemplate =
      inServices.requireService(IdFMTemplateService.class)
        .pageMessage();
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
            IdUCommandContext.create(
              this.services(),
              transaction,
              request,
              session,
              this.user()
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
    } catch (final IdValidityException e) {
      userController.messageCurrentSet(
        new IdSessionMessage(
          requestIdFor(request),
          true,
          false,
          strings.format("error"),
          e.getMessage(),
          "/password-update"
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
          requestIdFor(request),
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

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

import com.io7m.idstore.server.internal.IdServerBrandingService;
import com.io7m.idstore.server.internal.IdServerUserControllersService;
import com.io7m.idstore.server.internal.freemarker.IdFMMessageData;
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
import java.util.UUID;

import static com.io7m.idstore.server.internal.IdServerRequestDecoration.requestIdFor;

/**
 * The generic message page.
 */

public final class IdUViewMessage extends IdUViewAuthenticatedServlet
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdUViewMessage.class);

  private final IdFMTemplateType<IdFMMessageData> template;
  private final IdServerBrandingService branding;

  /**
   * The generic message page.
   *
   * @param inServices The service directory
   */

  public IdUViewMessage(
    final IdServiceDirectoryType inServices)
  {
    super(inServices);

    this.branding =
      inServices.requireService(IdServerBrandingService.class);
    this.template =
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
    throws Exception
  {
    final var userController =
      this.services()
        .requireService(IdServerUserControllersService.class)
        .createOrGet((UUID) session.getAttribute("UserID"), session.getId());

    servletResponse.setContentType("application/xhtml+xml");

    final var messageOpt = userController.messageCurrent();
    if (messageOpt.isEmpty()) {
      servletResponse.sendRedirect("/");
      return;
    }

    try (var writer = servletResponse.getWriter()) {
      final var message = messageOpt.get();

      if (message.isError()) {
        if (message.isServerError()) {
          servletResponse.setStatus(500);
        } else {
          servletResponse.setStatus(400);
        }
      } else {
        servletResponse.setStatus(200);
      }

      this.template.process(
        new IdFMMessageData(
          this.branding.htmlTitle(message.messageTitle()),
          message.messageTitle(),
          requestIdFor(request),
          message.isError(),
          message.isServerError(),
          message.messageTitle(),
          message.message(),
          message.returnTo()
        ),
        writer
      );
    } catch (final TemplateException e) {
      throw new IOException(e);
    }
  }
}

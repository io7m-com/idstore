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


package com.io7m.idstore.server.user_view;

import com.io7m.idstore.server.http.IdRequestUniqueIDs;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.sessions.IdSessionUser;
import com.io7m.idstore.server.service.templating.IdFMMessageData;
import com.io7m.idstore.server.service.templating.IdFMTemplateServiceType;
import com.io7m.idstore.server.service.templating.IdFMTemplateType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import freemarker.template.TemplateException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * The generic message page.
 */

public final class IdUViewMessage extends IdUViewAuthenticatedServlet
{
  private final IdFMTemplateType<IdFMMessageData> template;
  private final IdServerBrandingServiceType branding;

  /**
   * The generic message page.
   *
   * @param inServices The service directory
   */

  public IdUViewMessage(
    final RPServiceDirectoryType inServices)
  {
    super(inServices);

    this.branding =
      inServices.requireService(IdServerBrandingServiceType.class);
    this.template =
      inServices.requireService(IdFMTemplateServiceType.class)
        .pageMessage();
  }

  @Override
  protected void serviceAuthenticated(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse,
    final IdSessionUser session)
    throws Exception
  {
    servletResponse.setContentType("application/xhtml+xml");

    final var messageOpt = session.messageCurrent();
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
          IdRequestUniqueIDs.requestIdFor(request),
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

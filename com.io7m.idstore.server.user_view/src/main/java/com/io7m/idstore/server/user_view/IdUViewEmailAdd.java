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
import com.io7m.idstore.server.service.branding.IdServerBrandingService;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.sessions.IdSessionUser;
import com.io7m.idstore.server.service.templating.IdFMEmailAddData;
import com.io7m.idstore.server.service.templating.IdFMTemplateService;
import com.io7m.idstore.server.service.templating.IdFMTemplateType;
import com.io7m.idstore.services.api.IdServiceDirectoryType;
import freemarker.template.TemplateException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * The email addition form.
 */

public final class IdUViewEmailAdd extends IdUViewAuthenticatedServlet
{
  private final IdFMTemplateType<IdFMEmailAddData> template;
  private final IdServerBrandingServiceType branding;

  /**
   * The email addition form.
   *
   * @param inServices The service directory
   */

  public IdUViewEmailAdd(
    final IdServiceDirectoryType inServices)
  {
    super(inServices);

    this.branding =
      inServices.requireService(IdServerBrandingService.class);
    this.template =
      inServices.requireService(IdFMTemplateService.class)
        .pageEmailAddTemplate();
  }

  @Override
  protected void serviceAuthenticated(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse,
    final IdSessionUser session)
    throws Exception
  {
    servletResponse.setContentType("application/xhtml+xml");

    try (var writer = servletResponse.getWriter()) {
      this.template.process(
        new IdFMEmailAddData(
          this.branding.htmlTitle("Add an email address."),
          this.branding.title(),
          IdRequestUniqueIDs.requestIdFor(request)
        ),
        writer
      );
    } catch (final TemplateException e) {
      throw new IOException(e);
    }
  }
}

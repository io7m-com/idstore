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

import com.io7m.idstore.server.http.IdCommonInstrumentedServlet;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.templating.IdFMPasswordResetData;
import com.io7m.idstore.server.service.templating.IdFMTemplateServiceType;
import com.io7m.idstore.server.service.templating.IdFMTemplateType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import freemarker.template.TemplateException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * The page that allows for entering a username or password to begin a
 * password reset operation.
 */

public final class IdUViewPasswordReset extends IdCommonInstrumentedServlet
{
  private final IdFMTemplateType<IdFMPasswordResetData> template;
  private final IdServerBrandingServiceType branding;

  /**
   * The page that allows for entering a username or password to begin a
   * password reset operation.
   *
   * @param inServices The service directory
   */

  public IdUViewPasswordReset(
    final RPServiceDirectoryType inServices)
  {
    super(inServices);

    this.branding =
      inServices.requireService(IdServerBrandingServiceType.class);
    this.template =
      inServices.requireService(IdFMTemplateServiceType.class)
        .pagePasswordResetTemplate();
  }

  @Override
  protected void service(
    final HttpServletRequest request,
    final HttpServletResponse response)
    throws ServletException, IOException
  {
    response.setContentType("application/xhtml+xml");

    try (var writer = response.getWriter()) {
      this.template.process(
        new IdFMPasswordResetData(
          this.branding.htmlTitle("Reset password."),
          this.branding.title()
        ),
        writer
      );
    } catch (final TemplateException e) {
      throw new IOException(e);
    }
  }
}

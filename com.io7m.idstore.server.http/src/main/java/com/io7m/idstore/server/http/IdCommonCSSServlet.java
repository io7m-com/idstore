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

package com.io7m.idstore.server.http;

import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.services.api.IdServiceDirectoryType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The CSS servlet.
 */

public final class IdCommonCSSServlet extends IdCommonInstrumentedServlet
{
  private static final Pattern LEADING_SLASHES =
    Pattern.compile("^/+");

  private final IdServerBrandingServiceType branding;

  /**
   * The CSS servlet.
   *
   * @param inServices The service directory
   */

  public IdCommonCSSServlet(
    final IdServiceDirectoryType inServices)
  {
    super(Objects.requireNonNull(inServices, "services"));

    this.branding =
      inServices.requireService(IdServerBrandingServiceType.class);
  }

  @Override
  protected void service(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse)
    throws IOException
  {
    final var path =
      request.getPathInfo();
    final var stripped =
      LEADING_SLASHES.matcher(path)
        .replaceFirst("");

    if (Objects.equals(stripped, "reset.css")) {
      servletResponse.setStatus(200);
      servletResponse.setContentType("text/css; charset=utf-8");
      try (var stream = IdCommonCSSServlet.class.getResourceAsStream("/com/io7m/idstore/server/http/reset.css")) {
        try (var output = servletResponse.getOutputStream()) {
          stream.transferTo(output);
          output.flush();
        }
      }
      return;
    }

    if (Objects.equals(stripped, "style.css")) {
      servletResponse.setStatus(200);
      servletResponse.setContentType("text/css; charset=utf-8");
      try (var output = servletResponse.getOutputStream()) {
        output.print(this.branding.css());
        output.flush();
      }
      return;
    }

    if (Objects.equals(stripped, "xbutton.css")) {
      servletResponse.setStatus(200);
      servletResponse.setContentType("text/css; charset=utf-8");
      try (var output = servletResponse.getOutputStream()) {
        output.print(this.branding.xButtonCSS());
        output.flush();
      }
      return;
    }

    servletResponse.setStatus(404);
  }
}

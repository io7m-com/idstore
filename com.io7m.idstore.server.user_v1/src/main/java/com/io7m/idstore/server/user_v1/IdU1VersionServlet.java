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

package com.io7m.idstore.server.user_v1;

import com.io7m.idstore.server.http.IdCommonInstrumentedServlet;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * A servlet for showing the server version.
 */

public final class IdU1VersionServlet extends IdCommonInstrumentedServlet
{
  /**
   * A servlet for showing the server version.
   *
   * @param inServices The service directory
   */

  public IdU1VersionServlet(
    final RPServiceDirectoryType inServices)
  {
    super(inServices);
  }

  @Override
  protected void service(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse)
    throws IOException
  {
    final var text =
      String.format(
        "com.io7m.idstore %s %s\r\n\r\n",
        IdU1Version.SERVER_VERSION,
        IdU1Version.SERVER_BUILD
      );

    final var textBytes =
      text.getBytes(StandardCharsets.UTF_8);

    servletResponse.setStatus(200);
    servletResponse.setContentType("text/plain");
    servletResponse.setContentLength(textBytes.length);

    try (var stream = servletResponse.getOutputStream()) {
      stream.write(textBytes);
      stream.flush();
    }
  }
}

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

import com.io7m.idstore.services.api.IdServiceDirectoryType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import static java.util.Map.entry;

/**
 * The static resource servlet.
 */

public final class IdUViewResource extends HttpServlet
{
  private static final Pattern LEADING_SLASHES =
    Pattern.compile("^/+");

  private static final Map<String, String> RESOURCES =
    Map.ofEntries(
      entry("style.css", "text/css"),
      entry("reset.css", "text/css"),
      entry("idstore.svg", "image/svg+xml")
    );

  /**
   * The static resource servlet.
   *
   * @param inServices The service directory
   */

  public IdUViewResource(
    final IdServiceDirectoryType inServices)
  {
    Objects.requireNonNull(inServices, "inServices");
  }

  @Override
  protected void service(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse)
    throws ServletException, IOException
  {
    final var path =
      request.getPathInfo();
    final var stripped =
      LEADING_SLASHES.matcher(path)
        .replaceFirst("");

    final var contentType = RESOURCES.get(stripped);
    if (contentType != null) {
      final var resourcePath =
        "/com/io7m/idstore/server/internal/%s".formatted(stripped);

      servletResponse.setStatus(200);
      servletResponse.setContentType(contentType);
      try (var stream = IdUViewResource.class.getResourceAsStream(resourcePath)) {
        try (var output = servletResponse.getOutputStream()) {
          stream.transferTo(output);
          output.flush();
        }
      }
      return;
    }

    servletResponse.setStatus(404);
  }
}

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

import com.io7m.idstore.server.http.IdHTTPServletFunctional;
import com.io7m.idstore.server.http.IdHTTPServletFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPServletResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPServletResponseType;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.regex.Pattern;

import static com.io7m.idstore.model.IdUserDomain.USER;
import static com.io7m.idstore.server.http.IdHTTPServletCoreInstrumented.withInstrumentation;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A CSS servlet.
 */

public final class IdUVCSS extends IdHTTPServletFunctional
{
  private static final Pattern LEADING_SLASHES =
    Pattern.compile("^/+");

  private static final byte[] NOTHING =
    new byte[0];

  /**
   * A CSS servlet.
   *
   * @param services The service directory
   */

  public IdUVCSS(
    final RPServiceDirectoryType services)
  {
    super(createCore(services));
  }

  private static IdHTTPServletFunctionalCoreType createCore(
    final RPServiceDirectoryType services)
  {
    final var branding =
      services.requireService(IdServerBrandingServiceType.class);

    final byte[] resetCssData;
    try (var stream =
           IdUVCSS.class.getResourceAsStream(
             "/com/io7m/idstore/server/user_view/reset.css")) {
      resetCssData = stream.readAllBytes();
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }

    final IdHTTPServletFunctionalCoreType main = (request, information) -> {
      return execute(request, branding, resetCssData);
    };

    return withInstrumentation(services, USER, main);
  }

  private static IdHTTPServletResponseType execute(
    final HttpServletRequest request,
    final IdServerBrandingServiceType branding,
    final byte[] resetCssData)
  {
    final var path =
      request.getPathInfo();
    final var stripped =
      LEADING_SLASHES.matcher(path)
        .replaceFirst("");

    if (Objects.equals(stripped, "reset.css")) {
      return new IdHTTPServletResponseFixedSize(
        200,
        "text/css; charset=utf-8",
        resetCssData
      );
    }

    if (Objects.equals(stripped, "style.css")) {
      return new IdHTTPServletResponseFixedSize(
        200,
        "text/css; charset=utf-8",
        branding.css().getBytes(UTF_8)
      );
    }

    if (Objects.equals(stripped, "xbutton.css")) {
      return new IdHTTPServletResponseFixedSize(
        200,
        "text/css; charset=utf-8",
        branding.xButtonCSS().getBytes(UTF_8)
      );
    }

    return new IdHTTPServletResponseFixedSize(
      404,
      "text/plain",
      NOTHING
    );
  }
}

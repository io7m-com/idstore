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

import com.io7m.idstore.server.http.IdHTTPHandlerFunctional;
import com.io7m.idstore.server.http.IdHTTPHandlerFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPResponseType;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import io.helidon.webserver.http.ServerRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import static com.io7m.idstore.model.IdUserDomain.USER;
import static com.io7m.idstore.server.http.IdHTTPHandlerCoreInstrumented.withInstrumentation;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A CSS servlet.
 */

public final class IdUVCSS extends IdHTTPHandlerFunctional
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

  private static IdHTTPHandlerFunctionalCoreType createCore(
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

    final IdHTTPHandlerFunctionalCoreType main = (request, information) -> {
      return execute(request, branding, resetCssData);
    };

    return withInstrumentation(services, USER, main);
  }

  private static IdHTTPResponseType execute(
    final ServerRequest request,
    final IdServerBrandingServiceType branding,
    final byte[] resetCssData)
  {
    final var path =
      request.path()
        .path();
    final var stripped =
      LEADING_SLASHES.matcher(path)
        .replaceFirst("");

    if (Objects.equals(stripped, "css/reset.css")) {
      return new IdHTTPResponseFixedSize(
        200,
        Set.of(),
        "text/css; charset=utf-8",
        resetCssData
      );
    }

    if (Objects.equals(stripped, "css/style.css")) {
      return new IdHTTPResponseFixedSize(
        200,
        Set.of(),
        "text/css; charset=utf-8",
        branding.css().getBytes(UTF_8)
      );
    }

    if (Objects.equals(stripped, "css/xbutton.css")) {
      return new IdHTTPResponseFixedSize(
        200,
        Set.of(),
        "text/css; charset=utf-8",
        branding.xButtonCSS().getBytes(UTF_8)
      );
    }

    return new IdHTTPResponseFixedSize(
      404,
      Set.of(),
      "text/plain",
      NOTHING
    );
  }
}

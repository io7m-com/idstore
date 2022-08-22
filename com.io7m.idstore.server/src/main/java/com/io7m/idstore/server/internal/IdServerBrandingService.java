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


package com.io7m.idstore.server.internal;

import com.io7m.idstore.server.api.IdServerBrandingConfiguration;
import com.io7m.idstore.server.api.IdServerColorScheme;
import com.io7m.idstore.server.internal.freemarker.IdFMCSSData;
import com.io7m.idstore.server.internal.freemarker.IdFMTemplateService;
import com.io7m.idstore.server.internal.freemarker.IdFMTemplateType;
import com.io7m.idstore.services.api.IdServiceType;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * The service that supplies branding information.
 */

public final class IdServerBrandingService implements IdServiceType
{
  private final byte[] logo;
  private final String css;
  private final String title;

  private IdServerBrandingService(
    final byte[] inLogo,
    final String inCss,
    final String inTitle)
  {
    this.logo =
      Objects.requireNonNull(inLogo, "logo");
    this.css =
      Objects.requireNonNull(inCss, "css");
    this.title =
      Objects.requireNonNull(inTitle, "title");
  }

  /**
   * Create a branding service.
   *
   * @param strings       The server strings
   * @param configuration The branding configuration
   * @param templates     The template service
   *
   * @return A branding service
   *
   * @throws IOException On errors
   */

  public static IdServerBrandingService create(
    final IdServerStrings strings,
    final IdFMTemplateService templates,
    final IdServerBrandingConfiguration configuration)
    throws IOException
  {
    Objects.requireNonNull(templates, "templates");
    Objects.requireNonNull(configuration, "configuration");

    final var logo =
      loadLogo(configuration.logo());
    final var css =
      loadCSS(templates.cssTemplate(), configuration.scheme());
    final var title =
      configuration.productTitle()
        .orElse(strings.format("productTitle"));

    return new IdServerBrandingService(
      logo,
      css,
      title
    );
  }

  private static String loadCSS(
    final IdFMTemplateType<IdFMCSSData> template,
    final Optional<IdServerColorScheme> scheme)
    throws IOException
  {
    final IdFMCSSData templateParameters =
      scheme.map(IdFMCSSData::from)
        .orElseGet(IdFMCSSData::defaults);

    try (var writer = new StringWriter(8192)) {
      template.process(templateParameters, writer);
      return writer.toString();
    } catch (final TemplateException e) {
      throw new IOException(e);
    }
  }

  private static byte[] loadLogo(
    final Optional<Path> logoOpt)
    throws IOException
  {
    if (logoOpt.isPresent()) {
      return Files.readAllBytes(logoOpt.get());
    }
    final var c = IdServerBrandingService.class;
    try (var stream =
           c.getResourceAsStream(
             "/com/io7m/idstore/server/internal/idstore.svg")) {
      return stream.readAllBytes();
    }
  }

  /**
   * @return The bytes of an SVG logo image
   */

  public byte[] logoImage()
  {
    return this.logo;
  }

  /**
   * @return The product title
   */

  public String title()
  {
    return this.title;
  }

  /**
   * @return The CSS text
   */

  public String css()
  {
    return this.css;
  }

  @Override
  public String description()
  {
    return "Branding information service.";
  }

  /**
   * @param name The name/phrase
   *
   * @return An HTML title for the given name/phrase
   */

  public String htmlTitle(
    final String name)
  {
    return "%s: %s".formatted(this.title, name);
  }
}

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

import com.io7m.cxbutton.core.CxButtonCSS;
import com.io7m.idstore.model.IdOptional;
import com.io7m.idstore.server.api.IdServerBrandingConfiguration;
import com.io7m.idstore.server.api.IdServerColorScheme;
import com.io7m.idstore.server.internal.freemarker.IdFMCSSData;
import com.io7m.idstore.server.internal.freemarker.IdFMTemplateService;
import com.io7m.idstore.server.internal.freemarker.IdFMTemplateType;
import com.io7m.idstore.services.api.IdServiceType;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
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
  private final String mainCss;
  private final String xButtonCSS;
  private final String title;
  private final Optional<String> loginExtraText;

  private IdServerBrandingService(
    final byte[] inLogo,
    final String inMainCss,
    final String inXButtonCss,
    final String inTitle,
    final Optional<String> inLoginExtraText)
  {
    this.logo =
      Objects.requireNonNull(inLogo, "logo");
    this.mainCss =
      Objects.requireNonNull(inMainCss, "css");
    this.xButtonCSS =
      Objects.requireNonNull(inXButtonCss, "xButtonCss");
    this.title =
      Objects.requireNonNull(inTitle, "title");
    this.loginExtraText =
      Objects.requireNonNull(inLoginExtraText, "inLoginExtraText");
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
    final var xbuttonCss =
      loadXButtonCSS(configuration.scheme());
    final var mainCss =
      loadMainCSS(templates.cssTemplate(), configuration.scheme());
    final var title =
      configuration.productTitle()
        .orElse(strings.format("productTitle"));
    final var brandingText =
      IdOptional.mapPartial(
        configuration.loginExtra(),
        IdServerBrandingService::loadLoginExtraText);


    return new IdServerBrandingService(
      logo,
      mainCss,
      xbuttonCss,
      title,
      brandingText
    );
  }

  private static String loadLoginExtraText(
    final Path file)
    throws IOException
  {
    return Files.readString(file, StandardCharsets.UTF_8);
  }

  private static String loadXButtonCSS(
    final Optional<IdServerColorScheme> scheme)
    throws IOException
  {
    final IdServerColorScheme schemeParams =
      scheme.orElseGet(IdServerColorScheme::defaults);

    return CxButtonCSS.create()
      .cssOf(Optional.empty(), true, schemeParams.buttonColors());
  }

  private static String loadMainCSS(
    final IdFMTemplateType<IdFMCSSData> template,
    final Optional<IdServerColorScheme> scheme)
    throws IOException
  {
    final IdFMCSSData templateParameters =
      scheme.map(IdFMCSSData::new)
        .orElseGet(() -> {
          return new IdFMCSSData(IdServerColorScheme.defaults());
        });

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
   * @return The xButton CSS
   */

  public String xButtonCSS()
  {
    return this.xButtonCSS;
  }

  /**
   * @return The CSS text
   */

  public String css()
  {
    return this.mainCss;
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

  /**
   * @return The extra text inserted below the login form (XHTML)
   */

  public Optional<String> loginExtraText()
  {
    return this.loginExtraText;
  }

  @Override
  public String toString()
  {
    return "[IdServerBrandingService 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode()));
  }

  /**
   * The branded email subject.
   *
   * @param subject The subject txt
   *
   * @return The branded string
   */

  public String emailSubject(
    final String subject)
  {
    return "[idstore] %s".formatted(subject);
  }
}

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


package com.io7m.idstore.xbutton;

import com.io7m.idstore.xbutton.internal.IdXButtonTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Objects;

import static freemarker.template.Configuration.SQUARE_BRACKET_TAG_SYNTAX;

/**
 * Functions to retrieve CSS for buttons.
 */

public final class IdXButtonCSS
{
  private final Configuration configuration;

  private IdXButtonCSS(
    final Configuration inConfiguration)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
  }

  /**
   * @return A new CSS generator
   */

  public static IdXButtonCSS create()
  {
    final Configuration configuration =
      new Configuration(Configuration.VERSION_2_3_31);

    configuration.setTagSyntax(SQUARE_BRACKET_TAG_SYNTAX);
    configuration.setTemplateLoader(new IdXButtonTemplateLoader());
    return new IdXButtonCSS(configuration);
  }

  /**
   * Generate CSS.
   *
   * @param colors The colors
   *
   * @return A CSS text
   *
   * @throws IOException On errors
   */

  public String cssOf(
    final IdXButtonColors colors)
    throws IOException
  {
    Objects.requireNonNull(colors, "colors");

    try {
      final Template t =
        this.configuration.getTemplate("xbuttonCss");
      final var writer =
        new StringWriter();

      final var data = new HashMap<String, Object>();
      data.put("colors", colors);
      t.process(data, writer);

      return writer.toString();
    } catch (final TemplateException e) {
      throw new IOException(e);
    }
  }
}

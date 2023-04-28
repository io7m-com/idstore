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


package com.io7m.idstore.admin_gui.internal;

import com.io7m.idstore.admin_gui.IdAGConfiguration;
import com.io7m.junreachable.UnreachableCodeException;
import javafx.scene.Parent;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * CSS functions.
 */

public final class IdAGCSS
{
  private IdAGCSS()
  {

  }

  /**
   * @return The default CSS stylesheet
   */

  public static URI defaultCSS()
  {
    try {
      return IdAGApplication.class.getResource(
          "/com/io7m/idstore/admin_gui/internal/main.css")
        .toURI();
    } catch (final URISyntaxException e) {
      throw new UnreachableCodeException(e);
    }
  }

  /**
   * Set the CSS stylesheet for the given node. Either the default CSS
   * stylesheet will be used, or the configuration-provided override.
   *
   * @param configuration The configuration
   * @param node          The node
   */

  public static void setCSS(
    final IdAGConfiguration configuration,
    final Parent node)
  {
    Objects.requireNonNull(configuration, "configuration");
    Objects.requireNonNull(node, "node");

    node.getStylesheets()
      .add(String.valueOf(
        configuration.customCSS().orElseGet(IdAGCSS::defaultCSS))
      );
  }
}

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

package com.io7m.idstore.server.internal.freemarker;

import com.io7m.idstore.server.api.IdServerColor;
import com.io7m.idstore.server.api.IdServerColorScheme;
import com.io7m.jtensors.core.unparameterized.vectors.Vector3D;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.lang.Double.max;
import static java.lang.Double.min;

/**
 * A color scheme.
 *
 * @param buttonBodyColor           The body color of buttons
 * @param buttonBorderColor         The border color of buttons
 * @param buttonEmbossEColor        The east emboss color of buttons
 * @param buttonEmbossNColor        The north emboss color of buttons
 * @param buttonEmbossSColor        The south emboss color of buttons
 * @param buttonEmbossWColor        The west emboss color of buttons
 * @param buttonHoverBodyColor      The body color of buttons (hover)
 * @param buttonHoverBorderColor    The border color of buttons (hover)
 * @param buttonHoverEmbossEColor   The east emboss color of buttons (hover)
 * @param buttonHoverEmbossNColor   The north emboss color of buttons (hover)
 * @param buttonHoverEmbossSColor   The south emboss color of buttons (hover)
 * @param buttonHoverEmbossWColor   The west emboss color of buttons (hover)
 * @param buttonPressedBodyColor    The body color of buttons (pressed)
 * @param buttonPressedBorderColor  The border color of buttons (pressed)
 * @param buttonPressedEmbossEColor The east emboss color of buttons (pressed)
 * @param buttonPressedEmbossNColor The north emboss color of buttons (pressed)
 * @param buttonPressedEmbossSColor The south emboss color of buttons (pressed)
 * @param buttonPressedEmbossWColor The west emboss color of buttons (pressed)
 * @param errorBorderColor          The border color for error messages
 * @param headerBackgroundColor     The background color of the page header
 * @param headerLinkColor           The color of links in the page header
 * @param headerTextColor           The color of text in the page header
 * @param mainBackgroundColor       The background color of the main page
 * @param mainLinkColor             The color of links on the main page
 * @param mainMessageBorderColor    The border color of page messages
 * @param mainTableBorderColor      The border color of tables
 * @param mainTextColor             The main text color
 */

public record IdFMCSSData(
  Vector3D buttonBodyColor,
  Vector3D buttonBorderColor,
  Vector3D buttonEmbossEColor,
  Vector3D buttonEmbossNColor,
  Vector3D buttonEmbossSColor,
  Vector3D buttonEmbossWColor,
  Vector3D buttonHoverBodyColor,
  Vector3D buttonHoverBorderColor,
  Vector3D buttonHoverEmbossEColor,
  Vector3D buttonHoverEmbossNColor,
  Vector3D buttonHoverEmbossSColor,
  Vector3D buttonHoverEmbossWColor,
  Vector3D buttonPressedBodyColor,
  Vector3D buttonPressedBorderColor,
  Vector3D buttonPressedEmbossEColor,
  Vector3D buttonPressedEmbossNColor,
  Vector3D buttonPressedEmbossSColor,
  Vector3D buttonPressedEmbossWColor,
  Vector3D errorBorderColor,
  Vector3D headerBackgroundColor,
  Vector3D headerLinkColor,
  Vector3D headerTextColor,
  Vector3D mainBackgroundColor,
  Vector3D mainLinkColor,
  Vector3D mainMessageBorderColor,
  Vector3D mainTableBorderColor,
  Vector3D mainTextColor
) implements IdFMDataModelType
{
  /**
   * An ocean blue colour.
   */

  public static final Vector3D OCEAN_BLUE =
    Vector3D.of(0.105, 0.313, 0.454);

  /**
   * A color scheme.
   *
   * @param buttonBodyColor           The body color of buttons
   * @param buttonBorderColor         The border color of buttons
   * @param buttonEmbossEColor        The east emboss color of buttons
   * @param buttonEmbossNColor        The north emboss color of buttons
   * @param buttonEmbossSColor        The south emboss color of buttons
   * @param buttonEmbossWColor        The west emboss color of buttons
   * @param buttonHoverBodyColor      The body color of buttons (hover)
   * @param buttonHoverBorderColor    The border color of buttons (hover)
   * @param buttonHoverEmbossEColor   The east emboss color of buttons (hover)
   * @param buttonHoverEmbossNColor   The north emboss color of buttons (hover)
   * @param buttonHoverEmbossSColor   The south emboss color of buttons (hover)
   * @param buttonHoverEmbossWColor   The west emboss color of buttons (hover)
   * @param buttonPressedBodyColor    The body color of buttons (pressed)
   * @param buttonPressedBorderColor  The border color of buttons (pressed)
   * @param buttonPressedEmbossEColor The east emboss color of buttons
   *                                  (pressed)
   * @param buttonPressedEmbossNColor The north emboss color of buttons
   *                                  (pressed)
   * @param buttonPressedEmbossSColor The south emboss color of buttons
   *                                  (pressed)
   * @param buttonPressedEmbossWColor The west emboss color of buttons
   *                                  (pressed)
   * @param errorBorderColor          The border color for error messages
   * @param headerBackgroundColor     The background color of the page header
   * @param headerLinkColor           The color of links in the page header
   * @param headerTextColor           The color of text in the page header
   * @param mainBackgroundColor       The background color of the main page
   * @param mainLinkColor             The color of links on the main page
   * @param mainMessageBorderColor    The border color of page messages
   * @param mainTableBorderColor      The border color of tables
   * @param mainTextColor             The main text color
   */

  public IdFMCSSData
  {
    Objects.requireNonNull(buttonBodyColor, "buttonBodyColor");
    Objects.requireNonNull(buttonBorderColor, "buttonBorderColor");
    Objects.requireNonNull(buttonEmbossEColor, "buttonEmbossEColor");
    Objects.requireNonNull(buttonEmbossNColor, "buttonEmbossNColor");
    Objects.requireNonNull(buttonEmbossSColor, "buttonEmbossSColor");
    Objects.requireNonNull(buttonEmbossWColor, "buttonEmbossWColor");
    Objects.requireNonNull(buttonHoverBodyColor, "buttonHoverBodyColor");
    Objects.requireNonNull(buttonHoverBorderColor, "buttonHoverBorderColor");
    Objects.requireNonNull(buttonHoverEmbossEColor, "buttonHoverEmbossEColor");
    Objects.requireNonNull(buttonHoverEmbossNColor, "buttonHoverEmbossNColor");
    Objects.requireNonNull(buttonHoverEmbossSColor, "buttonHoverEmbossSColor");
    Objects.requireNonNull(buttonHoverEmbossWColor, "buttonHoverEmbossWColor");
    Objects.requireNonNull(buttonPressedBodyColor, "buttonPressedBodyColor");
    Objects.requireNonNull(
      buttonPressedBorderColor,
      "buttonPressedBorderColor");
    Objects.requireNonNull(
      buttonPressedEmbossEColor,
      "buttonPressedEmbossEColor");
    Objects.requireNonNull(
      buttonPressedEmbossNColor,
      "buttonPressedEmbossNColor");
    Objects.requireNonNull(
      buttonPressedEmbossSColor,
      "buttonPressedEmbossSColor");
    Objects.requireNonNull(
      buttonPressedEmbossWColor,
      "buttonPressedEmbossWColor");
    Objects.requireNonNull(errorBorderColor, "errorBorderColor");
    Objects.requireNonNull(headerBackgroundColor, "headerBackgroundColor");
    Objects.requireNonNull(headerLinkColor, "headerLinkColor");
    Objects.requireNonNull(headerTextColor, "headerTextColor");
    Objects.requireNonNull(mainBackgroundColor, "mainBackgroundColor");
    Objects.requireNonNull(mainLinkColor, "mainLinkColor");
    Objects.requireNonNull(mainMessageBorderColor, "mainMessageBorderColor");
    Objects.requireNonNull(mainTableBorderColor, "mainTableBorderColor");
    Objects.requireNonNull(mainTextColor, "mainTextColor");
  }

  /**
   * @return The default theme.
   */

  public static IdFMCSSData defaults()
  {
    final var buttonBorderColor =
      Vector3D.of(0.0, 0.0, 0.0);
    final var buttonBodyColor =
      Vector3D.of(0.86, 0.86, 0.86);
    final var buttonEmbossNColor =
      Vector3D.of(1.0, 1.0, 1.0);
    final var buttonEmbossEColor =
      Vector3D.of(1.0, 1.0, 1.0);
    final var buttonEmbossSColor =
      Vector3D.of(0.66, 0.66, 0.66);
    final var buttonEmbossWColor =
      Vector3D.of(0.66, 0.66, 0.66);

    final var buttonHoverBorderColor =
      Vector3D.of(0.0, 0.0, 0.0);
    final var buttonHoverBodyColor =
      Vector3D.of(0.93, 0.93, 0.93);
    final var buttonHoverEmbossNColor =
      Vector3D.of(1.0, 1.0, 1.0);
    final var buttonHoverEmbossEColor =
      Vector3D.of(1.0, 1.0, 1.0);
    final var buttonHoverEmbossSColor =
      Vector3D.of(0.66, 0.66, 0.66);
    final var buttonHoverEmbossWColor =
      Vector3D.of(0.66, 0.66, 0.66);

    final var buttonPressedBorderColor =
      Vector3D.of(0.0, 0.0, 0.0);
    final var buttonPressedBodyColor =
      Vector3D.of(0.86, 0.86, 0.86);
    final var buttonPressedEmbossNColor =
      Vector3D.of(0.66, 0.66, 0.66);
    final var buttonPressedEmbossEColor =
      Vector3D.of(0.66, 0.66, 0.66);
    final var buttonPressedEmbossSColor =
      Vector3D.of(0.8, 0.8, 0.8);
    final var buttonPressedEmbossWColor =
      Vector3D.of(0.8, 0.8, 0.8);

    final var errorBorderColor =
      Vector3D.of(1.0, 0.0, 0.0);

    final var headerBackgroundColor =
      Vector3D.of(0.1875, 0.1875, 0.1875);
    final var headerLinkColor =
      Vector3D.of(1.0, 0.597, 0.2);
    final var headerTextColor =
      Vector3D.of(1.0, 1.0, 1.0);

    final var mainBackgroundColor =
      Vector3D.of(1.0, 1.0, 1.0);
    final var mainLinkColor =
      Vector3D.of(0.0, 0.0, 1.0);
    final var mainMessageBorderColor =
      Vector3D.of(0.66, 0.66, 0.66);
    final var mainTableBorderColor =
      Vector3D.of(0.66, 0.66, 0.66);
    final var mainTextColor =
      Vector3D.of(0.0, 0.0, 0.0);

    return new IdFMCSSData(
      buttonBodyColor,
      buttonBorderColor,
      buttonEmbossEColor,
      buttonEmbossNColor,
      buttonEmbossSColor,
      buttonEmbossWColor,
      buttonHoverBodyColor,
      buttonHoverBorderColor,
      buttonHoverEmbossEColor,
      buttonHoverEmbossNColor,
      buttonHoverEmbossSColor,
      buttonHoverEmbossWColor,
      buttonPressedBodyColor,
      buttonPressedBorderColor,
      buttonPressedEmbossEColor,
      buttonPressedEmbossNColor,
      buttonPressedEmbossSColor,
      buttonPressedEmbossWColor,
      errorBorderColor,
      headerBackgroundColor,
      headerLinkColor,
      headerTextColor,
      mainBackgroundColor,
      mainLinkColor,
      mainMessageBorderColor,
      mainTableBorderColor,
      mainTextColor
    );
  }

  /**
   * @return The ocean theme.
   */

  public static IdFMCSSData ocean()
  {
    final var main =
      OCEAN_BLUE;

    final var buttonBorderColor =
      Vector3D.of(0.0, 0.0, 0.0);
    final var buttonBodyColor =
      main;
    final var buttonEmbossNColor =
      lighter(main, 0.7);
    final var buttonEmbossEColor =
      lighter(main, 0.7);
    final var buttonEmbossSColor =
      darker(main, 0.3);
    final var buttonEmbossWColor =
      darker(main, 0.3);

    final var buttonHoverBorderColor =
      Vector3D.of(0.0, 0.0, 0.0);
    final var buttonHoverBodyColor =
      lighter(buttonBodyColor, 0.2);
    final var buttonHoverEmbossNColor =
      buttonEmbossNColor;
    final var buttonHoverEmbossEColor =
      buttonEmbossEColor;
    final var buttonHoverEmbossSColor =
      buttonEmbossSColor;
    final var buttonHoverEmbossWColor =
      buttonEmbossWColor;

    final var buttonPressedBorderColor =
      Vector3D.of(0.0, 0.0, 0.0);
    final var buttonPressedBodyColor =
      darker(main, 0.1);
    final var buttonPressedEmbossNColor =
      darker(main, 0.3);
    final var buttonPressedEmbossEColor =
      darker(main, 0.3);
    final var buttonPressedEmbossSColor =
      lighter(main, 0.2);
    final var buttonPressedEmbossWColor =
      lighter(main, 0.2);

    final var errorBorderColor =
      Vector3D.of(1.0, 0.0, 0.0);

    final var headerBackgroundColor =
      Vector3D.of(0.1875, 0.1875, 0.1875);
    final var headerLinkColor =
      Vector3D.of(1.0, 1.0, 1.0);
    final var headerTextColor =
      Vector3D.of(1.0, 1.0, 1.0);

    final var mainBackgroundColor =
      main;
    final var mainLinkColor =
      Vector3D.of(1.0, 1.0, 1.0);
    final var mainMessageBorderColor =
      lighter(main, 0.8);
    final var mainTableBorderColor =
      lighter(main, 0.8);
    final var mainTextColor =
      Vector3D.of(1.0, 1.0, 1.0);

    return new IdFMCSSData(
      buttonBodyColor,
      buttonBorderColor,
      buttonEmbossEColor,
      buttonEmbossNColor,
      buttonEmbossSColor,
      buttonEmbossWColor,
      buttonHoverBodyColor,
      buttonHoverBorderColor,
      buttonHoverEmbossEColor,
      buttonHoverEmbossNColor,
      buttonHoverEmbossSColor,
      buttonHoverEmbossWColor,
      buttonPressedBodyColor,
      buttonPressedBorderColor,
      buttonPressedEmbossEColor,
      buttonPressedEmbossNColor,
      buttonPressedEmbossSColor,
      buttonPressedEmbossWColor,
      errorBorderColor,
      headerBackgroundColor,
      headerLinkColor,
      headerTextColor,
      mainBackgroundColor,
      mainLinkColor,
      mainMessageBorderColor,
      mainTableBorderColor,
      mainTextColor
    );
  }

  private static String rgb(
    final Vector3D color)
  {
    final var r = color(color.x());
    final var g = color(color.y());
    final var b = color(color.z());
    return String.format(
      "#%02x%02x%02x",
      Integer.valueOf(r),
      Integer.valueOf(g),
      Integer.valueOf(b)
    );
  }

  private static Vector3D lighter(
    final Vector3D v,
    final double factor)
  {
    return Vector3D.of(
      v.x() + (v.x() * factor),
      v.y() + (v.y() * factor),
      v.z() + (v.z() * factor)
    );
  }

  private static Vector3D darker(
    final Vector3D v,
    final double factor)
  {
    return Vector3D.of(
      v.x() - (v.x() * factor),
      v.y() - (v.y() * factor),
      v.z() - (v.z() * factor)
    );
  }

  private static int color(
    final double x)
  {
    return (int) max(min(x * 255.0, 255.0), 0.0);
  }

  /**
   * Produce data from a server color scheme.
   *
   * @param s The scheme
   *
   * @return This template date
   */

  public static IdFMCSSData from(
    final IdServerColorScheme s)
  {
    return new IdFMCSSData(
      convert(s.buttonBodyColor()),
      convert(s.buttonBorderColor()),
      convert(s.buttonEmbossEColor()),
      convert(s.buttonEmbossNColor()),
      convert(s.buttonEmbossSColor()),
      convert(s.buttonEmbossWColor()),
      convert(s.buttonHoverBodyColor()),
      convert(s.buttonHoverBorderColor()),
      convert(s.buttonHoverEmbossEColor()),
      convert(s.buttonHoverEmbossNColor()),
      convert(s.buttonHoverEmbossSColor()),
      convert(s.buttonHoverEmbossWColor()),
      convert(s.buttonPressedBodyColor()),
      convert(s.buttonPressedBorderColor()),
      convert(s.buttonPressedEmbossEColor()),
      convert(s.buttonPressedEmbossNColor()),
      convert(s.buttonPressedEmbossSColor()),
      convert(s.buttonPressedEmbossWColor()),
      convert(s.errorBorderColor()),
      convert(s.headerBackgroundColor()),
      convert(s.headerLinkColor()),
      convert(s.headerTextColor()),
      convert(s.mainBackgroundColor()),
      convert(s.mainLinkColor()),
      convert(s.mainMessageBorderColor()),
      convert(s.mainTableBorderColor()),
      convert(s.mainTextColor())
    );
  }

  private static Vector3D convert(
    final IdServerColor c)
  {
    return Vector3D.of(c.red(), c.green(), c.blue());
  }

  @Override
  public Map<String, Object> toTemplateHash()
  {
    final var m = new HashMap<String, Object>(32);
    m.put("buttonBodyColor", rgb(this.buttonBodyColor));
    m.put("buttonBorderColor", rgb(this.buttonBorderColor));
    m.put("buttonEmbossEColor", rgb(this.buttonEmbossEColor));
    m.put("buttonEmbossNColor", rgb(this.buttonEmbossNColor));
    m.put("buttonEmbossSColor", rgb(this.buttonEmbossSColor));
    m.put("buttonEmbossWColor", rgb(this.buttonEmbossWColor));
    m.put("buttonHoverBodyColor", rgb(this.buttonHoverBodyColor));
    m.put("buttonHoverBorderColor", rgb(this.buttonHoverBorderColor));
    m.put("buttonHoverEmbossEColor", rgb(this.buttonHoverEmbossEColor));
    m.put("buttonHoverEmbossNColor", rgb(this.buttonHoverEmbossNColor));
    m.put("buttonHoverEmbossSColor", rgb(this.buttonHoverEmbossSColor));
    m.put("buttonHoverEmbossWColor", rgb(this.buttonHoverEmbossWColor));
    m.put("buttonPressedBodyColor", rgb(this.buttonPressedBodyColor));
    m.put("buttonPressedBorderColor", rgb(this.buttonPressedBorderColor));
    m.put("buttonPressedEmbossEColor", rgb(this.buttonPressedEmbossEColor));
    m.put("buttonPressedEmbossNColor", rgb(this.buttonPressedEmbossNColor));
    m.put("buttonPressedEmbossSColor", rgb(this.buttonPressedEmbossSColor));
    m.put("buttonPressedEmbossWColor", rgb(this.buttonPressedEmbossWColor));
    m.put("errorBorderColor", rgb(this.errorBorderColor));
    m.put("headerBackgroundColor", rgb(this.headerBackgroundColor));
    m.put("headerLinkColor", rgb(this.headerLinkColor));
    m.put("headerTextColor", rgb(this.headerTextColor));
    m.put("mainBackgroundColor", rgb(this.mainBackgroundColor));
    m.put("mainLinkColor", rgb(this.mainLinkColor));
    m.put("mainMessageBorderColor", rgb(this.mainMessageBorderColor));
    m.put("mainTableBorderColor", rgb(this.mainTableBorderColor));
    m.put("mainTextColor", rgb(this.mainTextColor));
    return m;
  }
}

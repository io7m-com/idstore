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

package com.io7m.idstore.server.api;

import com.io7m.cxbutton.core.CxButtonCSS;
import com.io7m.cxbutton.core.CxButtonColors;

import java.util.Objects;

/**
 * A server color scheme.
 *
 * @param buttonColors           The color of buttons
 * @param errorBorderColor       The border color for error messages
 * @param headerBackgroundColor  The background color of the page header
 * @param headerLinkColor        The color of links in the page header
 * @param headerTextColor        The color of text in the page header
 * @param mainBackgroundColor    The background color of the main page
 * @param mainLinkColor          The color of links on the main page
 * @param mainMessageBorderColor The border color of page messages
 * @param mainTableBorderColor   The border color of tables
 * @param mainTextColor          The main text color
 */

public record IdServerColorScheme(
  CxButtonColors buttonColors,
  IdColor errorBorderColor,
  IdColor headerBackgroundColor,
  IdColor headerLinkColor,
  IdColor headerTextColor,
  IdColor mainBackgroundColor,
  IdColor mainLinkColor,
  IdColor mainMessageBorderColor,
  IdColor mainTableBorderColor,
  IdColor mainTextColor
)
{
  /**
   * A server color scheme.
   *
   * @param buttonColors           The color of buttons
   * @param errorBorderColor       The border color for error messages
   * @param headerBackgroundColor  The background color of the page header
   * @param headerLinkColor        The color of links in the page header
   * @param headerTextColor        The color of text in the page header
   * @param mainBackgroundColor    The background color of the main page
   * @param mainLinkColor          The color of links on the main page
   * @param mainMessageBorderColor The border color of page messages
   * @param mainTableBorderColor   The border color of tables
   * @param mainTextColor          The main text color
   */

  public IdServerColorScheme
  {
    Objects.requireNonNull(
      errorBorderColor, "errorBorderColor");
    Objects.requireNonNull(
      headerBackgroundColor, "headerBackgroundColor");
    Objects.requireNonNull(
      headerLinkColor, "headerLinkColor");
    Objects.requireNonNull(
      headerTextColor, "headerTextColor");
    Objects.requireNonNull(
      mainBackgroundColor, "mainBackgroundColor");
    Objects.requireNonNull(
      mainLinkColor, "mainLinkColor");
    Objects.requireNonNull(
      mainMessageBorderColor, "mainMessageBorderColor");
    Objects.requireNonNull(
      mainTableBorderColor, "mainTableBorderColor");
    Objects.requireNonNull(
      mainTextColor, "mainTextColor");
  }

  /**
   * An ocean blue colour.
   */

  public static final IdColor OCEAN_BLUE =
    new IdColor(0.105, 0.313, 0.454);

  /**
   * @return The default color scheme
   */

  public static IdServerColorScheme defaults()
  {
    return new IdServerColorScheme(
      CxButtonCSS.defaultColors(),
      new IdColor(1.0, 0.0, 0.0),
      new IdColor(0.2, 0.2, 0.2),
      new IdColor(1.0, 0.596, 0.2),
      new IdColor(1.0, 1.0, 1.0),
      new IdColor(1.0, 1.0, 1.0),
      new IdColor(0.0, 0.0, 1.0),
      new IdColor(0.8, 0.8, 0.8),
      new IdColor(0.5, 0.5, 0.5),
      new IdColor(0.0, 0.0, 0.0)
    );
  }
}

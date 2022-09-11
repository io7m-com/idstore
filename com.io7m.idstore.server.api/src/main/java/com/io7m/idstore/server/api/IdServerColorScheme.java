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

package com.io7m.idstore.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.io7m.idstore.colors.IdColor;
import com.io7m.idstore.xbutton.IdXButtonColors;
import com.io7m.idstore.xbutton.IdXButtonStateColors;

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

@JsonSerialize
@JsonDeserialize
public record IdServerColorScheme(
  @JsonProperty(value = "ButtonColors", required = true)
  IdXButtonColors buttonColors,
  @JsonProperty(value = "ErrorBorderColor", required = true)
  IdColor errorBorderColor,
  @JsonProperty(value = "HeaderBackgroundColor", required = true)
  IdColor headerBackgroundColor,
  @JsonProperty(value = "HeaderLinkColor", required = true)
  IdColor headerLinkColor,
  @JsonProperty(value = "HeaderTextColor", required = true)
  IdColor headerTextColor,
  @JsonProperty(value = "MainBackgroundColor", required = true)
  IdColor mainBackgroundColor,
  @JsonProperty(value = "MainLinkColor", required = true)
  IdColor mainLinkColor,
  @JsonProperty(value = "MainMessageBorderColor", required = true)
  IdColor mainMessageBorderColor,
  @JsonProperty(value = "MainTableBorderColor", required = true)
  IdColor mainTableBorderColor,
  @JsonProperty(value = "MainTextColor", required = true)
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
    final var buttonEnabledTextColor =
      new IdColor(0.0, 0.0, 0.0);
    final var buttonEnabledBorderColor =
      new IdColor(0.0, 0.0, 0.0);
    final var buttonEnabledBodyColor =
      new IdColor(0.86, 0.86, 0.86);
    final var buttonEnabledEmbossNColor =
      new IdColor(1.0, 1.0, 1.0);
    final var buttonEnabledEmbossEColor =
      new IdColor(1.0, 1.0, 1.0);
    final var buttonEnabledEmbossSColor =
      new IdColor(0.66, 0.66, 0.66);
    final var buttonEnabledEmbossWColor =
      new IdColor(0.66, 0.66, 0.66);

    final var buttonHoverTextColor =
      new IdColor(0.0, 0.0, 0.0);
    final var buttonHoverBorderColor =
      new IdColor(0.0, 0.0, 0.0);
    final var buttonHoverBodyColor =
      new IdColor(0.93, 0.93, 0.93);
    final var buttonHoverEmbossNColor =
      new IdColor(1.0, 1.0, 1.0);
    final var buttonHoverEmbossEColor =
      new IdColor(1.0, 1.0, 1.0);
    final var buttonHoverEmbossSColor =
      new IdColor(0.66, 0.66, 0.66);
    final var buttonHoverEmbossWColor =
      new IdColor(0.66, 0.66, 0.66);

    final var buttonPressedTextColor =
      new IdColor(0.0, 0.0, 0.0);
    final var buttonPressedBorderColor =
      new IdColor(0.0, 0.0, 0.0);
    final var buttonPressedBodyColor =
      new IdColor(0.86, 0.86, 0.86);
    final var buttonPressedEmbossNColor =
      new IdColor(0.66, 0.66, 0.66);
    final var buttonPressedEmbossEColor =
      new IdColor(0.66, 0.66, 0.66);
    final var buttonPressedEmbossSColor =
      new IdColor(0.8, 0.8, 0.8);
    final var buttonPressedEmbossWColor =
      new IdColor(0.8, 0.8, 0.8);

    final var buttonDisabledBorderColor =
      new IdColor(0.0, 0.0, 0.0);
    final var buttonDisabledBodyColor =
      new IdColor(0.86, 0.86, 0.86);
    final var buttonDisabledTextColor =
      buttonDisabledBodyColor.darker(0.2);
    final var buttonDisabledEmbossNColor =
      buttonDisabledBodyColor;
    final var buttonDisabledEmbossEColor =
      buttonDisabledBodyColor;
    final var buttonDisabledEmbossSColor =
      buttonDisabledBodyColor;
    final var buttonDisabledEmbossWColor =
      buttonDisabledBodyColor;

    final var buttonColors = new IdXButtonColors(
      new IdXButtonStateColors(
        buttonEnabledTextColor,
        buttonEnabledBodyColor,
        buttonEnabledBorderColor,
        buttonEnabledEmbossEColor,
        buttonEnabledEmbossNColor,
        buttonEnabledEmbossSColor,
        buttonEnabledEmbossWColor
      ),
      new IdXButtonStateColors(
        buttonDisabledTextColor,
        buttonDisabledBodyColor,
        buttonDisabledBorderColor,
        buttonDisabledEmbossEColor,
        buttonDisabledEmbossNColor,
        buttonDisabledEmbossSColor,
        buttonDisabledEmbossWColor
      ),
      new IdXButtonStateColors(
        buttonPressedTextColor,
        buttonPressedBodyColor,
        buttonPressedBorderColor,
        buttonPressedEmbossEColor,
        buttonPressedEmbossNColor,
        buttonPressedEmbossSColor,
        buttonPressedEmbossWColor
      ),
      new IdXButtonStateColors(
        buttonHoverTextColor,
        buttonHoverBodyColor,
        buttonHoverBorderColor,
        buttonHoverEmbossEColor,
        buttonHoverEmbossNColor,
        buttonHoverEmbossSColor,
        buttonHoverEmbossWColor
      )
    );

    return new IdServerColorScheme(
      buttonColors,
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

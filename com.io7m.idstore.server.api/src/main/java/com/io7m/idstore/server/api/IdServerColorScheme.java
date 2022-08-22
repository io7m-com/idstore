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

import java.util.Objects;

/**
 * A server color scheme.
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

@JsonSerialize
@JsonDeserialize
public record IdServerColorScheme(
  @JsonProperty(value = "ButtonBodyColor", required = true)
  IdServerColor buttonBodyColor,
  @JsonProperty(value = "ButtonBorderColor", required = true)
  IdServerColor buttonBorderColor,
  @JsonProperty(value = "ButtonEmbossEColor", required = true)
  IdServerColor buttonEmbossEColor,
  @JsonProperty(value = "ButtonEmbossNColor", required = true)
  IdServerColor buttonEmbossNColor,
  @JsonProperty(value = "ButtonEmbossSColor", required = true)
  IdServerColor buttonEmbossSColor,
  @JsonProperty(value = "ButtonEmbossWColor", required = true)
  IdServerColor buttonEmbossWColor,
  @JsonProperty(value = "ButtonHoverBodyColor", required = true)
  IdServerColor buttonHoverBodyColor,
  @JsonProperty(value = "ButtonHoverBorderColor", required = true)
  IdServerColor buttonHoverBorderColor,
  @JsonProperty(value = "ButtonHoverEmbossEColor", required = true)
  IdServerColor buttonHoverEmbossEColor,
  @JsonProperty(value = "ButtonHoverEmbossNColor", required = true)
  IdServerColor buttonHoverEmbossNColor,
  @JsonProperty(value = "ButtonHoverEmbossSColor", required = true)
  IdServerColor buttonHoverEmbossSColor,
  @JsonProperty(value = "ButtonHoverEmbossWColor", required = true)
  IdServerColor buttonHoverEmbossWColor,
  @JsonProperty(value = "ButtonPressedBodyColor", required = true)
  IdServerColor buttonPressedBodyColor,
  @JsonProperty(value = "ButtonPressedBorderColor", required = true)
  IdServerColor buttonPressedBorderColor,
  @JsonProperty(value = "ButtonPressedEmbossEColor", required = true)
  IdServerColor buttonPressedEmbossEColor,
  @JsonProperty(value = "ButtonPressedEmbossNColor", required = true)
  IdServerColor buttonPressedEmbossNColor,
  @JsonProperty(value = "ButtonPressedEmbossSColor", required = true)
  IdServerColor buttonPressedEmbossSColor,
  @JsonProperty(value = "ButtonPressedEmbossWColor", required = true)
  IdServerColor buttonPressedEmbossWColor,
  @JsonProperty(value = "ErrorBorderColor", required = true)
  IdServerColor errorBorderColor,
  @JsonProperty(value = "HeaderBackgroundColor", required = true)
  IdServerColor headerBackgroundColor,
  @JsonProperty(value = "HeaderLinkColor", required = true)
  IdServerColor headerLinkColor,
  @JsonProperty(value = "HeaderTextColor", required = true)
  IdServerColor headerTextColor,
  @JsonProperty(value = "MainBackgroundColor", required = true)
  IdServerColor mainBackgroundColor,
  @JsonProperty(value = "MainLinkColor", required = true)
  IdServerColor mainLinkColor,
  @JsonProperty(value = "MainMessageBorderColor", required = true)
  IdServerColor mainMessageBorderColor,
  @JsonProperty(value = "MainTableBorderColor", required = true)
  IdServerColor mainTableBorderColor,
  @JsonProperty(value = "MainTextColor", required = true)
  IdServerColor mainTextColor
)
{
  /**
   * A server color scheme.
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

  public IdServerColorScheme
  {
    Objects.requireNonNull(
      buttonBodyColor, "buttonBodyColor");
    Objects.requireNonNull(
      buttonBorderColor, "buttonBorderColor");
    Objects.requireNonNull(
      buttonEmbossEColor, "buttonEmbossEColor");
    Objects.requireNonNull(
      buttonEmbossNColor, "buttonEmbossNColor");
    Objects.requireNonNull(
      buttonEmbossSColor, "buttonEmbossSColor");
    Objects.requireNonNull(
      buttonEmbossWColor, "buttonEmbossWColor");
    Objects.requireNonNull(
      buttonHoverBodyColor, "buttonHoverBodyColor");
    Objects.requireNonNull(
      buttonHoverBorderColor, "buttonHoverBorderColor");
    Objects.requireNonNull(
      buttonHoverEmbossEColor, "buttonHoverEmbossEColor");
    Objects.requireNonNull(
      buttonHoverEmbossNColor, "buttonHoverEmbossNColor");
    Objects.requireNonNull(
      buttonHoverEmbossSColor, "buttonHoverEmbossSColor");
    Objects.requireNonNull(
      buttonHoverEmbossWColor, "buttonHoverEmbossWColor");
    Objects.requireNonNull(
      buttonPressedBodyColor, "buttonPressedBodyColor");
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
}

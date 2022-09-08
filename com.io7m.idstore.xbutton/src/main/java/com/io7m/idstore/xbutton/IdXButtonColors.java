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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Objects;

/**
 * The colors for each button state.
 *
 * @param enabled  The colors for the enabled state
 * @param disabled The colors for the disabled state
 * @param pressed  The colors for the pressed state
 * @param hover    The colors for the hover state
 */

@JsonSerialize
@JsonDeserialize
public record IdXButtonColors(
  @JsonProperty(value = "Enabled", required = true)
  IdXButtonStateColors enabled,
  @JsonProperty(value = "Disabled", required = true)
  IdXButtonStateColors disabled,
  @JsonProperty(value = "Pressed", required = true)
  IdXButtonStateColors pressed,
  @JsonProperty(value = "Hover", required = true)
  IdXButtonStateColors hover)
{
  /**
   * The colors for each button state.
   *
   * @param enabled  The colors for the enabled state
   * @param disabled The colors for the disabled state
   * @param pressed  The colors for the pressed state
   * @param hover    The colors for the hover state
   */

  public IdXButtonColors
  {
    Objects.requireNonNull(enabled, "enabled");
    Objects.requireNonNull(disabled, "disabled");
    Objects.requireNonNull(pressed, "pressed");
    Objects.requireNonNull(hover, "hover");
  }
}

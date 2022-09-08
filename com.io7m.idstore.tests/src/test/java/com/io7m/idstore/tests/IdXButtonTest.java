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


package com.io7m.idstore.tests;

import com.io7m.idstore.colors.IdColor;
import com.io7m.idstore.xbutton.IdXButtonCSS;
import com.io7m.idstore.xbutton.IdXButtonColors;
import com.io7m.idstore.xbutton.IdXButtonStateColors;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public final class IdXButtonTest
{
  @Test
  public void testCSS()
    throws IOException
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

    final var colors = new IdXButtonColors(
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

    final var bcss = IdXButtonCSS.create();
    System.out.println(bcss.cssOf(colors));
  }
}

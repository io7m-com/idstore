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


package com.io7m.idstore.colors;

import static java.lang.Double.max;
import static java.lang.Double.min;

/**
 * A color.
 *
 * @param red   The red channel
 * @param green The green channel
 * @param blue  The blue channel
 */

public record IdColor(
  double red,
  double green,
  double blue)
{
  /**
   * A color.
   *
   * @param red   The red channel
   * @param green The green channel
   * @param blue  The blue channel
   */

  public IdColor(
    final double red,
    final double green,
    final double blue)
  {
    this.red = min(1.0, max(0.0, red));
    this.green = min(1.0, max(0.0, green));
    this.blue = min(1.0, max(0.0, blue));
  }

  @Override
  public String toString()
  {
    return String.format(
      "#%02x%02x%02x",
      Integer.valueOf((int) (this.red * 255.0)),
      Integer.valueOf((int) (this.green * 255.0)),
      Integer.valueOf((int) (this.blue * 255.0))
    );
  }

  /**
   * Scale this color by the given factor. Factors less than 1.0 make the color
   * darker. Factors greater than 1.0 make the color lighter.
   *
   * @param factor The factor
   *
   * @return The color scaled
   */

  public IdColor scale(
    final double factor)
  {
    return new IdColor(
      this.red * factor,
      this.green * factor,
      this.blue * factor
    );
  }

  /**
   * Lighten this color.
   *
   * @param factor The factor
   *
   * @return A lightened color
   */

  public IdColor lighter(
    final double factor)
  {
    return this.scale(1.0 + factor);
  }

  /**
   * Darken this color.
   *
   * @param factor The factor
   *
   * @return A darkened color
   */

  public IdColor darker(
    final double factor)
  {
    return this.scale(1.0 - factor);
  }
}

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

package com.io7m.idstore.model;

import java.util.Comparator;
import java.util.Objects;

/**
 * A user "real" name.
 *
 * @param value The name value
 */

public record IdRealName(String value)
  implements Comparable<IdRealName>
{
  /**
   * A user display name.
   *
   * @param value The name value
   */

  public IdRealName
  {
    Objects.requireNonNull(value, "value");

    if (value.isBlank()) {
      throw new IdValidityException(
        "Names must contain non-whitespace characters"
      );
    }
    final var nameLength = value.length();
    if (nameLength > 1024) {
      throw new IdValidityException(
        "Name length %d must be <= 1024".formatted(Integer.valueOf(nameLength))
      );
    }
  }

  @Override
  public String toString()
  {
    return this.value;
  }

  @Override
  public int compareTo(
    final IdRealName other)
  {
    return Comparator.comparing(IdRealName::value)
      .compare(this, other);
  }
}

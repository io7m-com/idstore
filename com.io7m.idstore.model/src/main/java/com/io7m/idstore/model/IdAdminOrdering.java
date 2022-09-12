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

package com.io7m.idstore.model;

import java.util.List;

/**
 * An ordering specification for admins.
 *
 * @param ordering The column orderings
 */

public record IdAdminOrdering(
  List<IdAdminColumnOrdering> ordering)
{
  private static final IdAdminOrdering NO_ORDERING =
    new IdAdminOrdering(List.of());

  /**
   * An ordering specification for admins.
   *
   * @param ordering The column orderings
   */

  public IdAdminOrdering(
    final List<IdAdminColumnOrdering> ordering)
  {
    final var distinctColumns =
      ordering.stream()
        .map(IdAdminColumnOrdering::column)
        .distinct()
        .count();

    if (distinctColumns != (long) ordering.size()) {
      throw new IdValidityException(
        "Columns in orderings can only be specified once!"
      );
    }

    this.ordering = List.copyOf(ordering);
  }

  /**
   * @return A value that specifies no particular ordering
   */

  public static IdAdminOrdering noOrdering()
  {
    return NO_ORDERING;
  }
}

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

import java.util.Objects;
import java.util.Optional;

/**
 * The immutable parameters required to search users.
 *
 * @param timeCreatedRange Only users created within this time range are
 *                         returned
 * @param timeUpdatedRange Only users updated within this time range are
 *                         returned
 * @param search           The search query
 * @param ordering         The ordering specification
 * @param limit            The limit on the number of returned users
 */

public record IdUserSearchParameters(
  IdTimeRange timeCreatedRange,
  IdTimeRange timeUpdatedRange,
  Optional<String> search,
  IdUserColumnOrdering ordering,
  int limit)
{
  /**
   * The immutable parameters required to list users.
   *
   * @param timeCreatedRange Only users created within this time range are
   *                         returned
   * @param timeUpdatedRange Only users updated within this time range are
   *                         returned
   * @param search           The search query
   * @param ordering         The ordering specification
   * @param limit            The limit on the number of returned users
   */

  public IdUserSearchParameters
  {
    Objects.requireNonNull(timeCreatedRange, "timeCreatedRange");
    Objects.requireNonNull(timeUpdatedRange, "timeUpdatedRange");
    Objects.requireNonNull(search, "search");
    Objects.requireNonNull(ordering, "ordering");
  }

  /**
   * @return The limit on the number of returned users
   */

  @Override
  public int limit()
  {
    return Math.max(1, this.limit);
  }
}

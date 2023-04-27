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

import java.util.Objects;

/**
 * The immutable parameters required to search admins.
 *
 * @param timeCreatedRange Only admins created within this time range are
 *                         returned
 * @param timeUpdatedRange Only admins updated within this time range are
 *                         returned
 * @param search           The search query
 * @param ordering         The ordering specification
 * @param limit            The limit on the number of returned admins
 */

public record IdAdminSearchByEmailParameters(
  IdTimeRange timeCreatedRange,
  IdTimeRange timeUpdatedRange,
  String search,
  IdAdminColumnOrdering ordering,
  int limit)
{
  /**
   * The immutable parameters required to list admins.
   *
   * @param timeCreatedRange Only admins created within this time range are
   *                         returned
   * @param timeUpdatedRange Only admins updated within this time range are
   *                         returned
   * @param search           The search query
   * @param ordering         The ordering specification
   * @param limit            The limit on the number of returned admins
   */

  public IdAdminSearchByEmailParameters(
    final IdTimeRange timeCreatedRange,
    final IdTimeRange timeUpdatedRange,
    final String search,
    final IdAdminColumnOrdering ordering,
    final int limit)
  {
    this.timeCreatedRange =
      Objects.requireNonNull(timeCreatedRange, "timeCreatedRange");
    this.timeUpdatedRange =
      Objects.requireNonNull(timeUpdatedRange, "timeUpdatedRange");
    this.search =
      Objects.requireNonNull(search, "search")
        .toLowerCase();
    this.ordering =
      Objects.requireNonNull(ordering, "ordering");
    this.limit =
      Math.max(1, limit);
  }
}

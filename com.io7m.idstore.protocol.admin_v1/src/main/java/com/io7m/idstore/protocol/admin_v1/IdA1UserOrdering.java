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

package com.io7m.idstore.protocol.admin_v1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.io7m.idstore.model.IdUserOrdering;
import com.io7m.idstore.model.IdValidityException;
import com.io7m.idstore.protocol.api.IdProtocolToModelType;

import java.util.List;
import java.util.Objects;

/**
 * An ordering specification for users.
 *
 * @param columns The column orderings
 */

@JsonDeserialize
@JsonSerialize
public record IdA1UserOrdering(
  @JsonProperty(value = "Columns", required = true)
  List<IdA1UserColumnOrdering> columns)
  implements IdProtocolToModelType<IdUserOrdering>
{
  private static final IdA1UserOrdering NO_ORDERING =
    new IdA1UserOrdering(List.of());

  /**
   * An ordering specification for users.
   *
   * @param columns The column orderings
   */

  @JsonCreator
  public IdA1UserOrdering(
    @JsonProperty(value = "Columns", required = true)
    final List<IdA1UserColumnOrdering> columns)
  {
    Objects.requireNonNull(columns, "columns");

    final var distinctColumns =
      columns.stream()
        .map(IdA1UserColumnOrdering::column)
        .distinct()
        .count();

    if (distinctColumns != (long) columns.size()) {
      throw new IdValidityException(
        "Columns in orderings can only be specified once!"
      );
    }

    this.columns = List.copyOf(columns);
  }

  /**
   * @return A value that specifies no particular ordering
   */

  public static IdA1UserOrdering noOrdering()
  {
    return NO_ORDERING;
  }

  @Override
  public IdUserOrdering toModel()
  {
    return new IdUserOrdering(
      this.columns.stream()
        .map(IdA1UserColumnOrdering::toModel)
        .toList()
    );
  }
}

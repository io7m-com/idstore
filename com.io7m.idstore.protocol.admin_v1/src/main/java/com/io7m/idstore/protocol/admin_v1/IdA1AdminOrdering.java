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
import com.io7m.idstore.model.IdAdminOrdering;
import com.io7m.idstore.model.IdValidityException;
import com.io7m.idstore.protocol.api.IdProtocolToModelType;

import java.util.List;
import java.util.Objects;

/**
 * An ordering specification for admins.
 *
 * @param columns The column orderings
 */

@JsonDeserialize
@JsonSerialize
public record IdA1AdminOrdering(
  @JsonProperty(value = "Columns", required = true)
  List<IdA1AdminColumnOrdering> columns)
  implements IdProtocolToModelType<IdAdminOrdering>
{
  private static final IdA1AdminOrdering NO_ORDERING =
    new IdA1AdminOrdering(List.of());

  /**
   * An ordering specification for admins.
   *
   * @param columns The column orderings
   */

  @JsonCreator
  public IdA1AdminOrdering(
    @JsonProperty(value = "Columns", required = true) final List<IdA1AdminColumnOrdering> columns)
  {
    Objects.requireNonNull(columns, "columns");

    final var distinctColumns =
      columns.stream()
        .map(IdA1AdminColumnOrdering::column)
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

  public static IdA1AdminOrdering noOrdering()
  {
    return NO_ORDERING;
  }

  /**
   * Convert the given value.
   *
   * @param ordering The value
   *
   * @return The converted value
   */

  public static IdA1AdminOrdering of(
    final IdAdminOrdering ordering)
  {
    return new IdA1AdminOrdering(
      ordering.ordering()
        .stream()
        .map(IdA1AdminColumnOrdering::of)
        .toList()
    );
  }

  @Override
  public IdAdminOrdering toModel()
  {
    return new IdAdminOrdering(
      this.columns.stream()
        .map(IdA1AdminColumnOrdering::toModel)
        .toList()
    );
  }
}

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.io7m.idstore.model.IdUserSearchByEmailParameters;
import com.io7m.idstore.protocol.api.IdProtocolToModelType;

import java.util.Objects;

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

@JsonDeserialize
@JsonSerialize
public record IdA1UserSearchByEmailParameters(
  @JsonProperty(value = "TimeCreatedRange", required = true)
  IdA1TimeRange timeCreatedRange,
  @JsonProperty(value = "TimeUpdatedRange", required = true)
  IdA1TimeRange timeUpdatedRange,
  @JsonProperty(value = "Search")
  String search,
  @JsonProperty(value = "Ordering", required = true)
  IdA1UserOrdering ordering,
  @JsonProperty(value = "Limit", required = true)
  int limit)
  implements IdProtocolToModelType<IdUserSearchByEmailParameters>
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

  public IdA1UserSearchByEmailParameters
  {
    Objects.requireNonNull(timeCreatedRange, "timeCreatedRange");
    Objects.requireNonNull(timeUpdatedRange, "timeUpdatedRange");
    Objects.requireNonNull(search, "search");
    Objects.requireNonNull(ordering, "ordering");
  }

  @Override
  public IdUserSearchByEmailParameters toModel()
  {
    return new IdUserSearchByEmailParameters(
      this.timeCreatedRange.toModel(),
      this.timeUpdatedRange.toModel(),
      this.search,
      this.ordering.toModel(),
      this.limit
    );
  }

  /**
   * Construct an A1 value from the given model value.
   *
   * @param parameters The parameters
   *
   * @return The A1 value
   */

  public static IdA1UserSearchByEmailParameters of(
    final IdUserSearchByEmailParameters parameters)
  {
    return new IdA1UserSearchByEmailParameters(
      IdA1TimeRange.of(parameters.timeCreatedRange()),
      IdA1TimeRange.of(parameters.timeUpdatedRange()),
      parameters.search(),
      IdA1UserOrdering.of(parameters.ordering()),
      parameters.limit()
    );
  }
}

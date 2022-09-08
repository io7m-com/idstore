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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.io7m.idstore.model.IdUserListParameters;
import com.io7m.idstore.protocol.api.IdProtocolToModelType;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.idstore.protocol.admin_v1.IdA1UserColumn.BY_ID;
import static java.time.ZoneOffset.UTC;

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
public record IdA1UserListParameters(
  @JsonProperty(value = "TimeCreatedRange", required = true)
  IdA1TimeRange timeCreatedRange,
  @JsonProperty(value = "TimeUpdatedRange", required = true)
  IdA1TimeRange timeUpdatedRange,
  @JsonInclude(JsonInclude.Include.NON_ABSENT)
  @JsonProperty(value = "Search")
  Optional<String> search,
  @JsonProperty(value = "Ordering", required = true)
  IdA1UserOrdering ordering,
  @JsonProperty(value = "Limit", required = true)
  int limit)
  implements IdProtocolToModelType<IdUserListParameters>
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

  public IdA1UserListParameters
  {
    Objects.requireNonNull(timeCreatedRange, "timeCreatedRange");
    Objects.requireNonNull(timeUpdatedRange, "timeUpdatedRange");
    Objects.requireNonNull(search, "search");
    Objects.requireNonNull(ordering, "ordering");
  }

  private static final OffsetDateTime DEFAULT_TIME_LOW =
    Instant.ofEpochSecond(0L).atOffset(UTC);

  /**
   * @return Reasonable default parameters
   */

  public static IdA1UserListParameters defaults()
  {
    final var now = OffsetDateTime.now();
    return new IdA1UserListParameters(
      new IdA1TimeRange(DEFAULT_TIME_LOW, now.plusDays(1L)),
      new IdA1TimeRange(DEFAULT_TIME_LOW, now.plusDays(1L)),
      Optional.empty(),
      new IdA1UserOrdering(List.of(new IdA1UserColumnOrdering(BY_ID, false))),
      20
    );
  }

  @Override
  public IdUserListParameters toModel()
  {
    return new IdUserListParameters(
      this.timeCreatedRange.toModel(),
      this.timeUpdatedRange.toModel(),
      this.search,
      this.ordering.toModel(),
      this.limit
    );
  }
}

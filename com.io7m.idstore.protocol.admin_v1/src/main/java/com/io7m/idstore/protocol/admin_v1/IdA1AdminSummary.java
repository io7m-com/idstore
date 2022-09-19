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
import com.io7m.idstore.model.IdAdminSummary;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.protocol.api.IdProtocolToModelType;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * A summary of an admin.
 *
 * @param id          The admin ID
 * @param idName      The admin's ID name
 * @param realName    The admin's real name
 * @param timeCreated The time the admin was created
 * @param timeUpdated The time the admin was updated
 */

public record IdA1AdminSummary(
  @JsonProperty(value = "ID", required = true)
  UUID id,
  @JsonProperty(value = "IDName", required = true)
  String idName,
  @JsonProperty(value = "RealName", required = true)
  String realName,
  @JsonProperty(value = "TimeCreated", required = true)
  OffsetDateTime timeCreated,
  @JsonProperty(value = "TimeUpdated", required = true)
  OffsetDateTime timeUpdated)
  implements IdProtocolToModelType<IdAdminSummary>
{
  /**
   * A summary of an admin.
   *
   * @param id          The admin ID
   * @param idName      The admin's ID name
   * @param realName    The admin name
   * @param timeCreated The time the admin was created
   * @param timeUpdated The time the admin was updated
   */

  public IdA1AdminSummary
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(idName, "idName");
    Objects.requireNonNull(realName, "realName");
    Objects.requireNonNull(timeCreated, "created");
    Objects.requireNonNull(timeUpdated, "timeUpdated");
  }

  /**
   * @param u The model value
   *
   * @return The given model value as an A1 value
   */

  public static IdA1AdminSummary of(
    final IdAdminSummary u)
  {
    return new IdA1AdminSummary(
      u.id(),
      u.idName().value(),
      u.realName().value(),
      u.timeCreated(),
      u.timeUpdated()
    );
  }

  @Override
  public IdAdminSummary toModel()
  {
    return new IdAdminSummary(
      this.id,
      new IdName(this.idName),
      new IdRealName(this.realName),
      this.timeCreated,
      this.timeUpdated
    );
  }
}

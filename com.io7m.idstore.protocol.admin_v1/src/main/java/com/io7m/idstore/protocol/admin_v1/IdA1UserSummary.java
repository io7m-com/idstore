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

import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdUserSummary;
import com.io7m.idstore.protocol.api.IdProtocolToModelType;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * A summary of a user.
 *
 * @param id          The user ID
 * @param idName      The user's ID name
 * @param realName    The user's real name
 * @param timeCreated The time the user was created
 * @param timeUpdated The time the user was updated
 */

public record IdA1UserSummary(
  UUID id,
  String idName,
  String realName,
  OffsetDateTime timeCreated,
  OffsetDateTime timeUpdated)
  implements IdProtocolToModelType<IdUserSummary>
{
  /**
   * A summary of a user.
   *
   * @param id          The user ID
   * @param idName      The user's ID name
   * @param realName    The user name
   * @param timeCreated The time the user was created
   * @param timeUpdated The time the user was updated
   */

  public IdA1UserSummary
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

  public static IdA1UserSummary of(
    final IdUserSummary u)
  {
    return new IdA1UserSummary(
      u.id(),
      u.idName().value(),
      u.realName().value(),
      u.timeCreated(),
      u.timeUpdated()
    );
  }

  @Override
  public IdUserSummary toModel()
  {
    return new IdUserSummary(
      this.id,
      new IdName(this.idName),
      new IdRealName(this.realName),
      this.timeCreated,
      this.timeUpdated
    );
  }
}

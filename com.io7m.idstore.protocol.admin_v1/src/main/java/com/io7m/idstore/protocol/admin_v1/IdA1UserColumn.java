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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.io7m.idstore.model.IdUserColumn;
import com.io7m.idstore.protocol.api.IdProtocolToModelType;

/**
 * The user columns that can be used for ordering.
 */

@JsonDeserialize
@JsonSerialize
public enum IdA1UserColumn
  implements IdProtocolToModelType<IdUserColumn>
{
  /**
   * Order by user ID.
   */

  BY_ID,

  /**
   * Order by user id name.
   */

  BY_IDNAME,

  /**
   * Order by user realname.
   */

  BY_REALNAME,

  /**
   * Order by user creation time.
   */

  BY_TIME_CREATED,

  /**
   * Order by user update time.
   */

  BY_TIME_UPDATED;

  /**
   * Convert the given value.
   *
   * @param column The value
   *
   * @return The converted value
   */

  public static IdA1UserColumn of(
    final IdUserColumn column)
  {
    return switch (column) {
      case BY_TIME_CREATED -> BY_TIME_CREATED;
      case BY_ID -> BY_ID;
      case BY_IDNAME -> BY_IDNAME;
      case BY_REALNAME -> BY_REALNAME;
      case BY_TIME_UPDATED -> BY_TIME_UPDATED;
    };
  }

  @Override
  public IdUserColumn toModel()
  {
    return switch (this) {
      case BY_TIME_CREATED -> IdUserColumn.BY_TIME_CREATED;
      case BY_ID -> IdUserColumn.BY_ID;
      case BY_IDNAME -> IdUserColumn.BY_IDNAME;
      case BY_REALNAME -> IdUserColumn.BY_REALNAME;
      case BY_TIME_UPDATED -> IdUserColumn.BY_TIME_UPDATED;
    };
  }
}

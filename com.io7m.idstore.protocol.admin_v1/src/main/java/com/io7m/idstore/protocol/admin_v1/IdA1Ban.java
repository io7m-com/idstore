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
import com.io7m.idstore.model.IdBan;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.api.IdProtocolToModelType;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * An account ban.
 *
 * @param user    The user (or admin) ID
 * @param reason  The ban reason
 * @param expires The expiration date, if any
 */

public record IdA1Ban(
  @JsonProperty(value = "User", required = true)
  UUID user,
  @JsonProperty(value = "Reason", required = true)
  String reason,
  @JsonProperty(value = "Expires")
  @JsonInclude(JsonInclude.Include.NON_ABSENT)
  Optional<OffsetDateTime> expires)
  implements IdProtocolToModelType<IdBan>
{
  /**
   * An account ban.
   *
   * @param user    The user (or admin) ID
   * @param reason  The ban reason
   * @param expires The expiration date, if any
   */

  public IdA1Ban
  {
    Objects.requireNonNull(user, "user");
    Objects.requireNonNull(reason, "reason");
    Objects.requireNonNull(expires, "expires");
  }

  @Override
  public IdBan toModel()
    throws IdProtocolException
  {
    return new IdBan(
      this.user,
      this.reason,
      this.expires
    );
  }

  /**
   * @param ban The model ban
   *
   * @return The ban as an A1 ban
   */

  public static IdA1Ban ofBan(
    final IdBan ban)
  {
    return new IdA1Ban(
      ban.user(),
      ban.reason(),
      ban.expires()
    );
  }
}

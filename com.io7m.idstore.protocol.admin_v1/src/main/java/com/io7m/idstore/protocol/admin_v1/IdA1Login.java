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
import com.io7m.idstore.model.IdLogin;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.api.IdProtocolToModelType;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * A record of a login.
 *
 * @param userId    The user ID
 * @param time      The login time
 * @param host      The host from which the user logged in
 * @param userAgent The user agent
 */

public record IdA1Login(
  @JsonProperty(value = "User", required = true)
  UUID userId,
  @JsonProperty(value = "Time", required = true)
  OffsetDateTime time,
  @JsonProperty(value = "Host", required = true)
  String host,
  @JsonProperty(value = "UserAgent", required = true)
  String userAgent)
  implements IdProtocolToModelType<IdLogin>
{
  /**
   * A record of a login.
   *
   * @param userId    The user ID
   * @param time      The login time
   * @param host      The host from which the user logged in
   * @param userAgent The user agent
   */

  public IdA1Login
  {
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(time, "time");
    Objects.requireNonNull(host, "host");
    Objects.requireNonNull(userAgent, "userAgent");
  }

  @Override
  public IdLogin toModel()
    throws IdProtocolException
  {
    return new IdLogin(
      this.userId,
      this.time,
      this.host,
      this.userAgent
    );
  }

  /**
   * @param login The model value
   *
   * @return An A1 value
   */

  public static IdA1Login ofModel(
    final IdLogin login)
  {
    return new IdA1Login(
      login.userId(),
      login.time(),
      login.host(),
      login.userAgent()
    );
  }
}

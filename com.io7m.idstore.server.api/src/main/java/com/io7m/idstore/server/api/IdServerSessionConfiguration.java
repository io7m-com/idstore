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

package com.io7m.idstore.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for user and admin login sessions.
 *
 * @param userSessionExpiration  The expiration time for inactive user sessions
 * @param adminSessionExpiration The expiration time for inactive admin
 *                               sessions
 */

@JsonDeserialize
@JsonSerialize
public record IdServerSessionConfiguration(
  @JsonProperty(value = "UserSessionExpiration", required = true)
  Duration userSessionExpiration,
  @JsonProperty(value = "AdminSessionExpiration", required = true)
  Duration adminSessionExpiration)
{
  /**
   * Configuration for user and admin login sessions.
   *
   * @param userSessionExpiration  The expiration time for inactive user
   *                               sessions
   * @param adminSessionExpiration The expiration time for inactive admin
   *                               sessions
   */

  public IdServerSessionConfiguration
  {
    Objects.requireNonNull(userSessionExpiration, "userSessionExpiration");
    Objects.requireNonNull(adminSessionExpiration, "adminSessionExpiration");
  }
}

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

import java.util.Objects;
import java.util.Set;

/**
 * Configuration for the parts of the server that serve over HTTP.
 *
 * @param adminAPIService  The admin API service
 * @param userAPIService   The user API service
 * @param userViewService  The user view service
 */

@JsonDeserialize
@JsonSerialize
public record IdServerHTTPConfiguration(
  @JsonProperty(value = "AdminAPIService", required = true)
  IdServerHTTPServiceConfiguration adminAPIService,
  @JsonProperty(value = "UserAPIService", required = true)
  IdServerHTTPServiceConfiguration userAPIService,
  @JsonProperty(value = "UserViewService", required = true)
  IdServerHTTPServiceConfiguration userViewService)
  implements IdServerJSONConfigurationElementType
{
  /**
   * Configuration for the parts of the server that serve over HTTP.
   *
   * @param adminAPIService  The admin API service
   * @param userAPIService   The user API service
   * @param userViewService  The user view service
   */

  public IdServerHTTPConfiguration
  {
    Objects.requireNonNull(adminAPIService, "adminAPIService");
    Objects.requireNonNull(userAPIService, "userAPIService");
    Objects.requireNonNull(userViewService, "userViewService");

    try {
      Set.of(
        Integer.valueOf(adminAPIService.listenPort()),
        Integer.valueOf(userAPIService.listenPort()),
        Integer.valueOf(userViewService.listenPort())
      );
    } catch (final IllegalArgumentException e) {
      throw new IllegalArgumentException(
        "All HTTP services must be configured to listen on different ports: %s"
          .formatted(e.getMessage()),
        e
      );
    }
  }
}

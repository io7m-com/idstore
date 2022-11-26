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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Branding information for the server.
 *
 * @param productTitle The product title
 * @param logo         The path to a logo image
 * @param loginExtra   An XHTML file inserted into the login screen
 * @param scheme       The color scheme
 */

@JsonDeserialize
@JsonSerialize
public record IdServerBrandingConfiguration(
  @JsonProperty(value = "ProductTitle", required = true)
  String productTitle,
  @JsonProperty(value = "Logo", required = false)
  @JsonInclude(value = JsonInclude.Include.NON_ABSENT)
  Optional<Path> logo,
  @JsonProperty(value = "LoginExtraXHTML", required = false)
  @JsonInclude(value = JsonInclude.Include.NON_ABSENT)
  Optional<Path> loginExtra,
  @JsonProperty(value = "ColorScheme", required = false)
  @JsonInclude(value = JsonInclude.Include.NON_ABSENT)
  Optional<IdServerColorScheme> scheme)
{
  /**
   * Branding information for the server.
   *
   * @param productTitle The product title
   * @param logo         The path to a logo image
   * @param loginExtra   An XHTML file inserted into the login screen
   * @param scheme       The color scheme
   */

  public IdServerBrandingConfiguration
  {
    Objects.requireNonNull(productTitle, "productTitle");
    Objects.requireNonNull(logo, "logo");
    Objects.requireNonNull(loginExtra, "loginExtra");
    Objects.requireNonNull(scheme, "scheme");
  }
}

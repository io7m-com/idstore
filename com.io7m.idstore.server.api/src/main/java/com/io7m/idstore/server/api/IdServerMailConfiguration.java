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

import com.io7m.idstore.model.IdEmail;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Configuration for the part of the server that sends mail.
 *
 * @param transportConfiguration      The transport configuration
 * @param authenticationConfiguration The authentication configuration
 * @param senderAddress               The sender address
 * @param verificationExpiration      The maximum age of email verifications
 */

public record IdServerMailConfiguration(
  IdServerMailTransportConfigurationType transportConfiguration,
  Optional<IdServerMailAuthenticationConfiguration> authenticationConfiguration,
  String senderAddress,
  Duration verificationExpiration)
  implements IdServerJSONConfigurationElementType
{
  /**
   * Configuration for the part of the server that sends mail.
   *
   * @param transportConfiguration      The transport configuration
   * @param authenticationConfiguration The authentication configuration
   * @param senderAddress               The sender address
   * @param verificationExpiration      The maximum age of email verifications
   */

  public IdServerMailConfiguration
  {
    Objects.requireNonNull(
      transportConfiguration, "transportConfiguration");
    Objects.requireNonNull(
      authenticationConfiguration, "authenticationConfiguration");
    Objects.requireNonNull(
      senderAddress, "senderAddress");
    Objects.requireNonNull(
      verificationExpiration, "verificationExpiration");

    new IdEmail(senderAddress);
  }
}

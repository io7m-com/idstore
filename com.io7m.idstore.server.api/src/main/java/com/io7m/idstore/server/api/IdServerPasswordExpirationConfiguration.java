/*
 * Copyright © 2023 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

import com.io7m.idstore.model.IdPassword;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Configuration options relating to password expiration.
 *
 * @param userPasswordValidityDuration  The duration for which user passwords are valid
 * @param adminPasswordValidityDuration The duration for which admin passwords are valid
 */

public record IdServerPasswordExpirationConfiguration(
  Optional<Duration> userPasswordValidityDuration,
  Optional<Duration> adminPasswordValidityDuration)
{
  /**
   * Configuration options relating to password expiration.
   *
   * @param userPasswordValidityDuration  The duration for which user passwords are valid
   * @param adminPasswordValidityDuration The duration for which admin passwords are valid
   */

  public IdServerPasswordExpirationConfiguration
  {
    Objects.requireNonNull(
      userPasswordValidityDuration,
      "userPasswordValidityDuration"
    );
    Objects.requireNonNull(
      adminPasswordValidityDuration,
      "adminPasswordValidityDuration"
    );
  }

  /**
   * Take the given password and apply an expiration date to it based on the
   * current clock and settings.
   *
   * @param clock    The clock
   * @param password The password
   *
   * @return The password with a new expiration date
   */

  public IdPassword expireUserPasswordIfNecessary(
    final Clock clock,
    final IdPassword password)
  {
    if (this.userPasswordValidityDuration.isPresent()) {
      final var expiration =
        this.userPasswordValidityDuration.get();
      final var expires =
        OffsetDateTime.now(clock).plus(expiration);
      return password.withExpirationDate(expires);
    }
    return password;
  }

  /**
   * Take the given password and apply an expiration date to it based on the
   * current clock and settings.
   *
   * @param clock    The clock
   * @param password The password
   *
   * @return The password with a new expiration date
   */

  public IdPassword expireAdminPasswordIfNecessary(
    final Clock clock,
    final IdPassword password)
  {
    if (this.adminPasswordValidityDuration.isPresent()) {
      final var expiration =
        this.adminPasswordValidityDuration.get();
      final var expires =
        OffsetDateTime.now(clock).plus(expiration);
      return password.withExpirationDate(expires);
    }
    return password;
  }
}

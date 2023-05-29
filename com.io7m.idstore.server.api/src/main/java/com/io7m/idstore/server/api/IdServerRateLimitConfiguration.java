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

import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for rate limiting.
 *
 * @param emailVerificationRateLimit The minimum allowed time between email
 *                                   verifications for a given user
 * @param passwordResetRateLimit     The minimum allowed time between password
 *                                   resets
 * @param loginRateLimit             The minimum time allowed between logins for a host
 * @param loginDelay                 A delay applied to each login attempt
 */

public record IdServerRateLimitConfiguration(
  Duration emailVerificationRateLimit,
  Duration passwordResetRateLimit,
  Duration loginRateLimit,
  Duration loginDelay)
  implements IdServerJSONConfigurationElementType
{
  /**
   * Configuration for rate limiting.
   *
   * @param emailVerificationRateLimit The minimum allowed time between email
   *                                   verifications for a given user
   * @param passwordResetRateLimit     The minimum allowed time between password
   *                                   resets
   * @param loginRateLimit             The minimum time allowed between logins for a host
   * @param loginDelay                 A delay applied to each login attempt
   */

  public IdServerRateLimitConfiguration
  {
    Objects.requireNonNull(
      emailVerificationRateLimit, "emailVerificationRateLimit");
    Objects.requireNonNull(
      passwordResetRateLimit, "passwordResetRateLimit");
    Objects.requireNonNull(
      loginRateLimit, "loginRateLimit");
    Objects.requireNonNull(
      loginDelay, "loginDelay");
  }
}

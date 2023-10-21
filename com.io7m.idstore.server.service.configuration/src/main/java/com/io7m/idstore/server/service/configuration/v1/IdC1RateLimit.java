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


package com.io7m.idstore.server.service.configuration.v1;

import com.io7m.blackthorne.core.BTElementHandlerType;
import com.io7m.blackthorne.core.BTElementParsingContextType;
import com.io7m.idstore.server.api.IdServerRateLimitConfiguration;
import org.xml.sax.Attributes;

import java.time.Duration;
import java.util.Optional;

final class IdC1RateLimit
  implements BTElementHandlerType<Object, IdServerRateLimitConfiguration>
{
  private IdServerRateLimitConfiguration result;

  IdC1RateLimit(
    final BTElementParsingContextType context)
  {

  }

  @Override
  public void onElementStart(
    final BTElementParsingContextType context,
    final Attributes a)
  {
    this.result =
      new IdServerRateLimitConfiguration(
        requiredDuration(a, "EmailVerificationRateLimit"),
        requiredDuration(a, "PasswordResetRateLimit"),
        optionalDuration(a, "UserLoginRateLimit", Duration.ofSeconds(5L)),
        optionalDuration(a, "UserLoginDelay", Duration.ofSeconds(1L)),
        optionalDuration(a, "AdminLoginRateLimit", Duration.ofSeconds(5L)),
        optionalDuration(a, "AdminLoginDelay", Duration.ofSeconds(1L))
      );
  }

  private static Duration requiredDuration(
    final Attributes attributes,
    final String name)
  {
    return IdC1Durations.parse(attributes.getValue(name));
  }

  private static Duration optionalDuration(
    final Attributes attributes,
    final String name,
    final Duration otherwise)
  {
    return Optional.ofNullable(attributes.getValue(name))
      .map(IdC1Durations::parse)
      .orElse(otherwise);
  }

  @Override
  public IdServerRateLimitConfiguration onElementFinished(
    final BTElementParsingContextType context)
  {
    return this.result;
  }
}

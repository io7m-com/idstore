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

package com.io7m.idstore.model;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A short human-readable token.
 *
 * @param value The token string
 */

public record IdShortHumanToken(String value)
{
  /**
   * The pattern that defines a valid token.
   */

  public static final Pattern VALID_NUMBER =
    Pattern.compile("[0-9]{6}");

  /**
   * A generic token.
   *
   * @param value The token string
   */

  public IdShortHumanToken
  {
    Objects.requireNonNull(value, "value");

    if (!VALID_NUMBER.matcher(value).matches()) {
      throw new IdValidityException(
        "Token value %s must match %s".formatted(value, VALID_NUMBER));
    }
  }

  /**
   * Generate a random token.
   *
   * @param random The secure random instance
   *
   * @return A random token
   */

  public static IdShortHumanToken generate(
    final SecureRandom random)
  {
    Objects.requireNonNull(random, "random");

    final var builder = new StringBuilder(6);
    for (int index = 0; index < 6; ++index) {
      builder.append(random.nextInt(10));
    }
    return new IdShortHumanToken(builder.toString());
  }

  /**
   * Generate a random token, using a default strong RNG instance.
   *
   * @return A random token
   */

  public static IdShortHumanToken generate()
  {
    try {
      return generate(SecureRandom.getInstanceStrong());
    } catch (final NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public String toString()
  {
    return this.value;
  }
}

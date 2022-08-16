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

package com.io7m.idstore.model;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A generic token.
 *
 * @param value The token string
 */

public record IdToken(String value)
{
  /**
   * The pattern that defines a valid token.
   */

  public static final Pattern VALID_HEX =
    Pattern.compile("[A-F0-9]{1,64}");

  /**
   * A generic token.
   *
   * @param value The token string
   */

  public IdToken
  {
    Objects.requireNonNull(value, "value");

    if (!VALID_HEX.matcher(value).matches()) {
      throw new IdValidityException(
        "Token value %s must match %s".formatted(value, VALID_HEX));
    }
  }

  /**
   * Generate a random token.
   *
   * @param random The secure random instance
   *
   * @return A random token
   */

  public static IdToken generate(
    final SecureRandom random)
  {
    Objects.requireNonNull(random, "random");
    final var b = new byte[16];
    random.nextBytes(b);
    return new IdToken(HexFormat.of().formatHex(b).toUpperCase(Locale.ROOT));
  }

  /**
   * Generate a random token, using a default strong RNG instance.
   *
   * @return A random token
   */

  public static IdToken generate()
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

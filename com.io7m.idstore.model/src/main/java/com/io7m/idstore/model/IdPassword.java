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

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Formattable;
import java.util.Formatter;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A hashed password for a user.
 *
 * @param algorithm The hash algorithm
 * @param hash      The hashed password
 * @param salt      The salt value
 * @param expires   The expiration date, if any
 */

public record IdPassword(
  IdPasswordAlgorithmType algorithm,
  String hash,
  String salt,
  Optional<OffsetDateTime> expires)
  implements Formattable
{
  /**
   * The pattern that defines a valid hash.
   */

  public static final Pattern VALID_HEX =
    Pattern.compile("[A-F0-9]+");

  /**
   * A hashed password for a user.
   *
   * @param algorithm The hash algorithm
   * @param hash      The hashed password
   * @param salt      The salt value
   * @param expires   The expiration date, if any
   */

  public IdPassword
  {
    Objects.requireNonNull(algorithm, "algorithm");
    Objects.requireNonNull(hash, "hash");
    Objects.requireNonNull(salt, "salt");
    Objects.requireNonNull(expires, "expires");

    if (!VALID_HEX.matcher(hash).matches()) {
      throw new IdValidityException("Hash must match " + VALID_HEX);
    }
    if (!VALID_HEX.matcher(salt).matches()) {
      throw new IdValidityException("Salt must match " + VALID_HEX);
    }
  }

  /**
   * @return This object as a string, with the hash redacted so that it cannot
   * appear in logs
   */

  @Override
  public String toString()
  {
    return "%s|<REDACTED>|%s|%s"
      .formatted(
        this.algorithm.identifier(),
        this.salt,
        this.expires.map(OffsetDateTime::toString).orElse("")
      );
  }

  /**
   * Check the given plain text password against this hashed password.
   *
   * @param clock        The clock against which to check credentials
   * @param passwordText The plain text password
   *
   * @return {@code  true} iff the password matches
   *
   * @throws IdPasswordException On internal errors such as missing algorithm
   *                             support
   * @see IdPasswordAlgorithmType#check(String, String, byte[])
   */

  public boolean check(
    final Clock clock,
    final String passwordText)
    throws IdPasswordException
  {
    Objects.requireNonNull(passwordText, "passwordText");

    if (!this.checkExpiration(clock)) {
      return false;
    }

    return this.algorithm.check(
      this.hash,
      passwordText,
      HexFormat.of().parseHex(this.salt)
    );
  }

  private boolean checkExpiration(
    final Clock clock)
  {
    if (this.expires.isPresent()) {
      final var expiration = this.expires.get();
      return expiration.isAfter(OffsetDateTime.now(clock));
    }
    return true;
  }

  /**
   * Format this object as a string, with the hash redacted so that it cannot
   * appear in logs.
   *
   * @param flags     The formatting flags
   * @param formatter The formatter
   * @param width     The width
   * @param precision The precision
   */

  @Override
  public void formatTo(
    final Formatter formatter,
    final int flags,
    final int width,
    final int precision)
  {
    formatter.format(
      "%s|<REDACTED>|%s|%s",
      this.algorithm.identifier(),
      this.salt,
      this.expires.map(OffsetDateTime::toString).orElse("")
    );
  }

  /**
   * @param date The date
   *
   * @return This password with the given expiration date
   */

  public IdPassword withExpirationDate(
    final OffsetDateTime date)
  {
    return this.withExpirationDate(Optional.of(date));
  }

  /**
   * @return This password without an expiration date
   */

  public IdPassword withoutExpirationDate()
  {
    return this.withExpirationDate(Optional.empty());
  }

  /**
   * @param newExpiration The expiration date, if any
   *
   * @return This password with the given expiration date
   */
  public IdPassword withExpirationDate(
    final Optional<OffsetDateTime> newExpiration)
  {
    return new IdPassword(
      this.algorithm,
      this.hash,
      this.salt,
      newExpiration
    );
  }
}

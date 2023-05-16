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

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PASSWORD_ERROR;
import static java.util.Locale.ROOT;

/**
 * The PBKDF2 hashing algorithm using a SHA-256 HMAC.
 */

public final class IdPasswordAlgorithmPBKDF2HmacSHA256
  implements IdPasswordAlgorithmType
{
  private static final int DEFAULT_ITERATION_COUNT = 100_000;
  private static final int MAX_ITERATION_COUNT = 1000_000;
  private final int iterationCount;

  private IdPasswordAlgorithmPBKDF2HmacSHA256(
    final int inIterationCount)
  {
    if (Integer.compareUnsigned(inIterationCount, MAX_ITERATION_COUNT) > 0) {
      throw new IdValidityException(
        "It is not permitted to use more than %d iterations of PBKDF2."
          .formatted(Integer.valueOf(MAX_ITERATION_COUNT))
      );
    }
    this.iterationCount = inIterationCount;
  }

  /**
   * Create an algorithm with the given iteration count and key length.
   *
   * @param iterationCount The iteration count
   * @return An algorithm
   */

  public static IdPasswordAlgorithmType create(
    final int iterationCount)
  {
    return new IdPasswordAlgorithmPBKDF2HmacSHA256(iterationCount);
  }

  /**
   * Create an algorithm with a strong iteration count and key length.
   *
   * @return An algorithm
   */

  public static IdPasswordAlgorithmType create()
  {
    return create(DEFAULT_ITERATION_COUNT);
  }

  @Override
  public boolean equals(
    final Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || !this.getClass().equals(o.getClass())) {
      return false;
    }
    final IdPasswordAlgorithmPBKDF2HmacSHA256 that = (IdPasswordAlgorithmPBKDF2HmacSHA256) o;
    return this.iterationCount == that.iterationCount;
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(
      Integer.valueOf(this.iterationCount)
    );
  }

  @Override
  public boolean check(
    final String expectedHash,
    final String receivedPassword,
    final byte[] salt)
    throws IdPasswordException
  {
    Objects.requireNonNull(expectedHash, "expectedHash");
    Objects.requireNonNull(receivedPassword, "receivedPassword");
    Objects.requireNonNull(salt, "salt");

    try {
      final var formatter =
        HexFormat.of();
      final var keyFactory =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

      final var keySpec =
        new PBEKeySpec(
          receivedPassword.toCharArray(),
          salt,
          this.iterationCount,
          256
        );

      final var expectedHashU =
        expectedHash.toUpperCase(ROOT);

      final var receivedHash =
        keyFactory.generateSecret(keySpec).getEncoded();
      final var receivedHashU =
        formatter.formatHex(receivedHash).toUpperCase(ROOT);

      final var size =
        Math.min(expectedHashU.length(), receivedHashU.length());

      int result = 0;
      for (int index = 0; index < size; ++index) {
        final var ec = expectedHashU.codePointAt(index);
        final var rc = receivedHashU.codePointAt(index);
        result |= ec ^ rc;
      }
      return result == 0;
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new IdPasswordException(
        e.getMessage(),
        e,
        PASSWORD_ERROR,
        Map.of(),
        Optional.empty()
      );
    }
  }

  @Override
  public IdPassword createHashed(
    final String passwordText,
    final byte[] salt)
    throws IdPasswordException
  {
    Objects.requireNonNull(passwordText, "passwordText");
    Objects.requireNonNull(salt, "salt");

    try {
      final var formatter =
        HexFormat.of();
      final var passwordSalt =
        formatter.formatHex(salt);
      final var keyFactory =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
      final var keySpec =
        new PBEKeySpec(
          passwordText.toCharArray(),
          salt,
          this.iterationCount,
          256
        );
      final var hash =
        keyFactory.generateSecret(keySpec).getEncoded();
      final var passwordHash =
        formatter.formatHex(hash);

      return new IdPassword(
        this,
        passwordHash.toUpperCase(ROOT),
        passwordSalt.toUpperCase(ROOT)
      );
    } catch (final NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new IdPasswordException(
        e.getMessage(),
        e,
        PASSWORD_ERROR,
        Map.of(),
        Optional.empty()
      );
    }
  }

  @Override
  public String identifier()
  {
    return String.format(
      "%s:%s",
      "PBKDF2WithHmacSHA256",
      Integer.toUnsignedString(this.iterationCount)
    );
  }
}

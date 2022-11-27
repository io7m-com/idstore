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

package com.io7m.idstore.server.service.sessions;

import com.io7m.idstore.model.IdValidityException;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A session identifier. This value is expected to be treated as a secret, as
 * getting access to someone's session identifier allows access to the server
 * authenticated as that user.
 *
 * @param value The identifier
 */

public record IdSessionSecretIdentifier(
  String value)
{
  private static final HexFormat HEX_FORMAT =
    HexFormat.of()
      .withUpperCase();

  private static final Pattern VALID_SECRET =
    Pattern.compile("[A-F0-9]{64,128}");

  /**
   * A session identifier. This value is expected to be treated as a secret, as
   * getting access to someone's session identifier allows access to the server
   * authenticated as that user.
   *
   * @param value The identifier
   */

  public IdSessionSecretIdentifier
  {
    Objects.requireNonNull(value, "value");

    if (!VALID_SECRET.matcher(value).matches()) {
      throw new IdValidityException(
        "Session identifiers must match '%s'".formatted(VALID_SECRET)
      );
    }
  }

  @Override
  public String toString()
  {
    return this.value;
  }

  /**
   * Generate a random identifier.
   *
   * @return The identifier
   */

  public static IdSessionSecretIdentifier generate()
  {
    try {
      return generate(SecureRandom.getInstanceStrong());
    } catch (final NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Generate a random identifier.
   *
   * @param rng A random number generator
   *
   * @return The identifier
   */

  public static IdSessionSecretIdentifier generate(
    final SecureRandom rng)
  {
    final var data = new byte[32];
    rng.nextBytes(data);
    return new IdSessionSecretIdentifier(HEX_FORMAT.formatHex(data));
  }
}

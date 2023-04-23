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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PASSWORD_ERROR;

/**
 * Functions over password algorithms.
 */

public final class IdPasswordAlgorithms
{
  private IdPasswordAlgorithms()
  {

  }

  /**
   * Parse a password algorithm identifier (such as
   * "PBKDF2WithHmacSHA256:10000:256").
   *
   * @param text The identifier
   *
   * @return A password algorithm
   *
   * @throws IdPasswordException On parse errors
   * @see IdPasswordAlgorithmType#identifier()
   */

  public static IdPasswordAlgorithmType parse(
    final String text)
    throws IdPasswordException
  {
    Objects.requireNonNull(text, "text");

    final var segments = List.of(text.split(":"));

    final var name = segments.get(0);
    return switch (name) {
      case "REDACTED" -> {
        yield IdPasswordAlgorithmRedacted.create();
      }

      case "PBKDF2WithHmacSHA256" -> {
        try {
          if (segments.size() == 3) {
            yield IdPasswordAlgorithmPBKDF2HmacSHA256.create(
              Integer.parseUnsignedInt(segments.get(1)),
              Integer.parseUnsignedInt(segments.get(2))
            );
          }

          throw new IdPasswordException(
            "Unparseable password algorithm.",
            PASSWORD_ERROR,
            Map.ofEntries(
              Map.entry(
                "Expected",
                "'PBKDF2WithHmacSHA256' : <iteration count> : <key length>"),
              Map.entry("Received", text)
            ),
            Optional.of("Use the correct syntax.")
          );
        } catch (final NumberFormatException e) {
          throw new IdPasswordException(
            e.getMessage(),
            e,
            PASSWORD_ERROR,
            Map.of(),
            Optional.empty()
          );
        }
      }

      default -> {
        throw new IdPasswordException(
          "Unsupported algorithm: %s".formatted(name),
          PASSWORD_ERROR,
          Map.of(),
          Optional.empty()
        );
      }
    };
  }
}

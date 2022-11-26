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


package com.io7m.idstore.tests.model;

import com.io7m.idstore.error_codes.IdErrorCode;
import com.io7m.idstore.error_codes.IdStandardErrorCodes;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static java.lang.reflect.Modifier.STATIC;

public final class IdStandardErrorCodesTest
{
  @Test
  public void testErrorCodesUnique()
  {
    final var errorCodes =
      standardErrorCodes();

    final var codesUnique = new HashSet<IdErrorCode>();
    final var codesDuplicate = new HashSet<IdErrorCode>();
    for (final var errorCode : errorCodes) {
      if (codesUnique.contains(errorCode)) {
        codesDuplicate.add(errorCode);
      }
      codesUnique.add(errorCode);
    }

    if (!codesDuplicate.isEmpty()) {
      Assertions.fail("Non unique error codes: %s".formatted(codesDuplicate));
    }
  }

  public static List<IdErrorCode> standardErrorCodes()
  {
    return Arrays.stream(IdStandardErrorCodes.class.getFields())
      .filter(f -> (f.getModifiers() & STATIC) == STATIC)
      .filter(f -> f.getType().equals(IdErrorCode.class))
      .map(f -> {
        try {
          return f.get(null);
        } catch (final IllegalAccessException e) {
          throw new IllegalStateException(e);
        }
      })
      .map(IdErrorCode.class::cast)
      .toList();
  }
}

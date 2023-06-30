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


package com.io7m.idstore.tests.server.service.sessions;

import com.io7m.idstore.model.IdValidityException;
import com.io7m.idstore.server.service.sessions.IdSessionSecretIdentifier;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.Size;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.HexFormat;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class IdSessionSecretIdentifierTest
{
  @TestFactory
  public Stream<DynamicTest> testValid()
  {
    final var text = new StringBuilder();
    for (int index = 0; index <= 127; ++index) {
      text.append('A');
    }

    return Stream.of(
      "D509D6CC1758D30D38096FC680294C34090BE15E8869CC8270248588A1355CF1",
      text.toString()
    ).map(IdSessionSecretIdentifierTest::validTestOf);
  }

  private static DynamicTest validTestOf(
    final String name)
  {
    return DynamicTest.dynamicTest(
      "testValid_%s".formatted(name),
      () -> {
        new IdSessionSecretIdentifier(name);
      });
  }

  @TestFactory
  public Stream<DynamicTest> testInvalid()
  {
    final var text = new StringBuilder();
    for (int index = 0; index <= 128; ++index) {
      text.append('A');
    }

    return Stream.of(
      "d509d6cc1758d30d38096fc680294c34090be15e8869cc8270248588a1355cf1",
      "_",
      "-",
      "1",
      "A",
      text.toString()
    ).map(IdSessionSecretIdentifierTest::invalidTestOf);
  }

  private static DynamicTest invalidTestOf(
    final String name)
  {
    return DynamicTest.dynamicTest(
      "testInvalid_%s".formatted(name),
      () -> {
        assertThrows(IdValidityException.class, () -> {
          new IdSessionSecretIdentifier(name);
        });
      });
  }

  @Property
  public void testEquals(
    final @ForAll @Size(min = 32, max = 64) byte[] data)
  {
    final var hex =
      HexFormat.of()
        .withUpperCase();

    final var x = new IdSessionSecretIdentifier(hex.formatHex(data));
    final var y = new IdSessionSecretIdentifier(hex.formatHex(data));
    assertEquals(x, y);
    assertEquals(x.toString(), y.toString());
  }
}

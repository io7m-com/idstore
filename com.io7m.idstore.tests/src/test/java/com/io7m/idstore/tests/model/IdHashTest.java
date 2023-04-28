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

package com.io7m.idstore.tests.model;

import com.io7m.idstore.model.IdHash;
import com.io7m.idstore.model.IdValidityException;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class IdHashTest
{
  @Test
  public void testValid()
  {
    assertEquals(
      "SHA-256:A2F6017F1FAB81333A4288F68557B74495A27337C7D37B3EBA46C866AA885098",
      IdHash.sha256Of("HELLO!".getBytes(StandardCharsets.UTF_8)).toString()
    );
  }

  @TestFactory
  public Stream<DynamicTest> testInvalid()
    throws IOException
  {
    return Stream.of(
      "G")
      .map(IdHashTest::invalidTestOf);
  }

  private static DynamicTest invalidTestOf(
    final String text)
  {
    return DynamicTest.dynamicTest("testInvalid_" + text, () -> {
      assertThrows(IdValidityException.class, () -> {
        new IdHash("SHA-256", text);
      });
    });
  }
}

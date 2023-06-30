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

import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdValidityException;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class IdRealNameTest
{
  @TestFactory
  public Stream<DynamicTest> testValid()
  {
    return Stream.of(
        "a",
        "Someone Else",
        "a9",
        "a-b",
        "a_b",
        "A-B",
        "A_b",
        "a.a.a.a.a.a.a.a.a.a.a.a.a.a.a.a.a.a.a.a.a.a.a.a.a.a",
        "com.io7m.idstore",
        "a.b.c")
      .map(IdRealNameTest::validTestOf);
  }

  @TestFactory
  public Stream<DynamicTest> testInvalid()
  {
    return Stream.of(
        " ",
        "a".repeat(1025))
      .map(IdRealNameTest::invalidTestOf);
  }

  private static DynamicTest validTestOf(
    final String text)
  {
    return DynamicTest.dynamicTest("testValid_" + text, () -> {
      new IdRealName(text);
    });
  }

  private static DynamicTest invalidTestOf(
    final String text)
  {
    return DynamicTest.dynamicTest("testInvalid_" + text, () -> {
      assertThrows(IdValidityException.class, () -> {
        new IdRealName(text);
      });
    });
  }
}

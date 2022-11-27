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

import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdValidityException;
import com.io7m.idstore.tests.IdTestDirectories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class IdEmailTest
{
  private Path directory;

  @BeforeEach
  public void setup()
    throws IOException
  {
    this.directory = IdTestDirectories.createTempDirectory();
  }

  @AfterEach
  public void tearDown()
    throws IOException
  {
    IdTestDirectories.deleteDirectory(this.directory);
  }

  @Test
  public void testTooLong()
  {
    assertThrows(IdValidityException.class, () -> {
      new IdEmail("%s@example.com".formatted("x".repeat(512)));
    });
  }

  @TestFactory
  public Stream<DynamicTest> testValid()
    throws IOException
  {
    final var path =
      IdTestDirectories.resourceOf(
        IdEmailTest.class,
        this.directory,
        "email-valid.txt"
      );

    return Files.lines(path)
      .map(IdEmailTest::validTestOf);
  }

  @TestFactory
  public Stream<DynamicTest> testInvalid()
    throws IOException
  {
    final var path =
      IdTestDirectories.resourceOf(
        IdEmailTest.class,
        this.directory,
        "email-invalid.txt"
      );

    return Files.lines(path)
      .map(IdEmailTest::invalidTestOf);
  }

  private static DynamicTest validTestOf(
    final String text)
  {
    return DynamicTest.dynamicTest("testValid_" + text, () -> {
      new IdEmail(text);
    });
  }

  private static DynamicTest invalidTestOf(
    final String text)
  {
    return DynamicTest.dynamicTest("testInvalid_" + text, () -> {
      assertThrows(IdValidityException.class, () -> {
        new IdEmail(text);
      });
    });
  }
}

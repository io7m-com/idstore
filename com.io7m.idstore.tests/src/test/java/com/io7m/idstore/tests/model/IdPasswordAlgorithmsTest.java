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

import com.io7m.idstore.model.IdPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.idstore.model.IdPasswordAlgorithmRedacted;
import com.io7m.idstore.model.IdPasswordAlgorithms;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdValidityException;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class IdPasswordAlgorithmsTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdPasswordAlgorithmsTest.class);
  private Clock clock;

  @BeforeEach
  public void setup()
  {
    this.clock =
      createFutureClock();
  }

  @Test
  public void testPBKDF2()
    throws Exception
  {
    final var p =
      IdPasswordAlgorithms.parse("PBKDF2WithHmacSHA256:10000");

    assertInstanceOf(IdPasswordAlgorithmPBKDF2HmacSHA256.class, p);
    assertEquals("PBKDF2WithHmacSHA256:10000", p.identifier());
  }

  @Test
  public void testRedacted()
    throws Exception
  {
    final var p =
      IdPasswordAlgorithms.parse("REDACTED");

    assertInstanceOf(IdPasswordAlgorithmRedacted.class, p);
    assertEquals("REDACTED", p.identifier());
  }

  @Test
  public void testPBKDF2Execute()
    throws Exception
  {
    final var algorithm =
      IdPasswordAlgorithmPBKDF2HmacSHA256.create(10000);

    final var salt = new byte[4];
    salt[0] = (byte) 0x10;
    salt[1] = (byte) 0x20;
    salt[2] = (byte) 0x30;
    salt[3] = (byte) 0x40;

    final var password =
      algorithm.createHashed("12345678", salt);

    LOG.debug("hash: {}", password.hash());
    LOG.debug("salt: {}", password.salt());

    assertTrue(password.check(this.clock, "12345678"));
    assertFalse(password.check(this.clock, "1"));
  }

  @TestFactory
  public Stream<DynamicTest> testUnparseable()
  {
    return Stream.of(
      "",
      "PBKDF2WithHmacSHA256",
      "PBKDF2WithHmacSHA256:10000:x",
      "PBKDF2WithHmacSHA256:y:245"
    ).map(IdPasswordAlgorithmsTest::testUnparseableOf);
  }

  @Test
  public void testTooManyIterations()
  {
    assertThrows(IdValidityException.class, () -> {
      IdPasswordAlgorithmPBKDF2HmacSHA256.create(1_000_001);
    });
  }

  private static DynamicTest testUnparseableOf(
    final String text)
  {
    return DynamicTest.dynamicTest(
      "testUnparseable_" + text,
      () -> {
        Assertions.assertThrows(IdPasswordException.class, () -> {
          IdPasswordAlgorithms.parse(text);
        });
      }
    );
  }

  /**
   * Redacted passwords never check.
   *
   * @param text The text
   *
   * @throws Exception On errors
   */

  @Property
  public void testRedactedPasswordsNeverCheck(
    final @ForAll String text)
    throws Exception
  {
    final var password =
      IdPasswordAlgorithmRedacted.create()
        .createHashed(text);

    assertFalse(password.check(this.clock, text));
  }

  @Provide
  public static Arbitrary<Instant> instants()
  {
    return Arbitraries.longs()
      .map(Instant::ofEpochMilli);
  }

  /**
   * Removing and re-adding an expiration date works.
   *
   * @param text The text
   * @param time The expiration time
   *
   * @throws Exception On errors
   */

  @Property(tries = 10)
  public void testExpiration(
    final @ForAll String text,
    final @ForAll("instants") Instant time)
    throws Exception
  {
    final var p0 =
      IdPasswordAlgorithmPBKDF2HmacSHA256.create()
        .createHashed(text);

    final var offTime =
      OffsetDateTime.ofInstant(time, ZoneId.systemDefault());

    final var p1 =
      p0.withExpirationDate(offTime);
    final var p2 =
      p1.withoutExpirationDate();

    assertEquals(p0, p2);
  }

  /**
   * Expired passwords fail the check.
   *
   * @param text The text
   *
   * @throws Exception On errors
   */

  @Property(tries = 10)
  public void testExpiredPassword(
    final @ForAll String text)
    throws Exception
  {
    this.clock =
      createFutureClock();

    final var p0 =
      IdPasswordAlgorithmPBKDF2HmacSHA256.create()
        .createHashed(text);

    final var offTime =
      OffsetDateTime.now()
        .minusDays(1L);

    final var p1 =
      p0.withExpirationDate(offTime);

    assertFalse(p1.check(this.clock, text));
  }

  /**
   * Create a clock that always returns a time one day into the future.
   */

  private static Clock createFutureClock()
  {
    return Clock.fixed(
      Instant.now().plusSeconds(86400L),
      ZoneId.systemDefault()
    );
  }
}

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

package com.io7m.idstore.tests;

import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.versions.IdVMessageType;
import com.io7m.idstore.protocol.versions.cb.IdVCB1Messages;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.CannotFindArbitraryException;
import net.jqwik.engine.properties.arbitraries.DefaultTypeArbitrary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class IdVMessagesTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdVMessagesTest.class);

  private IdVCB1Messages messages;

  private static <T> Set<Class<? extends T>> enumerateSubclasses(
    final Class<? extends T> clazz)
  {
    final var classes = new HashSet<Class<? extends T>>();
    enumerateSubclassesStep(clazz, classes);
    return classes;
  }

  private static <T> void enumerateSubclassesStep(
    final Class<? extends T> clazz,
    final HashSet<Class<? extends T>> classes)
  {
    if (!clazz.isInterface()) {
      classes.add(clazz);
    }

    final var subs = clazz.getPermittedSubclasses();
    if (subs != null) {
      for (final var sub : subs) {
        enumerateSubclassesStep((Class<? extends T>) sub, classes);
      }
    }
  }

  private static IdVMessageType arbitraryOf(
    final Class<? extends IdVMessageType> c)
  {
    Arbitrary<? extends IdVMessageType> arbitrary = null;

    try {
      arbitrary = Arbitraries.defaultFor(c);
    } catch (final CannotFindArbitraryException e) {
      // OK
    }

    try {
      if (arbitrary == null) {
        arbitrary = Arbitraries.forType(c);
      }
    } catch (final CannotFindArbitraryException e) {
      // OK
    }

    if (arbitrary == null) {
      arbitrary = new DefaultTypeArbitrary<>(c);
    }

    LOG.debug("arbitrary: {}", arbitrary);
    final var v = arbitrary.sample();
    LOG.debug("value: {}", v);
    return v;
  }

  @BeforeEach
  public void setup()
  {
    this.messages = new IdVCB1Messages();
  }

  /**
   * Messages are correctly serialized and parsed.
   */

  @TestFactory
  public Stream<DynamicTest> testRoundTripReflective()
  {
    final var classes =
      enumerateSubclasses(IdVMessageType.class);

    assertFalse(classes.isEmpty());

    return classes.stream()
      .map(IdVMessagesTest::arbitraryOf)
      .map(this::dynamicTestOfRoundTrip);
  }

  private DynamicTest dynamicTestOfRoundTrip(
    final IdVMessageType o)
  {
    return DynamicTest.dynamicTest(
      "testRoundTrip_" + o.getClass(),
      () -> {
        final var b = this.messages.serialize(o);
        LOG.debug("{}", new String(b, UTF_8));
        assertEquals(o, this.messages.parse(b));
      }
    );
  }

  /**
   * Invalid messages aren't parsed.
   *
   * @throws Exception On errors
   */

  @Test
  public void testInvalid()
    throws Exception
  {
    assertThrows(IdProtocolException.class, () -> {
      this.messages.parse("{}".getBytes(UTF_8));
    });
  }
}

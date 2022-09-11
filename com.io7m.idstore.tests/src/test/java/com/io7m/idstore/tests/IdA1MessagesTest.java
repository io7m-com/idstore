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

import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSelf;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAuditSearchBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAuditSearchNext;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAuditSearchPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandLogin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserCreate;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserGet;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserGetByEmail;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserSearchBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserSearchByEmailBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserSearchByEmailNext;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserSearchNext;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserSearchPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserUpdate;
import com.io7m.idstore.protocol.admin_v1.IdA1MessageType;
import com.io7m.idstore.protocol.admin_v1.IdA1Messages;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminSelf;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAuditSearchBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAuditSearchNext;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAuditSearchPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseError;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseLogin;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserCreate;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserGet;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserSearchBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserSearchByEmailBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserSearchByEmailNext;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserSearchNext;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserSearchPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserUpdate;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.CannotFindArbitraryException;
import net.jqwik.engine.properties.arbitraries.DefaultTypeArbitrary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class IdA1MessagesTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdA1MessagesTest.class);

  private IdA1Messages messages;
  private Path directory;

  private static final Map<
    Class<? extends IdA1MessageType>,
    Arbitrary<? extends IdA1MessageType>> MESSAGE_ARBITRARIES =
    Map.ofEntries(
      entry(IdA1CommandAuditSearchBegin.class, commandAuditSearchBegin()),
      entry(IdA1CommandAuditSearchNext.class, commandAuditSearchNext()),
      entry(IdA1CommandAuditSearchPrevious.class, commandAuditSearchPrevious()),
      entry(IdA1CommandAdminSelf.class, commandAdminSelf()),
      entry(IdA1CommandLogin.class, commandLogin()),
      entry(IdA1CommandUserSearchBegin.class, commandUserSearchBegin()),
      entry(IdA1CommandUserSearchNext.class, commandUserSearchNext()),
      entry(IdA1CommandUserSearchPrevious.class, commandUserSearchPrevious()),
      entry(IdA1CommandUserSearchByEmailBegin.class, commandUserSearchByEmailBegin()),
      entry(IdA1CommandUserSearchByEmailNext.class, commandUserSearchByEmailNext()),
      entry(IdA1CommandUserSearchByEmailPrevious.class, commandUserSearchByEmailPrevious()),
      entry(IdA1CommandUserGet.class, commandUserGet()),
      entry(IdA1CommandUserGetByEmail.class, commandUserGetByEmail()),
      entry(IdA1CommandUserUpdate.class, commandUserUpdate()),
      entry(IdA1CommandUserCreate.class, commandUserCreate()),
      entry(IdA1ResponseAdminSelf.class, responseAdminSelf()),
      entry(IdA1ResponseError.class, responseError()),
      entry(IdA1ResponseLogin.class, responseLogin()),
      entry(IdA1ResponseUserSearchBegin.class, responseUserSearchBegin()),
      entry(IdA1ResponseUserSearchNext.class, responseUserSearchNext()),
      entry(IdA1ResponseUserSearchPrevious.class, responseUserSearchPrevious()),
      entry(IdA1ResponseUserSearchByEmailBegin.class, responseUserSearchByEmailBegin()),
      entry(IdA1ResponseUserSearchByEmailNext.class, responseUserSearchByEmailNext()),
      entry(IdA1ResponseUserSearchByEmailPrevious.class, responseUserSearchByEmailPrevious()),
      entry(IdA1ResponseUserGet.class, responseUserGet()),
      entry(IdA1ResponseUserUpdate.class, responseUserUpdate()),
      entry(IdA1ResponseUserCreate.class, responseUserCreate()),
      entry(IdA1ResponseAuditSearchBegin.class, responseAuditSearchBegin()),
      entry(IdA1ResponseAuditSearchNext.class, responseAuditSearchNext()),
      entry(IdA1ResponseAuditSearchPrevious.class, responseAuditSearchPrevious())
    );

  private static <T> List<Class<? extends T>> enumerateSubclasses(
    final Class<? extends T> clazz)
  {
    final var classes = new HashSet<Class<? extends T>>();
    enumerateSubclassesStep(clazz, classes);
    return classes.stream()
      .sorted(Comparator.comparing(Class::getCanonicalName))
      .toList();
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

  private static IdA1MessageType arbitraryOf(
    final Class<? extends IdA1MessageType> c)
  {
    final var arbitrary =
      Optional.ofNullable(MESSAGE_ARBITRARIES.get(c))
          .orElseThrow(() -> {
            return new NoSuchElementException(c.getCanonicalName());
          });

    LOG.debug("arbitrary: {}", arbitrary);
    final var v = arbitrary.sample();
    LOG.debug("value: {}", v);
    return v;
  }

  @BeforeEach
  public void setup()
    throws IOException
  {
    this.messages = new IdA1Messages();
    this.directory = IdTestDirectories.createTempDirectory();
  }

  @AfterEach
  public void tearDown()
    throws IOException
  {
    IdTestDirectories.deleteDirectory(this.directory);
  }

  /**
   * Messages are correctly serialized and parsed.
   */

  @TestFactory
  public Stream<DynamicTest> testRoundTripReflective()
  {
    final var classes =
      enumerateSubclasses(IdA1MessageType.class);

    assertFalse(classes.isEmpty());

    return classes.stream()
      .map(IdA1MessagesTest::arbitraryOf)
      .map(this::dynamicTestOfRoundTrip);
  }

  private DynamicTest dynamicTestOfRoundTrip(
    final IdA1MessageType o)
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

  /**
   * Test a specific problematic case.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCase0()
    throws Exception
  {
    this.messages.parse(
      IdTestDirectories.resourceBytesOf(
        IdA1MessagesTest.class,
        this.directory,
        "case-0.json")
    );
  }
}

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

import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminBanCreate;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminBanDelete;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminBanGet;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminCreate;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminDelete;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminEmailAdd;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminEmailRemove;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminGet;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminGetByEmail;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminPermissionGrant;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminPermissionRevoke;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSearchBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSearchByEmailBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSearchByEmailNext;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSearchNext;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSearchPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSelf;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminUpdate;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAuditSearchBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAuditSearchNext;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAuditSearchPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandLogin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserBanCreate;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserBanDelete;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserBanGet;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserCreate;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserDelete;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserEmailAdd;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserEmailRemove;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserGet;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserGetByEmail;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserLoginHistory;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserSearchBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserSearchByEmailBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserSearchByEmailNext;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserSearchNext;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserSearchPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserUpdate;
import com.io7m.idstore.protocol.admin_v1.IdA1MessageType;
import com.io7m.idstore.protocol.admin_v1.IdA1Messages;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminBanCreate;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminBanDelete;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminBanGet;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminCreate;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminDelete;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminGet;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminSearchBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminSearchByEmailBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminSearchByEmailNext;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminSearchNext;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminSearchPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminSelf;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminUpdate;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAuditSearchBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAuditSearchNext;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAuditSearchPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseError;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseLogin;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserBanCreate;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserBanDelete;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserBanGet;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserCreate;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserDelete;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserGet;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserLoginHistory;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserSearchBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserSearchByEmailBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserSearchByEmailNext;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserSearchNext;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserSearchPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserUpdate;
import com.io7m.idstore.protocol.api.IdProtocolException;
import net.jqwik.api.Arbitrary;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandAdminBanCreate;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandAdminBanDelete;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandAdminBanGet;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandAdminCreate;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandAdminDelete;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandAdminEmailAdd;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandAdminEmailRemove;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandAdminGet;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandAdminGetByEmail;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandAdminPermissionGrant;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandAdminPermissionRevoke;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandAdminSearchBegin;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandAdminSearchByEmailBegin;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandAdminSearchByEmailNext;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandAdminSearchByEmailPrevious;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandAdminSearchNext;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandAdminSearchPrevious;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandAdminSelf;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandAdminUpdate;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandAuditSearchBegin;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandAuditSearchNext;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandAuditSearchPrevious;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandLogin;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandUserBanCreate;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandUserBanDelete;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandUserBanGet;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandUserCreate;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandUserDelete;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandUserEmailAdd;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandUserEmailRemove;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandUserGet;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandUserGetByEmail;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandUserLoginHistory;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandUserSearchBegin;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandUserSearchByEmailBegin;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandUserSearchByEmailNext;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandUserSearchByEmailPrevious;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandUserSearchNext;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandUserSearchPrevious;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.commandUserUpdate;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseAdminBanCreate;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseAdminBanDelete;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseAdminBanGet;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseAdminCreate;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseAdminDelete;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseAdminGet;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseAdminSearchBegin;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseAdminSearchByEmailBegin;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseAdminSearchByEmailNext;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseAdminSearchByEmailPrevious;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseAdminSearchNext;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseAdminSearchPrevious;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseAdminSelf;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseAdminUpdate;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseAuditSearchBegin;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseAuditSearchNext;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseAuditSearchPrevious;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseError;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseLogin;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseUserBanCreate;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseUserBanDelete;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseUserBanGet;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseUserCreate;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseUserDelete;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseUserGet;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseUserLoginHistory;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseUserSearchBegin;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseUserSearchByEmailBegin;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseUserSearchByEmailNext;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseUserSearchByEmailPrevious;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseUserSearchNext;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseUserSearchPrevious;
import static com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider.responseUserUpdate;
import static java.nio.charset.StandardCharsets.UTF_8;
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
    Arbitrary<? extends IdA1MessageType>> MESSAGE_ARBITRARIES;

  static {
    MESSAGE_ARBITRARIES = new HashMap<>();

    final var m = MESSAGE_ARBITRARIES;
    m.put(IdA1CommandUserLoginHistory.class, commandUserLoginHistory());
    m.put(IdA1ResponseUserLoginHistory.class, responseUserLoginHistory());
    m.put(IdA1CommandAdminBanCreate.class, commandAdminBanCreate());
    m.put(IdA1CommandAdminBanDelete.class, commandAdminBanDelete());
    m.put(IdA1CommandAdminBanGet.class, commandAdminBanGet());
    m.put(IdA1CommandAdminCreate.class, commandAdminCreate());
    m.put(IdA1CommandAdminDelete.class, commandAdminDelete());
    m.put(IdA1CommandAdminEmailAdd.class, commandAdminEmailAdd());
    m.put(IdA1CommandAdminEmailRemove.class, commandAdminEmailRemove());
    m.put(IdA1CommandAdminGet.class, commandAdminGet());
    m.put(IdA1CommandAdminGetByEmail.class, commandAdminGetByEmail());
    m.put(IdA1CommandAdminPermissionGrant.class, commandAdminPermissionGrant());
    m.put(IdA1CommandAdminPermissionRevoke.class, commandAdminPermissionRevoke());
    m.put(IdA1CommandAdminSearchBegin.class, commandAdminSearchBegin());
    m.put(
      IdA1CommandAdminSearchByEmailBegin.class,
      commandAdminSearchByEmailBegin());
    m.put(
      IdA1CommandAdminSearchByEmailNext.class,
      commandAdminSearchByEmailNext());
    m.put(
      IdA1CommandAdminSearchByEmailPrevious.class,
      commandAdminSearchByEmailPrevious());
    m.put(IdA1CommandAdminSearchNext.class, commandAdminSearchNext());
    m.put(IdA1CommandAdminSearchPrevious.class, commandAdminSearchPrevious());
    m.put(IdA1CommandAdminSelf.class, commandAdminSelf());
    m.put(IdA1CommandAdminUpdate.class, commandAdminUpdate());
    m.put(IdA1CommandAuditSearchBegin.class, commandAuditSearchBegin());
    m.put(IdA1CommandAuditSearchNext.class, commandAuditSearchNext());
    m.put(IdA1CommandAuditSearchPrevious.class, commandAuditSearchPrevious());
    m.put(IdA1CommandLogin.class, commandLogin());
    m.put(IdA1CommandUserBanCreate.class, commandUserBanCreate());
    m.put(IdA1CommandUserBanDelete.class, commandUserBanDelete());
    m.put(IdA1CommandUserBanGet.class, commandUserBanGet());
    m.put(IdA1CommandUserCreate.class, commandUserCreate());
    m.put(IdA1CommandUserDelete.class, commandUserDelete());
    m.put(IdA1CommandUserEmailAdd.class, commandUserEmailAdd());
    m.put(IdA1CommandUserEmailRemove.class, commandUserEmailRemove());
    m.put(IdA1CommandUserGet.class, commandUserGet());
    m.put(IdA1CommandUserGetByEmail.class, commandUserGetByEmail());
    m.put(IdA1CommandUserSearchBegin.class, commandUserSearchBegin());
    m.put(
      IdA1CommandUserSearchByEmailBegin.class,
      commandUserSearchByEmailBegin());
    m.put(
      IdA1CommandUserSearchByEmailNext.class,
      commandUserSearchByEmailNext());
    m.put(
      IdA1CommandUserSearchByEmailPrevious.class,
      commandUserSearchByEmailPrevious());
    m.put(IdA1CommandUserSearchNext.class, commandUserSearchNext());
    m.put(IdA1CommandUserSearchPrevious.class, commandUserSearchPrevious());
    m.put(IdA1CommandUserUpdate.class, commandUserUpdate());
    m.put(IdA1ResponseAdminBanCreate.class, responseAdminBanCreate());
    m.put(IdA1ResponseAdminBanDelete.class, responseAdminBanDelete());
    m.put(IdA1ResponseAdminBanGet.class, responseAdminBanGet());
    m.put(IdA1ResponseAdminCreate.class, responseAdminCreate());
    m.put(IdA1ResponseAdminDelete.class, responseAdminDelete());
    m.put(IdA1ResponseAdminGet.class, responseAdminGet());
    m.put(IdA1ResponseAdminSearchBegin.class, responseAdminSearchBegin());
    m.put(
      IdA1ResponseAdminSearchByEmailBegin.class,
      responseAdminSearchByEmailBegin());
    m.put(
      IdA1ResponseAdminSearchByEmailNext.class,
      responseAdminSearchByEmailNext());
    m.put(
      IdA1ResponseAdminSearchByEmailPrevious.class,
      responseAdminSearchByEmailPrevious());
    m.put(IdA1ResponseAdminSearchNext.class, responseAdminSearchNext());
    m.put(IdA1ResponseAdminSearchPrevious.class, responseAdminSearchPrevious());
    m.put(IdA1ResponseAdminSelf.class, responseAdminSelf());
    m.put(IdA1ResponseAdminUpdate.class, responseAdminUpdate());
    m.put(IdA1ResponseAuditSearchBegin.class, responseAuditSearchBegin());
    m.put(IdA1ResponseAuditSearchNext.class, responseAuditSearchNext());
    m.put(IdA1ResponseAuditSearchPrevious.class, responseAuditSearchPrevious());
    m.put(IdA1ResponseError.class, responseError());
    m.put(IdA1ResponseLogin.class, responseLogin());
    m.put(IdA1ResponseUserBanCreate.class, responseUserBanCreate());
    m.put(IdA1ResponseUserBanDelete.class, responseUserBanDelete());
    m.put(IdA1ResponseUserBanGet.class, responseUserBanGet());
    m.put(IdA1ResponseUserCreate.class, responseUserCreate());
    m.put(IdA1ResponseUserDelete.class, responseUserDelete());
    m.put(IdA1ResponseUserGet.class, responseUserGet());
    m.put(IdA1ResponseUserSearchBegin.class, responseUserSearchBegin());
    m.put(
      IdA1ResponseUserSearchByEmailBegin.class,
      responseUserSearchByEmailBegin());
    m.put(
      IdA1ResponseUserSearchByEmailNext.class,
      responseUserSearchByEmailNext());
    m.put(
      IdA1ResponseUserSearchByEmailPrevious.class,
      responseUserSearchByEmailPrevious());
    m.put(IdA1ResponseUserSearchNext.class, responseUserSearchNext());
    m.put(IdA1ResponseUserSearchPrevious.class, responseUserSearchPrevious());
    m.put(IdA1ResponseUserUpdate.class, responseUserUpdate());
  }

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

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


package com.io7m.idstore.tests.server;

import com.io7m.ervilla.api.EContainerSupervisorType;
import com.io7m.ervilla.test_extension.ErvillaCloseAfterAll;
import com.io7m.ervilla.test_extension.ErvillaConfiguration;
import com.io7m.ervilla.test_extension.ErvillaExtension;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.server.api.IdServerException;
import com.io7m.idstore.server.vanilla.IdServers;
import com.io7m.idstore.tests.extensions.IdTestDatabases;
import com.io7m.idstore.tests.extensions.IdTestServers;
import com.io7m.zelador.test_extension.CloseableResourcesType;
import com.io7m.zelador.test_extension.ZeladorExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({ErvillaExtension.class, ZeladorExtension.class})
@ErvillaConfiguration(disabledIfUnsupported = true)
public final class IdServerTest
{
  private static IdTestDatabases.IdDatabaseFixture DATABASE_FIXTURE;
  private static final IdServers SERVERS = new IdServers();
  private IdTestServers.IdTestServerFixture serverFixture;

  @BeforeAll
  public static void setupOnce(
    final @ErvillaCloseAfterAll EContainerSupervisorType containers)
    throws Exception
  {
    DATABASE_FIXTURE = IdTestDatabases.create(containers, 15432);
  }

  @BeforeEach
  public void setup(
    final CloseableResourcesType closeables)
    throws Exception
  {
    DATABASE_FIXTURE.reset();

    this.serverFixture =
      closeables.addPerTestResource(IdTestServers.create(
        DATABASE_FIXTURE,
        30025,
        50000,
        50001,
        51000
      ));
  }

  /**
   * Check that starting and stopping the server does not leak threads.
   */

  @Test
  public void testStartStop()
    throws Exception
  {
    final var server = this.serverFixture.server();
    server.close();
    assertTrue(server.isClosed());

    server.start();
    assertFalse(server.isClosed());

    final var adminId = UUID.randomUUID();
    server.createOrUpdateInitialAdmin(
      adminId,
      new IdName("x"),
      new IdEmail("x@example.com"),
      new IdRealName("Ex"),
      "12345678"
    );

    server.close();
    assertTrue(server.isClosed());

    server.start();
    assertFalse(server.isClosed());

    server.createOrUpdateInitialAdmin(
      adminId,
      new IdName("x"),
      new IdEmail("x@example.com"),
      new IdRealName("Ex"),
      "12345678"
    );

    server.close();
    assertTrue(server.isClosed());
  }

  /**
   * Only one admin can be the initial admin.
   */

  @Test
  public void testUpdateInitialAdmin()
    throws Exception
  {
    final var server = this.serverFixture.server();
    server.close();
    assertTrue(server.isClosed());

    server.start();
    assertFalse(server.isClosed());

    final var adminId =
      UUID.randomUUID();
    final var otherAdminId =
      UUID.randomUUID();

    assertNotEquals(adminId, otherAdminId);

    server.createOrUpdateInitialAdmin(
      adminId,
      new IdName("x"),
      new IdEmail("x@example.com"),
      new IdRealName("Ex"),
      "12345678"
    );

    server.close();
    assertTrue(server.isClosed());

    server.start();
    assertFalse(server.isClosed());

    assertThrows(IdServerException.class, () -> {
      server.createOrUpdateInitialAdmin(
        otherAdminId,
        new IdName("x"),
        new IdEmail("x@example.com"),
        new IdRealName("Ex"),
        "12345678"
      );
    });
  }
}

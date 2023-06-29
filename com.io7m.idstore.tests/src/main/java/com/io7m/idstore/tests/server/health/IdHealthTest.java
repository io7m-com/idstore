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

package com.io7m.idstore.tests.server.health;

import com.io7m.ervilla.api.EContainerSupervisorType;
import com.io7m.ervilla.test_extension.ErvillaCloseAfterAll;
import com.io7m.ervilla.test_extension.ErvillaConfiguration;
import com.io7m.ervilla.test_extension.ErvillaExtension;
import com.io7m.idstore.tests.extensions.IdTestDatabases;
import com.io7m.idstore.tests.extensions.IdTestServers;
import com.io7m.zelador.test_extension.CloseableResourcesType;
import com.io7m.zelador.test_extension.ZeladorExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({ErvillaExtension.class, ZeladorExtension.class})
@ErvillaConfiguration(disabledIfUnsupported = true)
public final class IdHealthTest
{
  private IdTestServers.IdTestServerFixture serverFixture;
  private static IdTestDatabases.IdDatabaseFixture DATABASE_FIXTURE;

  @BeforeAll
  public static void setupOnce(
    final @ErvillaCloseAfterAll EContainerSupervisorType containers)
    throws Exception
  {
    DATABASE_FIXTURE =
      IdTestDatabases.create(containers, 15432);
  }

  @BeforeEach
  public void setup(
    final CloseableResourcesType closeables)
    throws Exception
  {
    this.serverFixture =
      closeables.addPerTestResource(
        IdTestServers.create(
          DATABASE_FIXTURE,
          10025,
          50000,
          50001,
          51000
        ));
  }

  @Test
  public void testHealthAdminOK()
    throws Exception
  {
    final var client =
      HttpClient.newHttpClient();
    final var healthURI =
      this.serverFixture.server()
        .adminAPI()
        .resolve("health");

    final var response =
      client.send(
        HttpRequest.newBuilder(healthURI)
          .build(),
        HttpResponse.BodyHandlers.ofString()
      );

    assertEquals("OK", response.body());
  }

  @Test
  public void testHealthUserOK()
    throws Exception
  {
    final var client =
      HttpClient.newHttpClient();
    final var healthURI =
      this.serverFixture.server()
        .adminAPI()
        .resolve("health");

    final var response =
      client.send(
        HttpRequest.newBuilder(healthURI)
          .build(),
        HttpResponse.BodyHandlers.ofString()
      );

    assertEquals("OK", response.body());
  }
}

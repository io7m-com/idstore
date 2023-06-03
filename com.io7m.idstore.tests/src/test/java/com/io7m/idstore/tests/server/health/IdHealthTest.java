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

import com.io7m.idstore.server.api.IdServerType;
import com.io7m.idstore.tests.extensions.IdTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers(disabledWithoutDocker = true)
@ExtendWith(IdTestExtension.class)
public final class IdHealthTest
{
  @Test
  public void testHealthAdminOK(
    final IdServerType server)
    throws Exception
  {
    final var client =
      HttpClient.newHttpClient();
    final var healthURI =
      server.adminAPI()
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
  public void testHealthUserOK(
    final IdServerType server)
    throws Exception
  {
    final var client =
      HttpClient.newHttpClient();
    final var healthURI =
      server.userAPI()
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

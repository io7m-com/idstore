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


package com.io7m.idstore.tests.server;

import com.io7m.idstore.database.api.IdDatabaseConfiguration;
import com.io7m.idstore.database.postgres.IdDatabases;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.server.api.IdServerBrandingConfiguration;
import com.io7m.idstore.server.api.IdServerConfiguration;
import com.io7m.idstore.server.api.IdServerException;
import com.io7m.idstore.server.api.IdServerHTTPServiceConfiguration;
import com.io7m.idstore.server.api.IdServerHistoryConfiguration;
import com.io7m.idstore.server.api.IdServerMailConfiguration;
import com.io7m.idstore.server.api.IdServerMailTransportSMTP;
import com.io7m.idstore.server.api.IdServerRateLimitConfiguration;
import com.io7m.idstore.server.api.IdServerSessionConfiguration;
import com.io7m.idstore.server.api.IdServerType;
import com.io7m.idstore.server.vanilla.IdServers;
import com.io7m.idstore.tests.database.IdDatabaseExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
@ExtendWith(IdDatabaseExtension.class)
public final class IdServerTest
{
  private IdServers servers;

  @BeforeEach
  public void setup()
  {
    this.servers = new IdServers();
  }

  /**
   * Check that starting and stopping the server does not leak threads.
   */

  @Test
  public void testStartStop(
    final IdDatabaseConfiguration databaseConfiguration)
    throws IdServerException
  {
    final var server =
      this.createServer(databaseConfiguration);

    server.start();
    server.close();
    server.setup(
      Optional.empty(),
      new IdName("x"),
      new IdEmail("x@example.com"),
      new IdRealName("Ex"),
      "12345678"
    );
    server.start();
    server.close();

    final var threadNames =
      Thread.getAllStackTraces()
        .keySet()
        .stream()
        .map(Thread::getName)
        .collect(Collectors.toUnmodifiableSet());

    for (final var name : threadNames) {
      System.out.printf("threadNames: %s%n", name);
    }

    final var hikari =
      threadNames.stream()
        .filter(name -> name.contains("HikariPool"))
        .count();

    assertTrue(hikari <= 2L);
  }

  private IdServerType createServer(
    final IdDatabaseConfiguration databaseConfiguration)
  {
    final var mailService =
      new IdServerMailConfiguration(
        new IdServerMailTransportSMTP("localhost", 25000),
        Optional.empty(),
        "no-reply@example.com",
        Duration.ofDays(1L)
      );

    final var userApiService =
      new IdServerHTTPServiceConfiguration(
        "localhost",
        50000,
        URI.create("http://localhost:50000/")
      );
    final var userViewService =
      new IdServerHTTPServiceConfiguration(
        "localhost",
        50001,
        URI.create("http://localhost:50001/")
      );
    final var adminApiService =
      new IdServerHTTPServiceConfiguration(
        "localhost",
        51000,
        URI.create("http://localhost:51000/")
      );

    final var branding =
      new IdServerBrandingConfiguration(
        "idstore",
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
      );

    final var history =
      new IdServerHistoryConfiguration(
        100,
        100
      );

    final var rateLimit =
      new IdServerRateLimitConfiguration(
        Duration.of(10L, ChronoUnit.MINUTES),
        Duration.of(10L, ChronoUnit.MINUTES)
      );

    final var sessions =
      new IdServerSessionConfiguration(
        Duration.of(30L, ChronoUnit.MINUTES),
        Duration.of(30L, ChronoUnit.MINUTES)
      );

    return this.servers.createServer(
      new IdServerConfiguration(
        Locale.getDefault(),
        Clock.systemUTC(),
        new IdDatabases(),
        databaseConfiguration,
        mailService,
        userApiService,
        userViewService,
        adminApiService,
        sessions,
        branding,
        history,
        rateLimit,
        Optional.empty()
      )
    );
  }
}

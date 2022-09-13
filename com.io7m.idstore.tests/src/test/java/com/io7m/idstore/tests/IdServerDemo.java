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

import com.io7m.idstore.database.api.IdDatabaseAdminsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseConfiguration;
import com.io7m.idstore.database.api.IdDatabaseCreate;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseUpgrade;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.database.postgres.IdDatabases;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.server.IdServers;
import com.io7m.idstore.server.api.IdServerBrandingConfiguration;
import com.io7m.idstore.server.api.IdServerConfiguration;
import com.io7m.idstore.server.api.IdServerHTTPServiceConfiguration;
import com.io7m.idstore.server.api.IdServerMailConfiguration;
import com.io7m.idstore.server.api.IdServerMailTransportSMTP;
import com.io7m.idstore.server.api.IdServerType;
import com.io7m.idstore.server.api.events.IdServerEventReady;
import com.io7m.idstore.server.api.events.IdServerEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.server.SMTPServer;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;

public final class IdServerDemo
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdServerDemo.class);

  private IdServerDemo()
  {

  }

  public static void main(
    final String[] args)
    throws Exception
  {
    System.setProperty("org.jooq.no-tips", "true");
    System.setProperty("org.jooq.no-logo", "true");

    final var databaseConfiguration =
      new IdDatabaseConfiguration(
        "postgres",
        "12345678",
        "localhost",
        54322,
        "postgres",
        IdDatabaseCreate.CREATE_DATABASE,
        IdDatabaseUpgrade.UPGRADE_DATABASE,
        Clock.systemUTC()
      );

    final var mailService =
      new IdServerMailConfiguration(
        new IdServerMailTransportSMTP("localhost", 25000),
        Optional.empty(),
        "no-reply@example.com",
        Duration.ofDays(1L)
      );

    final var directory =
      Files.createDirectories(Paths.get("idstore-sessions"));

    final var userApiService =
      new IdServerHTTPServiceConfiguration(
        directory.resolve("user-api"),
        "localhost",
        50000,
        URI.create("http://localhost:50000/")
      );
    final var userViewService =
      new IdServerHTTPServiceConfiguration(
        directory.resolve("user-view"),
        "localhost",
        50001,
        URI.create("http://localhost:50001/")
      );
    final var adminApiService =
      new IdServerHTTPServiceConfiguration(
        directory.resolve("admin-api"),
        "localhost",
        51000,
        URI.create("http://localhost:51000/")
      );
    final var adminViewService =
      new IdServerHTTPServiceConfiguration(
        directory.resolve("admin-view"),
        "localhost",
        51001,
        URI.create("http://localhost:51001/")
      );

    final var branding =
      new IdServerBrandingConfiguration(
        Optional.of("Lemon"),
        Optional.empty(),
        Optional.empty()
      );

    final var serverConfiguration =
      new IdServerConfiguration(
        Locale.getDefault(),
        Clock.systemUTC(),
        new IdDatabases(),
        databaseConfiguration,
        mailService,
        userApiService,
        userViewService,
        adminApiService,
        adminViewService,
        branding
      );

    final var smtp =
      SMTPServer.port(25000)
        .executorService(Executors.newSingleThreadExecutor(r -> {
          final var thread = new Thread(r);
          thread.setName("mail");
          thread.setDaemon(true);
          return thread;
        }))
        .messageHandler((messageContext, source, destination, data) -> {

        })
        .build();

    try {
      final var servers = new IdServers();
      try (var server = servers.createServer(serverConfiguration)) {
        final var watchdog = new Watchdog();
        server.events().subscribe(watchdog);

        smtp.start();
        server.start();

        for (int second = 1; second <= 10; ++second) {
          if (watchdog.isStarted()) {
            break;
          }
          if (second == 10) {
            throw new IOException("Server startup failed!");
          }
          Thread.sleep(1_000L);
        }

        createInitialAdmin(server);
        createUsers(server);

        while (true) {
          Thread.sleep(1_000L);
        }
      }
    } finally {
      smtp.stop();
    }
  }

  private static void createInitialAdmin(
    final IdServerType server)
  {
    try {
      final var db = server.database();
      try (var c = db.openConnection(IDSTORE)) {
        try (var t = c.openTransaction()) {
          final var admins =
            t.queries(IdDatabaseAdminsQueriesType.class);
          final var algo =
            IdPasswordAlgorithmPBKDF2HmacSHA256.create();
          final var password =
            algo.createHashed("12345678");

          final var adminId = UUID.randomUUID();
          admins.adminCreateInitial(
            adminId,
            new IdName("someone"),
            new IdRealName("Someone R. Incogito"),
            new IdEmail("admin@example.com"),
            OffsetDateTime.now(),
            password
          );

          t.commit();
          t.adminIdSet(adminId);

          admins.adminCreate(
            UUID.randomUUID(),
            new IdName("someone-u"),
            new IdRealName("Someone R. Incogito"),
            new IdEmail("admin-u@example.com"),
            OffsetDateTime.now(),
            password,
            Set.of()
          );

          t.commit();
        }
      }
    } catch (final IdDatabaseException | IdPasswordException e) {
      e.getMessage();
    }
  }

  private static void createUsers(
    final IdServerType server)
  {
    try {
      final var db = server.database();
      try (var c = db.openConnection(IDSTORE)) {
        try (var t = c.openTransaction()) {
          final var admins =
            t.queries(IdDatabaseAdminsQueriesType.class);

          final var admin =
            admins.adminGetForName(new IdName("someone"))
              .orElseThrow();

          final var users =
            t.queries(IdDatabaseUsersQueriesType.class);

          if (users.userGetForName(new IdName("someone-0")).isPresent()) {
            return;
          }

          final var algo =
            IdPasswordAlgorithmPBKDF2HmacSHA256.create();
          final var password =
            algo.createHashed("12345678");

          final var userSet =
            IdTestUserSet.users();

          for (final var u : userSet) {
            final var id = u.id();

            t.adminIdSet(admin.id());
            users.userCreate(
              id,
              u.idName(),
              u.realName(),
              new IdEmail("%s@example.com".formatted(u.idName())),
              OffsetDateTime.now(),
              password
            );

            t.userIdSet(id);
            users.userEmailAdd(
              id,
              new IdEmail("%s-alt0@example.com".formatted(u.idName())));
            users.userEmailAdd(
              id,
              new IdEmail("%s-alt1@example.com".formatted(u.idName())));
            users.userEmailAdd(
              id,
              new IdEmail("%s-alt2@example.com".formatted(u.idName())));
          }

          t.commit();
        }
      }
    } catch (final IdDatabaseException | IdPasswordException | IOException e) {
      e.getMessage();
    }
  }

  private static final class Watchdog
    implements Flow.Subscriber<IdServerEventType>
  {
    private Flow.Subscription subscription;
    private boolean isStarted;

    Watchdog()
    {

    }

    public boolean isStarted()
    {
      return this.isStarted;
    }

    @Override
    public void onSubscribe(
      final Flow.Subscription newSubscription)
    {
      this.subscription = newSubscription;
      newSubscription.request(1L);
    }

    @Override
    public void onNext(
      final IdServerEventType item)
    {
      if (item instanceof IdServerEventReady) {
        LOG.debug("server is ready");
        this.isStarted = true;
        this.subscription.cancel();
      } else {
        this.subscription.request(1L);
      }
    }

    @Override
    public void onError(
      final Throwable throwable)
    {

    }

    @Override
    public void onComplete()
    {

    }
  }
}

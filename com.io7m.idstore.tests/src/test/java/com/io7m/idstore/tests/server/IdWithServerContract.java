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

import com.io7m.idstore.database.api.IdDatabaseAdminsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseConfiguration;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.database.postgres.IdDatabases;
import com.io7m.idstore.model.IdAdminPermission;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPassword;
import com.io7m.idstore.model.IdPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdUser;
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
import com.io7m.idstore.tests.IdFakeClock;
import com.io7m.idstore.tests.IdTestDirectories;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.jmulticlose.core.ClosingResourceFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.server.SMTPServer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.io7m.idstore.database.api.IdDatabaseCreate.CREATE_DATABASE;
import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.database.api.IdDatabaseUpgrade.UPGRADE_DATABASE;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers(disabledWithoutDocker = true)
public abstract class IdWithServerContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdWithServerContract.class);

  @Container
  private static final PostgreSQLContainer<?> CONTAINER =
    new PostgreSQLContainer<>("postgres")
      .withDatabaseName("postgres")
      .withUsername("postgres")
      .withPassword("12345678");

  private IdCapturingDatabases databases;
  private IdServerType server;
  private IdServers servers;
  private Path directory;
  private AtomicBoolean started;
  private SMTPServer smtp;
  private ConcurrentLinkedQueue<MimeMessage> emailsReceived;
  private IdFakeClock clock;
  private CloseableCollectionType<ClosingResourceFailedException> resources;

  protected final IdFakeClock clock()
  {
    return this.clock;
  }

  private static IdPassword createBadPassword()
    throws IdPasswordException
  {
    return IdPasswordAlgorithmPBKDF2HmacSHA256.create()
      .createHashed("12345678");
  }

  protected static OffsetDateTime timeNow()
  {
    return OffsetDateTime.now(Clock.systemUTC()).withNano(0);
  }

  protected final UUID serverCreateUser(
    final UUID admin,
    final String name)
    throws IdDatabaseException, IdPasswordException
  {
    this.serverStartIfNecessary();

    final var database = this.databases.mostRecent();
    try (var connection = database.openConnection(IDSTORE)) {
      try (var transaction = connection.openTransaction()) {
        final var users =
          transaction.queries(IdDatabaseUsersQueriesType.class);
        transaction.adminIdSet(admin);

        final var userId = UUID.randomUUID();
        users.userCreate(
          userId,
          new IdName(name),
          new IdRealName(name),
          new IdEmail("%s@example.com".formatted(name)),
          timeNow(),
          createBadPassword()
        );
        transaction.commit();
        return userId;
      }
    }
  }

  public final IdServerType server()
  {
    return this.server;
  }

  protected final ConcurrentLinkedQueue<MimeMessage> emailsReceived()
  {
    return this.emailsReceived;
  }

  @BeforeEach
  public final void serverSetup()
    throws Exception
  {
    LOG.debug("serverSetup");

    this.waitForDatabaseToStart();

    this.started =
      new AtomicBoolean(false);
    this.clock =
      new IdFakeClock();
    this.directory =
      IdTestDirectories.createTempDirectory();
    this.servers =
      new IdServers();
    this.databases =
      new IdCapturingDatabases(new IdDatabases());
    this.server =
      this.createServer();
    this.emailsReceived =
      new ConcurrentLinkedQueue<>();
    this.smtp =
      SMTPServer.port(25000)
        .messageHandler((messageContext, source, destination, data) -> {
          LOG.debug(
            "received mail: {} {} {}",
            source,
            destination,
            Integer.valueOf(data.length)
          );

          try {
            final var message =
              new MimeMessage(
                Session.getDefaultInstance(new Properties()),
                new ByteArrayInputStream(data)
              );

            this.emailsReceived.add(message);
          } catch (final MessagingException e) {
            throw new IllegalStateException(e);
          }
        })
        .build();
    this.smtp.start();

    this.resources.add(this.server);
    this.resources.add(() -> {
      this.smtp.stop();
    });
  }

  private void waitForDatabaseToStart()
    throws Exception
  {
    LOG.debug("waiting for database to start");
    final var timeWait = Duration.ofSeconds(60L);
    final var timeThen = Instant.now();
    while (!CONTAINER.isRunning()) {
      Thread.sleep(1L);
      final var timeNow = Instant.now();
      if (Duration.between(timeThen, timeNow).compareTo(timeWait) > 0) {
        LOG.error("timed out waiting for database to start");
        throw new TimeoutException("Timed out waiting for database to start");
      }
    }

    this.resources = CloseableCollection.create();
    this.resources.add(() -> {
      CONTAINER.execInContainer("dropdb", "postgres");
    });

    CONTAINER.addEnv("PGPASSWORD", "12345678");

    final var r0 =
      CONTAINER.execInContainer(
        "dropdb",
        "-w",
        "-U",
        "postgres",
        "postgres"
      );
    LOG.debug("stderr: {}", r0.getStderr());

    final var r1 =
      CONTAINER.execInContainer(
        "createdb",
        "-w",
        "-U",
        "postgres",
        "postgres"
      );

    LOG.debug("stderr: {}", r0.getStderr());
    assertEquals(0, r1.getExitCode());
    LOG.debug("database started");
  }

  @AfterEach
  public final void serverTearDown()
    throws Exception
  {
    LOG.debug("serverTearDown");
    this.resources.close();
  }

  private IdServerType createServer()
  {
    LOG.debug("creating server");

    final var databaseConfiguration =
      new IdDatabaseConfiguration(
        "postgres",
        "12345678",
        CONTAINER.getContainerIpAddress(),
        CONTAINER.getFirstMappedPort().intValue(),
        "postgres",
        CREATE_DATABASE,
        UPGRADE_DATABASE,
        this.clock
      );

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
        this.clock,
        this.databases,
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

  protected final URI serverUserAPIURL()
  {
    return URI.create("http://localhost:50000/");
  }

  protected final URI serverUserViewURL()
  {
    return URI.create("http://localhost:50001/");
  }

  protected final URI serverAdminAPIURL()
  {
    return URI.create("http://localhost:51000/");
  }

  protected final URI serverAdminViewURL()
  {
    return URI.create("http://localhost:51001/");
  }

  protected final UUID serverCreateAdminInitial(
    final String user,
    final String pass)
    throws Exception
  {
    this.serverStartIfNecessary();

    final var database = this.databases.mostRecent();
    try (var c = database.openConnection(IDSTORE)) {
      try (var t = c.openTransaction()) {
        final var q =
          t.queries(IdDatabaseAdminsQueriesType.class);

        final var password =
          IdPasswordAlgorithmPBKDF2HmacSHA256.create()
            .createHashed(pass);

        final var id = UUID.randomUUID();
        q.adminCreateInitial(
          id,
          new IdName(user),
          new IdRealName(user),
          new IdEmail(id + "@example.com"),
          OffsetDateTime.now(),
          password
        );
        t.commit();
        return id;
      }
    }
  }

  protected final UUID serverCreateAdmin(
    final UUID admin,
    final String name)
    throws IdDatabaseException, IdPasswordException
  {
    this.serverStartIfNecessary();

    final var database = this.databases.mostRecent();
    try (var c = database.openConnection(IDSTORE)) {
      try (var t = c.openTransaction()) {
        final var q =
          t.queries(IdDatabaseAdminsQueriesType.class);

        t.adminIdSet(admin);

        final var password =
          IdPasswordAlgorithmPBKDF2HmacSHA256.create()
            .createHashed("12345678");

        final var id = UUID.randomUUID();
        q.adminCreate(
          id,
          new IdName(name),
          new IdRealName(name),
          new IdEmail("extra_%s@example.com".formatted(name)),
          OffsetDateTime.now(),
          password,
          EnumSet.allOf(IdAdminPermission.class)
        );
        t.commit();
        return id;
      }
    }
  }

  protected final void serverStartIfNecessary()
  {
    if (this.started.compareAndSet(false, true)) {
      try {
        this.server.start();
      } catch (final IdServerException e) {
        this.started.set(false);
        throw new IllegalStateException(e);
      }
    }
  }

  protected final IdUser userGet(
    final UUID id)
    throws IdDatabaseException
  {
    final var database = this.databases.mostRecent();
    try (var connection = database.openConnection(IDSTORE)) {
      try (var transaction = connection.openTransaction()) {
        final var users =
          transaction.queries(IdDatabaseUsersQueriesType.class);

        return users.userGetRequire(id);
      }
    }
  }
}

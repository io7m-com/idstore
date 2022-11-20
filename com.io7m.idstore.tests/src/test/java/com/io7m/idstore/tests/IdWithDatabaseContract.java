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
import com.io7m.idstore.database.api.IdDatabaseAuditQueriesType;
import com.io7m.idstore.database.api.IdDatabaseConfiguration;
import com.io7m.idstore.database.api.IdDatabaseConnectionType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseRole;
import com.io7m.idstore.database.api.IdDatabaseTransactionType;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.database.postgres.IdDatabases;
import com.io7m.idstore.model.IdAuditSearchParameters;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPassword;
import com.io7m.idstore.model.IdPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdTimeRange;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.jmulticlose.core.ClosingResourceFailedException;
import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.idstore.database.api.IdDatabaseCreate.CREATE_DATABASE;
import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.database.api.IdDatabaseUpgrade.UPGRADE_DATABASE;
import static java.lang.Integer.MAX_VALUE;
import static java.time.OffsetDateTime.now;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers(disabledWithoutDocker = true)
public abstract class IdWithDatabaseContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdWithDatabaseContract.class);

  protected static final IdDatabases DATABASES =
    new IdDatabases();

  @Container
  private static final PostgreSQLContainer<?> CONTAINER =
    new PostgreSQLContainer<>(DockerImageName.parse("postgres").withTag("14.4"))
      .withDatabaseName("postgres")
      .withUsername("postgres")
      .withPassword("12345678");

  private CloseableCollectionType<ClosingResourceFailedException> resources;
  private IdDatabaseType database;
  private IdDatabaseConfiguration configuration;

  protected static void checkAuditLog(
    final IdDatabaseTransactionType transaction,
    final ExpectedEvent... expectedEvents)
    throws IdDatabaseException
  {
    final var audit =
      transaction.queries(IdDatabaseAuditQueriesType.class);
    final var events =
      audit.auditEventsSearch(
        new IdAuditSearchParameters(
          new IdTimeRange(timeNow().minusYears(1L), timeNow().plusYears(1L)),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          MAX_VALUE
        )
      ).pageCurrent(audit).items();

    for (var index = 0; index < expectedEvents.length; ++index) {
      final var event =
        events.get(index);
      final var expect =
        expectedEvents[index];

      assertEquals(
        expect.type,
        event.type(),
        String.format(
          "Event [%d] %s type must be %s",
          Integer.valueOf(index),
          event,
          expect.type)
      );

      if (expect.message != null) {
        assertEquals(
          expect.message,
          event.message(),
          String.format(
            "Event [%d] %s message must be %s",
            Integer.valueOf(index),
            event,
            expect.message)
        );
      }
    }
  }

  protected static OffsetDateTime timeNow()
  {
    /*
     * Postgres doesn't store times at as high a resolution as the JVM,
     * so trim the nanoseconds off in order to ensure we can correctly
     * compare results returned from the database.
     */

    return now().withNano(0);
  }

  protected static IdPassword databaseGenerateBadPassword()
    throws IdPasswordException
  {
    return IdPasswordAlgorithmPBKDF2HmacSHA256.create()
      .createHashed("12345678");
  }

  protected static IdPassword databaseGenerateDifferentBadPassword()
    throws IdPasswordException
  {
    return IdPasswordAlgorithmPBKDF2HmacSHA256.create()
      .createHashed("abcdefgh");
  }

  @BeforeEach
  public final void withDatabaseSetup()
    throws Exception
  {
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
  }

  @AfterEach
  public final void withDatabaseTearDown()
    throws ClosingResourceFailedException
  {
    this.resources.close();
  }

  public final IdDatabaseType database()
  {
    return this.database;
  }

  protected final boolean containerIsRunning()
  {
    return CONTAINER.isRunning();
  }

  private IdDatabaseType databaseOf(
    final PostgreSQLContainer<?> container)
    throws IdDatabaseException
  {
    this.configuration =
      new IdDatabaseConfiguration(
      "postgres",
      "12345678",
      container.getContainerIpAddress(),
      container.getFirstMappedPort().intValue(),
      "postgres",
      CREATE_DATABASE,
      UPGRADE_DATABASE,
      Clock.systemUTC()
    );

    return this.resources.add(
      DATABASES.open(
        this.configuration,
        OpenTelemetry.noop(),
        message -> {

        }
      ));
  }

  protected final IdDatabaseTransactionType transactionOf(
    final IdDatabaseRole role)
    throws IdDatabaseException
  {
    final var connection =
      this.connectionOf(role);
    return this.resources.add(connection.openTransaction());
  }

  protected final IdDatabaseConnectionType connectionOf(
    final IdDatabaseRole role)
    throws IdDatabaseException
  {
    if (this.database == null) {
      this.database = this.databaseOf(this.CONTAINER);
    }

    final var connection =
      this.resources.add(this.database.openConnection(role));
    return connection;
  }

  protected final UUID databaseCreateUserInitial(
    final UUID adminId,
    final String user,
    final String pass)
    throws Exception
  {
    try (var t = this.transactionOf(IDSTORE)) {
      final var users =
        t.queries(IdDatabaseUsersQueriesType.class);

      final var password =
        IdPasswordAlgorithmPBKDF2HmacSHA256.create()
          .createHashed(pass);

      t.adminIdSet(adminId);

      final var id = UUID.randomUUID();
      users.userCreate(
        id,
        new IdName(user),
        new IdRealName(user),
        new IdEmail(id + "@example.com"),
        now(),
        password
      );
      t.commit();
      return id;
    }
  }

  protected final UUID databaseCreateAdminInitial(
    final String user,
    final String pass)
    throws Exception
  {
    try (var t = this.transactionOf(IDSTORE)) {
      final var admins =
        t.queries(IdDatabaseAdminsQueriesType.class);

      final var password =
        IdPasswordAlgorithmPBKDF2HmacSHA256.create()
          .createHashed(pass);

      final var id = UUID.randomUUID();
      admins.adminCreateInitial(
        id,
        new IdName(user),
        new IdRealName("%s Real Name".formatted(user)),
        new IdEmail(UUID.randomUUID() + "@example.com"),
        now(),
        password
      );
      t.commit();
      return id;
    }
  }

  protected final IdDatabaseConfiguration databaseConfiguration()
  {
    return this.configuration;
  }

  protected record ExpectedEvent(
    String type,
    String message)
  {

  }
}

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


package com.io7m.idstore.tests.database;

import com.io7m.idstore.database.api.IdDatabaseAdminsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseAuditQueriesType;
import com.io7m.idstore.database.api.IdDatabaseConfiguration;
import com.io7m.idstore.database.api.IdDatabaseCreate;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseTransactionType;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.database.api.IdDatabaseUpgrade;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.database.postgres.IdDatabases;
import com.io7m.idstore.model.IdAuditEvent;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static java.lang.Integer.MAX_VALUE;
import static java.time.OffsetDateTime.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

public final class IdDatabaseExtension
  implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback,
  ExtensionContext.Store.CloseableResource,
  ParameterResolver
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdDatabaseExtension.class);

  private static final IdDatabases DATABASES =
    new IdDatabases();

  private static final PostgreSQLContainer<?> CONTAINER =
    new PostgreSQLContainer<>(
      DockerImageName.parse("postgres")
        .withTag("14.4"))
      .withDatabaseName("idstore")
      .withUsername("postgres")
      .withPassword("12345678");

  private CloseableCollectionType<ClosingResourceFailedException> resources;
  private CloseableCollectionType<ClosingResourceFailedException> perTestResources;
  private boolean started;
  private IdDatabaseType database;
  private IdDatabaseConfiguration databaseConfiguration;

  public IdDatabaseExtension()
  {
    this.resources =
      CloseableCollection.create();
    this.perTestResources =
      CloseableCollection.create();
  }

  @Override
  public void beforeAll(
    final ExtensionContext context)
    throws Exception
  {
    if (!this.started) {
      this.started = true;

      context.getRoot()
        .getStore(GLOBAL)
        .put("com.io7m.idstore.tests.database.IdDatabaseExtension", this);

      CONTAINER.start();
      CONTAINER.addEnv("PGPASSWORD", "12345678");
    }
  }

  @Override
  public void close()
    throws Throwable
  {
    LOG.debug("tearing down database container");
    this.resources.close();
  }

  @Override
  public void beforeEach(
    final ExtensionContext context)
    throws Exception
  {
    LOG.debug("setting up database");

    this.resources = CloseableCollection.create();
    this.resources.add(CONTAINER::stop);

    final var r0 =
      CONTAINER.execInContainer(
        "dropdb",
        "-w",
        "-U",
        "postgres",
        "eigion"
      );
    LOG.debug("stderr: {}", r0.getStderr());

    final var r1 =
      CONTAINER.execInContainer(
        "createdb",
        "-w",
        "-U",
        "postgres",
        "eigion"
      );

    LOG.debug("stderr: {}", r0.getStderr());
    assertEquals(0, r1.getExitCode());

    this.databaseConfiguration =
      new IdDatabaseConfiguration(
        "postgres",
        "12345678",
        CONTAINER.getContainerIpAddress(),
        CONTAINER.getFirstMappedPort().intValue(),
        "eigion",
        IdDatabaseCreate.CREATE_DATABASE,
        IdDatabaseUpgrade.UPGRADE_DATABASE,
        Clock.systemUTC()
      );

    this.perTestResources = CloseableCollection.create();
    this.database =
      this.perTestResources.add(DATABASES.open(
        this.databaseConfiguration,
        OpenTelemetry.noop(),
        message -> {

        }
      ));
  }

  @Override
  public void afterEach(
    final ExtensionContext context)
    throws Exception
  {
    LOG.debug("tearing down database");
    this.perTestResources.close();
  }

  @Override
  public boolean supportsParameter(
    final ParameterContext parameterContext,
    final ExtensionContext extensionContext)
    throws ParameterResolutionException
  {
    final var requiredType =
      parameterContext.getParameter().getType();

    return Objects.equals(requiredType, IdDatabaseType.class)
           || Objects.equals(requiredType, IdDatabaseTransactionType.class)
           || Objects.equals(requiredType, IdDatabaseConfiguration.class);
  }

  @Override
  public Object resolveParameter(
    final ParameterContext parameterContext,
    final ExtensionContext extensionContext)
    throws ParameterResolutionException
  {
    try {
      final var requiredType =
        parameterContext.getParameter().getType();

      if (Objects.equals(requiredType, IdDatabaseType.class)) {
        return this.database;
      }

      if (Objects.equals(requiredType, IdDatabaseConfiguration.class)) {
        return this.databaseConfiguration;
      }

      if (Objects.equals(requiredType, IdDatabaseTransactionType.class)) {
        final var connection =
          this.perTestResources.add(
            this.database.openConnection(IDSTORE));
        final var transaction =
          this.perTestResources.add(connection.openTransaction());
        return transaction;
      }

      throw new IllegalStateException(
        "Unrecognized requested parameter type: %s".formatted(requiredType)
      );
    } catch (final IdDatabaseException e) {
      throw new ParameterResolutionException(e.getMessage(), e);
    }
  }

  public static UUID databaseCreateAdminInitial(
    final IdDatabaseTransactionType t,
    final String user,
    final String pass)
    throws Exception
  {
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

  public static UUID databaseCreateUserInitial(
    final IdDatabaseTransactionType t,
    final UUID adminId,
    final String user,
    final String pass)
    throws Exception
  {
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

  public record ExpectedEvent(
    String type,
    String message)
  {

  }

  public static void checkAuditLog(
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
      final IdAuditEvent event;
      try {
        event = events.get(index);
      } catch (final IndexOutOfBoundsException e) {
        Assertions.fail(
          String.format(
            "Expected an event %s at index %d, but no such event existed.",
            expectedEvents[index],
            Integer.valueOf(index)
          )
        );
        throw e;
      }

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

  public static IdPassword databaseGenerateBadPassword()
    throws IdPasswordException
  {
    return IdPasswordAlgorithmPBKDF2HmacSHA256.create()
      .createHashed("12345678");
  }

  public static IdPassword databaseGenerateDifferentBadPassword()
    throws IdPasswordException
  {
    return IdPasswordAlgorithmPBKDF2HmacSHA256.create()
      .createHashed("abcdefgh");
  }

  public static OffsetDateTime timeNow()
  {
    /*
     * Postgres doesn't store times at as high a resolution as the JVM,
     * so trim the nanoseconds off in order to ensure we can correctly
     * compare results returned from the database.
     */

    return now().withNano(0);
  }
}

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


package com.io7m.idstore.tests.extensions;

import com.io7m.ervilla.api.EContainerSupervisorType;
import com.io7m.ervilla.api.EContainerType;
import com.io7m.ervilla.api.EPortAddressType;
import com.io7m.ervilla.postgres.EPgSpecs;
import com.io7m.idstore.database.api.IdDatabaseAdminsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseAuditQueriesType;
import com.io7m.idstore.database.api.IdDatabaseConfiguration;
import com.io7m.idstore.database.api.IdDatabaseCreate;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseTelemetry;
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
import com.io7m.idstore.strings.IdStrings;
import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.lang.Integer.MAX_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Functions to construct test databases.
 */

public final class IdTestDatabases
{
  private static final IdDatabases DATABASES =
    new IdDatabases();

  /**
   * The version of PostgreSQL against which we test.
   */

  public static final String POSTGRESQL_VERSION =
    "15.2";

  private IdTestDatabases()
  {

  }

  /**
   * The basic database fixture.
   *
   * @param databaseConfiguration The database configuration
   * @param container             The database container
   */

  public record IdDatabaseFixture(
    IdDatabaseConfiguration databaseConfiguration,
    EContainerType container)
  {
    /**
     * Create a database from this container and configuration.
     *
     * @return A new database
     *
     * @throws IdDatabaseException On errors
     */

    public IdDatabaseType createDatabase()
      throws IdDatabaseException
    {
      return DATABASES.open(
        this.databaseConfiguration,
        new IdDatabaseTelemetry(
          true,
          OpenTelemetry.noop().getMeter("x"),
          OpenTelemetry.noop().getTracer("x")
        ),
        message -> {

        });

    }

    /**
     * Reset the container by dropping and recreating the database. This
     * is significantly faster than destroying and recreating the container.
     *
     * @throws IOException          On errors
     * @throws InterruptedException On interruption
     */

    public void reset()
      throws IOException, InterruptedException
    {
      assertEquals(
        0,
        this.container.executeAndWaitIndefinitely(
          List.of(
            "dropdb",
            "-w",
            "-f",
            "-U",
            "idstore_install",
            "idstore"
          )
        ));

      assertEquals(
        0,
        this.container.executeAndWaitIndefinitely(
          List.of(
            "createdb",
            "-w",
            "-U",
            "idstore_install",
            "idstore"
          )
        )
      );
    }
  }

  /**
   * Create a new database fixture.
   *
   * @param supervisor The container supervisor
   * @param port       The database port
   *
   * @return A new database fixture
   *
   * @throws Exception On errors
   */

  public static IdDatabaseFixture create(
    final EContainerSupervisorType supervisor,
    final int port)
    throws Exception
  {
    final var container =
      supervisor.start(
        EPgSpecs.builderFromDockerIO(
          POSTGRESQL_VERSION,
          new EPortAddressType.All(),
          port,
          "idstore",
          "idstore_install",
          "12345678"
        ).build()
      );

    final var databaseConfiguration =
      new IdDatabaseConfiguration(
        "idstore_install",
        "12345678",
        "12345678",
        Optional.of("12345678"),
        "127.0.0.1",
        port,
        "idstore",
        IdDatabaseCreate.CREATE_DATABASE,
        IdDatabaseUpgrade.UPGRADE_DATABASE,
        IdStrings.create(Locale.ROOT),
        Clock.systemUTC()
      );


    return new IdDatabaseFixture(databaseConfiguration, container);
  }

  /**
   * Create a new database fixture.
   *
   * @param supervisor The container supervisor
   * @param port       The database port
   *
   * @return A new database fixture
   *
   * @throws Exception On errors
   */

  public static IdDatabaseFixture createWithHostilePasswords(
    final EContainerSupervisorType supervisor,
    final int port)
    throws Exception
  {
    final var ownerRolePassword = "''\\'1";
    final var workerRolePassword = "''\\'2";
    final var readerRolePassword = "''\\'3";

    final var container =
      supervisor.start(
        EPgSpecs.builderFromDockerIO(
          POSTGRESQL_VERSION,
          new EPortAddressType.All(),
          port,
          "idstore",
          "idstore_install",
          ownerRolePassword
        ).build()
      );

    final var databaseConfiguration =
      new IdDatabaseConfiguration(
        "idstore_install",
        ownerRolePassword,
        workerRolePassword,
        Optional.of(readerRolePassword),
        "127.0.0.1",
        port,
        "idstore",
        IdDatabaseCreate.CREATE_DATABASE,
        IdDatabaseUpgrade.UPGRADE_DATABASE,
        IdStrings.create(Locale.ROOT),
        Clock.systemUTC()
      );


    return new IdDatabaseFixture(databaseConfiguration, container);
  }

  /**
   * Set the initial admin.
   *
   * @param t    The transaction
   * @param user The username
   * @param pass The password
   *
   * @return The ID of the new admin
   *
   * @throws Exception On errors
   */

  public static UUID createAdminInitial(
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
      OffsetDateTime.now(),
      password
    );
    t.commit();
    return id;
  }

  /**
   * Create a user.
   *
   * @param t       The transaction
   * @param adminId The admin ID
   * @param user    The username
   * @param pass    The password
   *
   * @return The ID of the new user
   *
   * @throws Exception On errors
   */

  public static UUID createUser(
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
      OffsetDateTime.now(),
      password
    );
    t.commit();
    return id;
  }

  /**
   * An expected audit event.
   *
   * @param type    The event type
   * @param message The event message
   */

  public record ExpectedEvent(
    String type,
    Map<String, String> message)
  {
    /**
     * An expected audit event.
     *
     * @param type    The event type
     * @param entries The event entries
     *
     * @return A new event
     */

    public static ExpectedEvent eventOf(
      final String type,
      final Map.Entry<?, ?>... entries)
    {
      final var map = new HashMap<String, String>(entries.length);
      for (final var e : entries) {
        map.put(e.getKey().toString(), e.getValue().toString());
      }
      return new ExpectedEvent(
        type,
        Map.copyOf(map)
      );
    }
  }

  /**
   * Check the audit log contains all the given events.
   *
   * @param transaction    The transaction
   * @param expectedEvents The expected events
   *
   * @throws IdDatabaseException On errors
   */

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
          event.data(),
          String.format(
            "Event [%d] %s message must be %s",
            Integer.valueOf(index),
            event,
            expect.message)
        );
      }
    }
  }

  /**
   * Generate a bad hashed password ("12345678").
   *
   * @return A hashed password
   *
   * @throws IdPasswordException On errors
   */

  public static IdPassword generateBadPassword()
    throws IdPasswordException
  {
    return IdPasswordAlgorithmPBKDF2HmacSHA256.create()
      .createHashed("12345678");
  }

  /**
   * Generate a bad hashed password ("abcdefgh").
   *
   * @return A hashed password
   *
   * @throws IdPasswordException On errors
   */

  public static IdPassword generateDifferentBadPassword()
    throws IdPasswordException
  {
    return IdPasswordAlgorithmPBKDF2HmacSHA256.create()
      .createHashed("abcdefgh");
  }

  /**
   * <p> Get the current time.</p>
   *
   * <p> Postgres doesn't store times at as high a resolution as the JVM,
   * so trim the nanoseconds off in order to ensure we can correctly
   * compare results returned from the database.</p>
   *
   * @return The current time
   */

  public static OffsetDateTime timeNow()
  {
    return OffsetDateTime.now().withNano(0);
  }
}

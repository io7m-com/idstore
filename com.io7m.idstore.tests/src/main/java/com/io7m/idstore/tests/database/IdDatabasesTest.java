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

import com.io7m.ervilla.api.EContainerSupervisorType;
import com.io7m.ervilla.test_extension.ErvillaCloseAfterAll;
import com.io7m.ervilla.test_extension.ErvillaConfiguration;
import com.io7m.ervilla.test_extension.ErvillaExtension;
import com.io7m.idstore.database.api.IdDatabaseConfiguration;
import com.io7m.idstore.database.api.IdDatabaseCreate;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseTelemetry;
import com.io7m.idstore.database.api.IdDatabaseUpgrade;
import com.io7m.idstore.database.postgres.IdDatabases;
import com.io7m.idstore.model.IdVersion;
import com.io7m.idstore.strings.IdStrings;
import com.io7m.idstore.tests.extensions.IdTestDatabases;
import com.io7m.zelador.test_extension.ZeladorExtension;
import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.postgresql.ds.PGConnectionPoolDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({ErvillaExtension.class, ZeladorExtension.class})
@ErvillaConfiguration(disabledIfUnsupported = true)
public final class IdDatabasesTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdDatabasesTest.class);

  private static IdTestDatabases.IdDatabaseFixture DATABASE_FIXTURE;
  private IdDatabaseConfiguration databaseConfiguration;
  private IdDatabaseConfiguration databaseConfigurationWithoutUpgrades;
  private IdDatabases databases;
  private PGConnectionPoolDataSource dataSource;

  @BeforeAll
  public static void setupOnce(
    final @ErvillaCloseAfterAll EContainerSupervisorType supervisor)
    throws Exception
  {
    DATABASE_FIXTURE =
      IdTestDatabases.create(supervisor, 15432);
  }

  @BeforeEach
  public void setup()
    throws Exception
  {
    DATABASE_FIXTURE.reset();

    this.databaseConfiguration =
      DATABASE_FIXTURE.databaseConfiguration();

    this.databaseConfigurationWithoutUpgrades =
      new IdDatabaseConfiguration(
        "idstore_install",
        "12345678",
        "12345678",
        Optional.of("12345678"),
        "127.0.0.1",
        DATABASE_FIXTURE.databaseConfiguration().port(),
        "idstore",
        IdDatabaseCreate.CREATE_DATABASE,
        IdDatabaseUpgrade.DO_NOT_UPGRADE_DATABASE,
        IdStrings.create(Locale.ROOT),
        Clock.systemUTC()
      );

    final var url = new StringBuilder(128);
    url.append("jdbc:postgresql://");
    url.append(this.databaseConfiguration.address());
    url.append(":");
    url.append(this.databaseConfiguration.port());
    url.append("/");
    url.append(this.databaseConfiguration.databaseName());

    this.dataSource = new PGConnectionPoolDataSource();
    this.dataSource.setUrl(url.toString());
    this.dataSource.setUser(this.databaseConfiguration.ownerRoleName());
    this.dataSource.setPassword(this.databaseConfiguration.ownerRolePassword());
    this.dataSource.setDefaultAutoCommit(false);
    this.databases = new IdDatabases();
  }

  /**
   * The database cannot be opened if it has the wrong application ID.
   *
   * @throws Exception On errors
   */

  @Test
  public void testWrongApplicationId()
    throws Exception
  {
    try (var connection = this.dataSource.getConnection()) {
      try (var st = connection.prepareStatement(
        """
                    create table schema_version (
                      version_lock            char(1) not null default 'X',
                      version_application_id  text    not null,
                      version_number          bigint  not null,

                      constraint check_lock_primary primary key (version_lock),
                      constraint check_lock_locked check (version_lock = 'X')
                    )
          """)) {
        st.execute();
      }
      try (var st = connection.prepareStatement(
        """
                    insert into schema_version (version_application_id, version_number) values (?, ?)
          """)) {
        st.setString(1, "com.io7m.something_else");
        st.setLong(2, 0L);
        st.execute();
      }
    }

    final var telemetry =
      new IdDatabaseTelemetry(
        true,
        OpenTelemetry.noop()
          .getMeter("com.io7m.idstore"),
        OpenTelemetry.noop()
          .getTracer("com.io7m.idstore", IdVersion.MAIN_VERSION)
      );

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        this.databases.open(
          this.databaseConfiguration,
          telemetry,
          s -> {
          }
        );
      });

    LOG.debug("message: {}", ex.getMessage());
    assertTrue(ex.getMessage().contains("com.io7m.something_else"));
  }

  /**
   * The database cannot be opened if it has the wrong version.
   *
   * @throws Exception On errors
   */

  @Test
  public void testWrongVersion()
    throws Exception
  {
    try (var connection = this.dataSource.getConnection()) {
      try (var st = connection.prepareStatement(
        """
                    create table schema_version (
                      version_lock            char(1) not null default 'X',
                      version_application_id  text    not null,
                      version_number          bigint  not null,

                      constraint check_lock_primary primary key (version_lock),
                      constraint check_lock_locked check (version_lock = 'X')
                    )
          """)) {
        st.execute();
      }
      try (var st = connection.prepareStatement(
        """
                    insert into schema_version (version_application_id, version_number) values (?, ?)
          """)) {
        st.setString(1, "com.io7m.idstore");
        st.setLong(2, (long) Integer.MAX_VALUE);
        st.execute();
      }
    }

    final var telemetry =
      new IdDatabaseTelemetry(
        true,
        OpenTelemetry.noop()
          .getMeter("com.io7m.idstore"),
        OpenTelemetry.noop()
          .getTracer("com.io7m.idstore", IdVersion.MAIN_VERSION)
      );

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        this.databases.open(
          this.databaseConfiguration,
          telemetry,
          s -> {
          }
        );
      });

    LOG.debug("message: {}", ex.getMessage());
    assertTrue(ex.getMessage().contains("Database schema version is too high"));
  }

  /**
   * The database cannot be opened if the version table is malformed.
   *
   * @throws Exception On errors
   */

  @Test
  public void testInvalidVersion()
    throws Exception
  {
    try (var connection = this.dataSource.getConnection()) {
      try (var st = connection.prepareStatement(
        """
                    create table schema_version (
                      version_application_id  text    not null,
                      version_number          bigint  not null
                    )
          """)) {
        st.execute();
      }
    }

    final var telemetry =
      new IdDatabaseTelemetry(
        true,
        OpenTelemetry.noop()
          .getMeter("com.io7m.idstore"),
        OpenTelemetry.noop()
          .getTracer("com.io7m.idstore", IdVersion.MAIN_VERSION)
      );

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        this.databases.open(
          this.databaseConfiguration,
          telemetry,
          s -> {
          }
        );
      });

    LOG.debug("message: {}", ex.getMessage());
    assertTrue(ex.getMessage().contains("schema_version table is empty"));
  }

  /**
   * The database cannot be opened if the version is too old, and upgrading
   * is not allowed.
   *
   * @throws Exception On errors
   */

  @Test
  public void testTooOld()
    throws Exception
  {
    try (var connection = this.dataSource.getConnection()) {
      try (var st = connection.prepareStatement(
        """
                    create table schema_version (
                      version_lock            char(1) not null default 'X',
                      version_application_id  text    not null,
                      version_number          bigint  not null,

                      constraint check_lock_primary primary key (version_lock),
                      constraint check_lock_locked check (version_lock = 'X')
                    )
          """)) {
        st.execute();
      }
      try (var st = connection.prepareStatement(
        """
                    insert into schema_version (version_application_id, version_number) values (?, ?)
          """)) {
        st.setString(1, "com.io7m.idstore");
        st.setLong(2, 0L);
        st.execute();
      }
    }

    final var telemetry =
      new IdDatabaseTelemetry(
        true,
        OpenTelemetry.noop()
          .getMeter("com.io7m.idstore"),
        OpenTelemetry.noop()
          .getTracer("com.io7m.idstore", IdVersion.MAIN_VERSION)
      );

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        this.databases.open(
          this.databaseConfigurationWithoutUpgrades,
          telemetry,
          s -> {
          }
        );
      });

    LOG.debug("message: {}", ex.getMessage());
    assertTrue(ex.getMessage().contains("Incompatible database schema"));
  }
}

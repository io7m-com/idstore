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

import com.io7m.idstore.database.api.IdDatabaseConfiguration;
import com.io7m.idstore.database.api.IdDatabaseCreate;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseUpgrade;
import com.io7m.idstore.database.postgres.IdDatabases;
import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGConnectionPoolDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Clock;

import static com.io7m.idstore.tests.database.IdDatabaseExtension.POSTGRES_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
public final class IdDatabasesTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdDatabasesTest.class);

  private static final PostgreSQLContainer<?> CONTAINER =
    new PostgreSQLContainer<>(
      DockerImageName.parse("postgres")
        .withTag(POSTGRES_VERSION))
      .withDatabaseName("idstore")
      .withUsername("postgres")
      .withPassword("12345678");

  private IdDatabaseConfiguration databaseConfiguration;
  private IdDatabaseConfiguration databaseConfigurationWithoutUpgrades;
  private IdDatabases databases;
  private PGConnectionPoolDataSource dataSource;

  @BeforeEach
  public void setup()
    throws Exception
  {
    CONTAINER.start();
    CONTAINER.addEnv("PGPASSWORD", "12345678");

    final var r0 =
      CONTAINER.execInContainer(
        "dropdb",
        "-w",
        "-U",
        "postgres",
        "idstore"
      );
    LOG.debug("stderr: {}", r0.getStderr());

    final var r1 =
      CONTAINER.execInContainer(
        "createdb",
        "-w",
        "-U",
        "postgres",
        "idstore"
      );

    LOG.debug("stderr: {}", r0.getStderr());
    assertEquals(0, r1.getExitCode());

    this.databaseConfiguration =
      new IdDatabaseConfiguration(
        "postgres",
        "12345678",
        CONTAINER.getHost(),
        CONTAINER.getFirstMappedPort().intValue(),
        "idstore",
        IdDatabaseCreate.CREATE_DATABASE,
        IdDatabaseUpgrade.UPGRADE_DATABASE,
        Clock.systemUTC()
      );

    this.databaseConfigurationWithoutUpgrades =
      new IdDatabaseConfiguration(
        "postgres",
        "12345678",
        CONTAINER.getHost(),
        CONTAINER.getFirstMappedPort().intValue(),
        "idstore",
        IdDatabaseCreate.CREATE_DATABASE,
        IdDatabaseUpgrade.DO_NOT_UPGRADE_DATABASE,
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
    this.dataSource.setUser(this.databaseConfiguration.user());
    this.dataSource.setPassword(this.databaseConfiguration.password());
    this.dataSource.setDefaultAutoCommit(false);

    this.databases = new IdDatabases();
  }

  @AfterEach
  public void tearDown()
  {
    CONTAINER.stop();
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

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        this.databases.open(
          this.databaseConfiguration,
          OpenTelemetry.noop(),
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

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        this.databases.open(
          this.databaseConfiguration,
          OpenTelemetry.noop(),
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

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        this.databases.open(
          this.databaseConfiguration,
          OpenTelemetry.noop(),
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

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        this.databases.open(
          this.databaseConfigurationWithoutUpgrades,
          OpenTelemetry.noop(),
          s -> {
          }
        );
      });

    LOG.debug("message: {}", ex.getMessage());
    assertTrue(ex.getMessage().contains("Incompatible database schema"));
  }
}

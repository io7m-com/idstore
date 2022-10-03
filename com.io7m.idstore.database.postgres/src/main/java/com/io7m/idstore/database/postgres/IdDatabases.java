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

package com.io7m.idstore.database.postgres;

import com.io7m.anethum.common.ParseException;
import com.io7m.idstore.database.api.IdDatabaseConfiguration;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseFactoryType;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.database.postgres.internal.IdDatabase;
import com.io7m.idstore.database.postgres.internal.IdDatabaseMetrics;
import com.io7m.trasco.api.TrEventExecutingSQL;
import com.io7m.trasco.api.TrEventType;
import com.io7m.trasco.api.TrEventUpgrading;
import com.io7m.trasco.api.TrException;
import com.io7m.trasco.api.TrExecutorConfiguration;
import com.io7m.trasco.api.TrSchemaRevisionSet;
import com.io7m.trasco.vanilla.TrExecutors;
import com.io7m.trasco.vanilla.TrSchemaRevisionSetParsers;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.opentelemetry.api.OpenTelemetry;
import org.postgresql.util.PSQLState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.math.BigInteger;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.IO_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SQL_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SQL_REVISION_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.TRASCO_ERROR;
import static com.io7m.trasco.api.TrExecutorUpgrade.FAIL_INSTEAD_OF_UPGRADING;
import static com.io7m.trasco.api.TrExecutorUpgrade.PERFORM_UPGRADES;
import static java.math.BigInteger.valueOf;

/**
 * The default postgres server database implementation.
 */

public final class IdDatabases implements IdDatabaseFactoryType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdDatabases.class);

  /**
   * The default postgres server database implementation.
   *
   */

  public IdDatabases()
  {

  }

  private static void schemaVersionSet(
    final BigInteger version,
    final Connection connection)
    throws SQLException
  {
    final String statementText;
    if (Objects.equals(version, BigInteger.ZERO)) {
      statementText = "insert into schema_version (version_number) values (?)";
    } else {
      statementText = "update schema_version set version_number = ?";
    }

    try (var statement = connection.prepareStatement(statementText)) {
      statement.setLong(1, version.longValueExact());
      statement.execute();
    }
  }

  private static Optional<BigInteger> schemaVersionGet(
    final Connection connection)
    throws SQLException
  {
    Objects.requireNonNull(connection, "connection");

    try {
      final var statementText = "SELECT version_number FROM schema_version";
      LOG.debug("execute: {}", statementText);

      try (var statement = connection.prepareStatement(statementText)) {
        try (var result = statement.executeQuery()) {
          if (!result.next()) {
            throw new SQLException("schema_version table is empty!");
          }
          return Optional.of(valueOf(result.getLong(1)));
        }
      }
    } catch (final SQLException e) {
      final var state = e.getSQLState();
      if (state == null) {
        throw e;
      }
      if (state.equals(PSQLState.UNDEFINED_TABLE.getState())) {
        connection.rollback();
        return Optional.empty();
      }

      throw e;
    }
  }

  @Override
  public String kind()
  {
    return "POSTGRESQL";
  }

  @Override
  public IdDatabaseType open(
    final IdDatabaseConfiguration configuration,
    final OpenTelemetry openTelemetry,
    final Consumer<String> startupMessages)
    throws IdDatabaseException
  {
    Objects.requireNonNull(configuration, "configuration");
    Objects.requireNonNull(openTelemetry, "openTelemetry");
    Objects.requireNonNull(startupMessages, "startupMessages");

    try {
      final var url = new StringBuilder(128);
      url.append("jdbc:postgresql://");
      url.append(configuration.address());
      url.append(":");
      url.append(configuration.port());
      url.append("/");
      url.append(configuration.databaseName());

      final var config = new HikariConfig();
      config.setJdbcUrl(url.toString());
      config.setUsername(configuration.user());
      config.setPassword(configuration.password());
      config.setAutoCommit(false);

      final var dataSource = new HikariDataSource(config);
      final var parsers = new TrSchemaRevisionSetParsers();

      final TrSchemaRevisionSet revisions;
      try (var stream = IdDatabases.class.getResourceAsStream(
        "/com/io7m/idstore/database/postgres/internal/database.xml")) {
        revisions = parsers.parse(URI.create("urn:source"), stream);
      }

      try (var connection = dataSource.getConnection()) {
        connection.setAutoCommit(false);

        new TrExecutors().create(
          new TrExecutorConfiguration(
            IdDatabases::schemaVersionGet,
            IdDatabases::schemaVersionSet,
            event -> publishTrEvent(startupMessages, event),
            revisions,
            switch (configuration.upgrade()) {
              case UPGRADE_DATABASE -> PERFORM_UPGRADES;
              case DO_NOT_UPGRADE_DATABASE -> FAIL_INSTEAD_OF_UPGRADING;
            },
            connection
          )
        ).execute();
        connection.commit();
      }

      final var metrics = new IdDatabaseMetrics();
      setupMetrics(metrics);

      return new IdDatabase(
        openTelemetry,
        configuration.clock(),
        dataSource,
        metrics
      );
    } catch (final IOException e) {
      throw new IdDatabaseException(e.getMessage(), e, IO_ERROR);
    } catch (final TrException e) {
      throw new IdDatabaseException(e.getMessage(), e, TRASCO_ERROR);
    } catch (final ParseException e) {
      throw new IdDatabaseException(e.getMessage(), e, SQL_REVISION_ERROR);
    } catch (final SQLException e) {
      throw new IdDatabaseException(e.getMessage(), e, SQL_ERROR);
    }
  }

  private static void setupMetrics(
    final IdDatabaseMetrics metrics)
  {
    try {
      final var server =
        ManagementFactory.getPlatformMBeanServer();
      final var objectName =
        new ObjectName("com.io7m.idstore.database.postgres:name=Metrics");

      server.registerMBean(metrics, objectName);
    } catch (final MalformedObjectNameException
                   | InstanceAlreadyExistsException
                   | MBeanRegistrationException
                   | NotCompliantMBeanException e) {
      LOG.error("unable to register metrics bean: ", e);
    }
  }

  private static void publishEvent(
    final Consumer<String> startupMessages,
    final String message)
  {
    try {
      LOG.trace("{}", message);
      startupMessages.accept(message);
    } catch (final Exception e) {
      LOG.error("ignored consumer exception: ", e);
    }
  }

  private static void publishTrEvent(
    final Consumer<String> startupMessages,
    final TrEventType event)
  {
    if (event instanceof TrEventExecutingSQL sql) {
      publishEvent(
        startupMessages,
        String.format("Executing SQL: %s", sql.statement())
      );
      return;
    }

    if (event instanceof TrEventUpgrading upgrading) {
      publishEvent(
        startupMessages,
        String.format(
          "Upgrading database from version %s -> %s",
          upgrading.fromVersion(),
          upgrading.toVersion())
      );
      return;
    }
  }
}

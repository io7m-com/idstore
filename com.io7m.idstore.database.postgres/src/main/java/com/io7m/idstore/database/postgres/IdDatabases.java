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

package com.io7m.idstore.database.postgres;

import com.io7m.anethum.api.ParsingException;
import com.io7m.idstore.database.api.IdDatabaseConfiguration;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseFactoryType;
import com.io7m.idstore.database.api.IdDatabaseTelemetry;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.database.postgres.internal.IdDatabase;
import com.io7m.jmulticlose.core.CloseableCollection;
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
import org.postgresql.util.PSQLState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
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
import static java.util.Objects.requireNonNullElse;

/**
 * The default postgres server database implementation.
 */

public final class IdDatabases implements IdDatabaseFactoryType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdDatabases.class);

  private static final String DATABASE_APPLICATION_ID =
    "com.io7m.idstore";

  /**
   * The default postgres server database implementation.
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
      statementText = "insert into schema_version (version_application_id, version_number) values (?, ?)";
      try (var statement =
             connection.prepareStatement(statementText)) {
        statement.setString(1, DATABASE_APPLICATION_ID);
        statement.setLong(2, version.longValueExact());
        statement.execute();
      }
    } else {
      statementText = "update schema_version set version_number = ?";
      try (var statement =
             connection.prepareStatement(statementText)) {
        statement.setLong(1, version.longValueExact());
        statement.execute();
      }
    }
  }

  private static Optional<BigInteger> schemaVersionGet(
    final Connection connection)
    throws SQLException
  {
    Objects.requireNonNull(connection, "connection");

    try {
      final var statementText =
        "SELECT version_application_id, version_number FROM schema_version";
      LOG.debug("execute: {}", statementText);

      try (var statement = connection.prepareStatement(statementText)) {
        try (var result = statement.executeQuery()) {
          if (!result.next()) {
            throw new SQLException("schema_version table is empty!");
          }
          final var applicationId =
            result.getString(1);
          final var version =
            result.getLong(2);

          if (!Objects.equals(applicationId, DATABASE_APPLICATION_ID)) {
            throw new SQLException(
              String.format(
                "Database application ID is %s but should be %s",
                applicationId,
                DATABASE_APPLICATION_ID
              )
            );
          }

          return Optional.of(valueOf(version));
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
    final IdDatabaseTelemetry telemetry,
    final Consumer<String> startupMessages)
    throws IdDatabaseException
  {
    Objects.requireNonNull(configuration, "configuration");
    Objects.requireNonNull(telemetry, "telemetry");
    Objects.requireNonNull(startupMessages, "startupMessages");

    final var resources = CloseableCollection.create(() -> {
      return new IdDatabaseException(
        "Closing a resource failed.",
        SQL_ERROR,
        Map.of(),
        Optional.empty()
      );
    });

    try {
      final var url = new StringBuilder(128);
      url.append("jdbc:postgresql://");
      url.append(configuration.address());
      url.append(':');
      url.append(configuration.port());
      url.append('/');
      url.append(configuration.databaseName());

      final var config = new HikariConfig();
      config.setJdbcUrl(url.toString());
      config.setUsername(configuration.user());
      config.setPassword(configuration.password());
      config.setAutoCommit(false);

      final var dataSource =
        resources.add(new HikariDataSource(config));

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

      return new IdDatabase(
        telemetry,
        configuration.clock(),
        dataSource,
        resources
      );
    } catch (final IOException e) {
      throw new IdDatabaseException(
        requireNonNullElse(e.getMessage(), e.getClass().getSimpleName()),
        e,
        IO_ERROR,
        Map.of(),
        Optional.empty()
      );
    } catch (final TrException e) {
      throw new IdDatabaseException(
        requireNonNullElse(e.getMessage(), e.getClass().getSimpleName()),
        e,
        TRASCO_ERROR,
        Map.of(),
        Optional.empty()
      );
    } catch (final ParsingException e) {
      throw new IdDatabaseException(
        requireNonNullElse(e.getMessage(), e.getClass().getSimpleName()),
        e,
        SQL_REVISION_ERROR,
        Map.of(),
        Optional.empty()
      );
    } catch (final SQLException e) {
      throw new IdDatabaseException(
        requireNonNullElse(e.getMessage(), e.getClass().getSimpleName()),
        e,
        SQL_ERROR,
        Map.of(),
        Optional.empty()
      );
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
    if (event instanceof final TrEventExecutingSQL sql) {
      publishEvent(
        startupMessages,
        String.format("Executing SQL: %s", sql.statement())
      );
      return;
    }

    if (event instanceof final TrEventUpgrading upgrading) {
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

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

package com.io7m.idstore.database.postgres.internal;

import com.io7m.idstore.database.api.IdDatabaseConnectionType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseRole;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.zaxxer.hikari.HikariDataSource;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import org.jooq.conf.RenderNameCase;
import org.jooq.conf.Settings;

import java.sql.SQLException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SQL_ERROR;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.DB_SYSTEM;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.DbSystemValues.POSTGRESQL;
import static java.util.Objects.requireNonNullElse;

/**
 * The default postgres server database implementation.
 */

public final class IdDatabase implements IdDatabaseType
{
  private final Clock clock;
  private final HikariDataSource dataSource;
  private final Settings settings;
  private final Tracer tracer;
  private final CloseableCollectionType<IdDatabaseException> resources;
  private final LongCounter transactions;
  private final LongCounter transactionCommits;
  private final LongCounter transactionRollbacks;
  private volatile long connectionTimeNow;

  /**
   * The default postgres server database implementation.
   *
   * @param inTracer     A telemetry tracer interface
   * @param meter        A telemetry meter interface
   * @param inClock      The clock
   * @param inDataSource A pooled data source
   * @param inResources  The resources to be closed
   */

  public IdDatabase(
    final Tracer inTracer,
    final Meter meter,
    final Clock inClock,
    final HikariDataSource inDataSource,
    final CloseableCollectionType<IdDatabaseException> inResources)
  {
    this.tracer =
      Objects.requireNonNull(inTracer, "tracer");
    this.resources =
      Objects.requireNonNull(inResources, "resources");
    Objects.requireNonNull(meter, "meter");

    this.clock =
      Objects.requireNonNull(inClock, "clock");
    this.dataSource =
      Objects.requireNonNull(inDataSource, "dataSource");
    this.settings =
      new Settings().withRenderNameCase(RenderNameCase.LOWER);

    this.transactions =
      meter.counterBuilder("idstore_db_transactions")
        .setDescription("The number of completed transactions.")
        .build();
    this.transactionCommits =
      meter.counterBuilder("idstore_db_commits")
        .setDescription("The number of database transaction commits.")
        .build();
    this.transactionRollbacks =
      meter.counterBuilder("idstore_db_rollbacks")
        .setDescription("The number of database transaction rollbacks.")
        .build();
    this.resources.add(
      meter.gaugeBuilder("idstore_db_connection_time")
        .setDescription("The amount of time a database connection is held.")
        .ofLongs()
        .buildWithCallback(measurement -> {
          measurement.record(this.connectionTimeNow);
        })
    );

    this.resources.add(
      meter.gaugeBuilder("idstore_db_connections_active")
        .setDescription("The number of connections that are currently active.")
        .ofLongs()
        .buildWithCallback(measurement -> {
          measurement.record(
            (long) this.dataSource.getHikariPoolMXBean().getActiveConnections()
          );
        })
    );

    this.resources.add(
      meter.gaugeBuilder("idstore_db_connections_total")
        .setDescription("The total number of available connections.")
        .ofLongs()
        .buildWithCallback(measurement -> {
          measurement.record(
            (long) this.dataSource.getHikariPoolMXBean().getTotalConnections()
          );
        })
    );
  }

  LongCounter counterTransactions()
  {
    return this.transactions;
  }

  LongCounter counterTransactionCommits()
  {
    return this.transactionCommits;
  }

  LongCounter counterTransactionRollbacks()
  {
    return this.transactionRollbacks;
  }

  @Override
  public void close()
    throws IdDatabaseException
  {
    this.resources.close();
  }

  /**
   * @return The OpenTelemetry tracer
   */

  public Tracer tracer()
  {
    return this.tracer;
  }

  @Override
  public IdDatabaseConnectionType openConnection(
    final IdDatabaseRole role)
    throws IdDatabaseException
  {
    final var span =
      this.tracer
        .spanBuilder("IdDatabaseConnection")
        .setSpanKind(SpanKind.SERVER)
        .setAttribute(DB_SYSTEM, POSTGRESQL)
        .startSpan();

    try {
      span.addEvent("RequestConnection");
      final var conn = this.dataSource.getConnection();
      span.addEvent("ObtainedConnection");
      final var timeNow = OffsetDateTime.now();
      conn.setAutoCommit(false);
      return new IdDatabaseConnection(this, conn, timeNow, role, span);
    } catch (final SQLException e) {
      span.recordException(e);
      span.end();

      throw new IdDatabaseException(
        requireNonNullElse(e.getMessage(), e.getClass().getSimpleName()),
        e,
        SQL_ERROR,
        Map.of(),
        Optional.empty()
      );
    }
  }

  /**
   * @return The jooq SQL settings
   */

  Settings settings()
  {
    return this.settings;
  }

  /**
   * @return The clock used for time-related queries
   */

  Clock clock()
  {
    return this.clock;
  }

  @Override
  public String description()
  {
    return "Server database service.";
  }

  @Override
  public String toString()
  {
    return "[IdDatabase 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode(), 16));
  }

  void setConnectionTimeNow(
    final long nanos)
  {
    this.connectionTimeNow = nanos;
  }
}

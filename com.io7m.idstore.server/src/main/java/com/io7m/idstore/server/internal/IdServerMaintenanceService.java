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

package com.io7m.idstore.server.internal;

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseMaintenanceQueriesType;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.services.api.IdServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;

/**
 * A service that performs nightly database maintenance.
 */

public final class IdServerMaintenanceService
  implements IdServiceType, AutoCloseable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdServerMaintenanceService.class);

  private final ScheduledExecutorService executor;
  private final IdDatabaseType database;

  private IdServerMaintenanceService(
    final ScheduledExecutorService inExecutor,
    final IdDatabaseType inDatabase)
  {
    this.executor =
      Objects.requireNonNull(inExecutor, "executor");
    this.database =
      Objects.requireNonNull(inDatabase, "database");
  }

  /**
   * A service that performs nightly database maintenance.
   *
   * @param clock    The clock
   * @param database The database
   *
   * @return The service
   */

  public static IdServerMaintenanceService create(
    final IdServerClock clock,
    final IdDatabaseType database)
  {
    final var executor =
      Executors.newSingleThreadScheduledExecutor(r -> {
        final var thread = new Thread(r);
        thread.setDaemon(true);
        thread.setName(
          "com.io7m.idstore.server.internal.IdServerMaintenanceService[%d]".formatted(
            thread.getId()));
        return thread;
      });

    final var maintenanceService =
      new IdServerMaintenanceService(executor, database);

    final var timeNow =
      clock.now();
    final var timeNextMidnight =
      timeNow.withHour(0)
        .withMinute(0)
        .withSecond(0)
        .plusDays(1L);

    final var initialDelay =
      Duration.between(
        timeNow,
        timeNextMidnight).toSeconds();

    final var period =
      Duration.of(1L, ChronoUnit.DAYS)
        .toSeconds();

    executor.scheduleAtFixedRate(
      maintenanceService::runMaintenance,
      initialDelay,
      period,
      TimeUnit.SECONDS
    );

    return maintenanceService;
  }

  private void runMaintenance()
  {
    LOG.info("maintenance starting");
    try (var connection =
           this.database.openConnection(IDSTORE)) {
      try (var transaction =
             connection.openTransaction()) {
        final var queries =
          transaction.queries(IdDatabaseMaintenanceQueriesType.class);
        queries.runMaintenance();
        transaction.commit();
        LOG.info("maintenance completed");
      }
    } catch (final IdDatabaseException e) {
      LOG.error("maintenance failed: ", e);
    }
  }

  @Override
  public String description()
  {
    return "Server maintenance service.";
  }

  @Override
  public void close()
    throws Exception
  {
    this.executor.shutdown();
  }
}

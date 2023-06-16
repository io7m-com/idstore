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


package com.io7m.idstore.server.service.health;

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import com.io7m.repetoir.core.RPServiceType;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE_READ_ONLY;

/**
 * The server health service.
 */

public final class IdServerHealth implements RPServiceType, AutoCloseable
{
  private final IdDatabaseType database;
  private final ScheduledExecutorService executor;
  private final IdServerTelemetryServiceType telemetry;
  private volatile String status;

  private IdServerHealth(
    final IdServerTelemetryServiceType inTelemetry,
    final IdDatabaseType inDatabase,
    final ScheduledExecutorService inExecutor)
  {
    this.telemetry =
      Objects.requireNonNull(inTelemetry, "inTelemetry");
    this.database =
      Objects.requireNonNull(inDatabase, "database");
    this.executor =
      Objects.requireNonNull(inExecutor, "executor");

    this.status = statusOKText();

    this.executor.scheduleAtFixedRate(
      this::updateHealthStatus, 0L, 30L, TimeUnit.SECONDS);
  }

  /**
   * @return The string used to indicate a healthy server
   */

  public static String statusOKText()
  {
    return "OK";
  }

  private void updateHealthStatus()
  {
    final var span =
      this.telemetry.tracer()
        .spanBuilder("IdServerHealth")
        .startSpan();

    try (var ignored1 = span.makeCurrent()) {
      try (var ignored2 = this.database.openConnection(IDSTORE_READ_ONLY)) {
        this.status = statusOKText();
      } catch (final IdDatabaseException e) {
        this.status = "UNHEALTHY DATABASE (%s)".formatted(e.getMessage());
      }
    } finally {
      span.end();
    }
  }

  /**
   * The server health service.
   *
   * @param services The services
   *
   * @return The health service
   */

  public static IdServerHealth create(
    final RPServiceDirectoryType services)
  {
    final var database =
      services.requireService(IdDatabaseType.class);
    final var telemetry =
      services.requireService(IdServerTelemetryServiceType.class);

    final var executor =
      Executors.newSingleThreadScheduledExecutor(r -> {
        final var thread = new Thread(r);
        thread.setDaemon(true);
        thread.setName(
          "com.io7m.idstore.server.service.health[%d]"
            .formatted(Long.valueOf(thread.getId()))
        );
        return thread;
      });

    return new IdServerHealth(telemetry, database, executor);
  }

  /**
   * @return The current health status
   */

  public String status()
  {
    return this.status;
  }

  @Override
  public String toString()
  {
    return "[IdServerHealth 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode(), 16));
  }

  @Override
  public String description()
  {
    return "Health service";
  }

  @Override
  public void close()
  {
    this.executor.shutdown();
  }
}

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


package com.io7m.idstore.database.api;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;

import java.util.function.Consumer;

/**
 * The type of server database factories.
 */

public interface IdDatabaseFactoryType
{
  /**
   * @return The database kind (such as "POSTGRESQL")
   */

  String kind();

  /**
   * Open a database.
   *
   * @param configuration   The database configuration
   * @param tracer          The telemetry tracer instance
   * @param meter           The telemetry meter instance
   * @param startupMessages A function that will receive startup messages
   *
   * @return A database
   *
   * @throws IdDatabaseException On errors
   */

  IdDatabaseType open(
    IdDatabaseConfiguration configuration,
    Tracer tracer,
    Meter meter,
    Consumer<String> startupMessages)
    throws IdDatabaseException;
}

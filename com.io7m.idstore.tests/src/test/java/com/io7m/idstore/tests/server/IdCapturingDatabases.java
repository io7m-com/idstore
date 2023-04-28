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

package com.io7m.idstore.tests.server;

import com.io7m.idstore.database.api.IdDatabaseConfiguration;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseFactoryType;
import com.io7m.idstore.database.api.IdDatabaseType;
import io.opentelemetry.api.OpenTelemetry;

import java.util.Objects;
import java.util.function.Consumer;

final class IdCapturingDatabases
  implements IdDatabaseFactoryType
{
  private final IdDatabaseFactoryType delegate;
  private IdDatabaseType mostRecent;

  IdCapturingDatabases(
    final IdDatabaseFactoryType inDelegate)
  {
    this.delegate =
      Objects.requireNonNull(inDelegate, "delegate");
  }

  @Override
  public String kind()
  {
    return this.delegate.kind();
  }

  @Override
  public IdDatabaseType open(
    final IdDatabaseConfiguration configuration,
    final OpenTelemetry openTelemetry,
    final Consumer<String> startupMessages)
    throws IdDatabaseException
  {
    final var database =
      this.delegate.open(configuration, openTelemetry, startupMessages);
    this.mostRecent = database;
    return database;
  }

  public IdDatabaseType mostRecent()
  {
    return this.mostRecent;
  }
}

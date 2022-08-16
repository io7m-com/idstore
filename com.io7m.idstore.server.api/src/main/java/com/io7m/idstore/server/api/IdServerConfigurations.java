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


package com.io7m.idstore.server.api;

import com.io7m.idstore.database.api.IdDatabaseConfiguration;
import com.io7m.idstore.database.api.IdDatabaseFactoryType;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import static com.io7m.idstore.database.api.IdDatabaseCreate.CREATE_DATABASE;
import static com.io7m.idstore.database.api.IdDatabaseCreate.DO_NOT_CREATE_DATABASE;
import static com.io7m.idstore.database.api.IdDatabaseUpgrade.DO_NOT_UPGRADE_DATABASE;
import static com.io7m.idstore.database.api.IdDatabaseUpgrade.UPGRADE_DATABASE;

/**
 * Functions to produce server configurations.
 */

public final class IdServerConfigurations
{
  private IdServerConfigurations()
  {

  }

  /**
   * Read a server configuration from the given file.
   *
   * @param locale The locale
   * @param clock  The clock
   * @param file   The file
   *
   * @return A server configuration
   *
   * @throws IOException On errors
   */

  public static IdServerConfiguration ofFile(
    final Locale locale,
    final Clock clock,
    final Path file)
    throws IOException
  {
    Objects.requireNonNull(locale, "locale");
    Objects.requireNonNull(clock, "clock");
    Objects.requireNonNull(file, "file");

    return ofFile(
      locale,
      clock,
      new IdServerConfigurationFiles().parse(file)
    );
  }

  /**
   * Read a server configuration from the given file.
   *
   * @param locale The locale
   * @param clock  The clock
   * @param file   The file
   *
   * @return A server configuration
   */

  public static IdServerConfiguration ofFile(
    final Locale locale,
    final Clock clock,
    final IdServerConfigurationFile file)
  {
    Objects.requireNonNull(locale, "locale");
    Objects.requireNonNull(clock, "clock");
    Objects.requireNonNull(file, "file");

    final var fileDbConfig =
      file.databaseConfiguration();

    final var databaseConfiguration =
      new IdDatabaseConfiguration(
        fileDbConfig.user(),
        fileDbConfig.password(),
        fileDbConfig.address(),
        fileDbConfig.port(),
        fileDbConfig.databaseName(),
        fileDbConfig.create() ? CREATE_DATABASE : DO_NOT_CREATE_DATABASE,
        fileDbConfig.upgrade() ? UPGRADE_DATABASE : DO_NOT_UPGRADE_DATABASE,
        clock
      );

    final var databaseFactories =
      ServiceLoader.load(IdDatabaseFactoryType.class)
        .iterator();

    final var database =
      findDatabase(databaseFactories, fileDbConfig.kind());

    return new IdServerConfiguration(
      locale,
      clock,
      database,
      databaseConfiguration,
      file.mailConfiguration(),
      file.httpConfiguration().userAPIService(),
      file.httpConfiguration().userViewService(),
      file.httpConfiguration().adminAPIService(),
      file.httpConfiguration().adminViewService()
    );
  }

  private static IdDatabaseFactoryType findDatabase(
    final Iterator<IdDatabaseFactoryType> databaseFactories,
    final IdServerDatabaseKind kind)
  {
    if (!databaseFactories.hasNext()) {
      throw new ServiceConfigurationError(
        "No available implementations of type %s"
          .formatted(IdDatabaseFactoryType.class)
      );
    }

    final var kinds = new ArrayList<String>();
    while (databaseFactories.hasNext()) {
      final var database = databaseFactories.next();
      kinds.add(database.kind());
      if (Objects.equals(database.kind(), kind.name())) {
        return database;
      }
    }

    throw new ServiceConfigurationError(
      "No available databases of kind %s (Available databases include: %s)"
        .formatted(IdDatabaseFactoryType.class, kinds)
    );
  }
}

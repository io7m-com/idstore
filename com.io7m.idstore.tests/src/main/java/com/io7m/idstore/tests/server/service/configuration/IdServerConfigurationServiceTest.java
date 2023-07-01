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


package com.io7m.idstore.tests.server.service.configuration;

import com.io7m.idstore.server.api.IdServerConfigurations;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationFiles;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationService;
import com.io7m.idstore.server.service.telemetry.api.IdMetricsService;
import com.io7m.idstore.tests.IdTestDirectories;
import com.io7m.idstore.tests.server.service.IdServiceContract;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Locale;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class IdServerConfigurationServiceTest
  extends IdServiceContract<IdServerConfigurationService>
{
  private Path directory;

  @BeforeEach
  public void setup()
    throws IOException
  {
    this.directory = IdTestDirectories.createTempDirectory();
  }

  @AfterEach
  public void tearDown()
    throws IOException
  {
    IdTestDirectories.deleteDirectory(this.directory);
  }

  @Test
  public void testConfig0()
  {
    this.roundTrip("server-config-0.xml");
  }

  @Test
  public void testConfig2()
  {
    this.roundTrip("server-config-2.xml");
  }

  private void roundTrip(
    final String name)
  {
    try {
      final var files =
        new IdServerConfigurationFiles();

      final var file =
        IdTestDirectories.resourceOf(
          IdServerConfigurationServiceTest.class,
          this.directory,
          name
        );

      final var parsed0 =
        files.parse(file);

      final var parsedConfig0 =
        IdServerConfigurations.ofFile(
          Locale.getDefault(),
          Clock.systemUTC(),
          parsed0
        );

      final var outputFile =
        this.directory.resolve("output.xml");

      try (var output = Files.newOutputStream(outputFile, CREATE, WRITE)) {
        files.serialize(output, parsedConfig0);
      }

      final var parsed1 =
        files.parse(outputFile);

      assertEquals(parsed0, parsed1);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  protected IdServerConfigurationService createInstanceA()
  {
    try {
      final var files =
        new IdServerConfigurationFiles();
      final var file =
        IdTestDirectories.resourceOf(
        IdServerConfigurationServiceTest.class,
        this.directory,
        "server-config-0.xml"
      );

      final var configFile =
        files.parse(file);

      final var configuration =
        IdServerConfigurations.ofFile(
          Locale.getDefault(),
          Clock.systemUTC(),
          configFile
        );

      return new IdServerConfigurationService(
        Mockito.mock(IdMetricsService.class),
        configuration
      );
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  protected IdServerConfigurationService createInstanceB()
  {
    try {
      final var files =
        new IdServerConfigurationFiles();
      final var file =
        IdTestDirectories.resourceOf(
          IdServerConfigurationServiceTest.class,
          this.directory,
          "server-config-2.xml"
        );

      final var configFile =
        files.parse(file);

      final var configuration =
        IdServerConfigurations.ofFile(
          Locale.getDefault(),
          Clock.systemUTC(),
          configFile
        );

      return new IdServerConfigurationService(
        Mockito.mock(IdMetricsService.class),
        configuration
      );
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}

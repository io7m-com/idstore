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

import com.io7m.anethum.slf4j.ParseStatusLogging;
import com.io7m.idstore.server.api.IdServerConfiguration;
import com.io7m.idstore.server.api.IdServerConfigurationFile;
import com.io7m.idstore.server.api.IdServerConfigurations;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationParsers;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationSerializers;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationService;
import com.io7m.idstore.server.service.telemetry.api.IdMetricsService;
import com.io7m.idstore.tests.IdTestDirectories;
import com.io7m.idstore.tests.server.service.IdServiceContract;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Locale;

import static com.io7m.blackthorne.core.BTPreserveLexical.DISCARD_LEXICAL_INFORMATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class IdServerConfigurationServiceTest
  extends IdServiceContract<IdServerConfigurationService>
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdServerConfigurationServiceTest.class);

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
    throws Exception
  {
    this.roundTrip("server-config-0.xml");
  }

  @Test
  public void testConfig2()
    throws Exception
  {
    final var c = this.roundTrip("server-config-2.xml");

    final var ot = c.openTelemetry().orElseThrow();
    assertTrue(ot.logs().isPresent());
    assertTrue(ot.metrics().isPresent());
    assertTrue(ot.traces().isPresent());
  }

  @Test
  public void testConfig3()
    throws Exception
  {
    this.roundTrip("server-config-3.xml");
  }

  private IdServerConfiguration roundTrip(
    final String name)
    throws Exception
  {
    final var parsers =
      new IdServerConfigurationParsers();
    final var serializers =
      new IdServerConfigurationSerializers();

    final var file =
      IdTestDirectories.resourceOf(
        IdServerConfigurationServiceTest.class,
        this.directory,
        name
      );

    final IdServerConfigurationFile parsed0;
    try (var parser =
           parsers.createParserForFileWithContext(
             DISCARD_LEXICAL_INFORMATION,
             file,
             x -> ParseStatusLogging.logWithAll(LOG, x))) {
      parsed0 = parser.execute();
    }

    final var parsedConfig0 =
      IdServerConfigurations.ofFile(
        Locale.getDefault(),
        Clock.systemUTC(),
        parsed0
      );

    final var outputFile =
      this.directory.resolve("output.xml");

    serializers.serializeFile(outputFile, parsed0);

    final IdServerConfigurationFile parsed1;
    try (var parser =
           parsers.createParserForFileWithContext(
             DISCARD_LEXICAL_INFORMATION,
             outputFile,
             x -> ParseStatusLogging.logWithAll(LOG, x))) {
      parsed1 = parser.execute();
    }

    assertEquals(parsed0.brandingConfiguration(), parsed1.brandingConfiguration());
    assertEquals(parsed0.databaseConfiguration(), parsed1.databaseConfiguration());
    assertEquals(parsed0.historyConfiguration(), parsed1.historyConfiguration());
    assertEquals(parsed0.httpConfiguration(), parsed1.httpConfiguration());
    assertEquals(parsed0.mailConfiguration(), parsed1.mailConfiguration());
    assertEquals(parsed0.openTelemetry(), parsed1.openTelemetry());
    assertEquals(parsed0.passwordExpiration(), parsed1.passwordExpiration());
    assertEquals(parsed0.rateLimit(), parsed1.rateLimit());
    assertEquals(parsed0.sessionConfiguration(), parsed1.sessionConfiguration());
    assertEquals(parsed0, parsed1);

    return parsedConfig0;
  }

  @Override
  protected IdServerConfigurationService createInstanceA()
  {
    try {
      final var parsers =
        new IdServerConfigurationParsers();

      final var file =
        IdTestDirectories.resourceOf(
          IdServerConfigurationServiceTest.class,
          this.directory,
          "server-config-0.xml"
        );

      final var configFile =
        parsers.parseFile(file);

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
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  protected IdServerConfigurationService createInstanceB()
  {
    try {
      final var parsers =
        new IdServerConfigurationParsers();

      final var file =
        IdTestDirectories.resourceOf(
          IdServerConfigurationServiceTest.class,
          this.directory,
          "server-config-2.xml"
        );

      final var configFile =
        parsers.parseFile(file);

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
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }
}

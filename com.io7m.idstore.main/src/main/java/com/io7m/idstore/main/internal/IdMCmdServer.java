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

package com.io7m.idstore.main.internal;

import com.io7m.idstore.server.api.IdServerConfigurations;
import com.io7m.idstore.server.api.IdServerFactoryType;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationFiles;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QCommandStatus;
import com.io7m.quarrel.core.QCommandType;
import com.io7m.quarrel.core.QParameterNamed1;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QParametersPositionalNone;
import com.io7m.quarrel.core.QParametersPositionalType;
import com.io7m.quarrel.core.QStringType;
import com.io7m.quarrel.ext.logback.QLogback;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Stream;

import static com.io7m.quarrel.core.QCommandStatus.SUCCESS;

/**
 * The "server" command.
 */

public final class IdMCmdServer implements QCommandType
{
  private static final QParameterNamed1<Path> CONFIGURATION_FILE =
    new QParameterNamed1<>(
      "--configuration",
      List.of(),
      new QStringType.QConstant("The configuration file."),
      Optional.empty(),
      Path.class
    );

  private final QCommandMetadata metadata;

  /**
   * Construct a command.
   */

  public IdMCmdServer()
  {
    this.metadata = new QCommandMetadata(
      "server",
      new QStringType.QConstant("Start the server."),
      Optional.empty()
    );
  }

  private static IllegalStateException noService()
  {
    return new IllegalStateException(
      "No services available of %s".formatted(IdServerFactoryType.class)
    );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return Stream.concat(
      Stream.of(CONFIGURATION_FILE),
      QLogback.parameters().stream()
    ).toList();
  }

  @Override
  public QParametersPositionalType onListPositionalParameters()
  {
    return new QParametersPositionalNone();
  }

  @Override
  public QCommandStatus onExecute(
    final QCommandContextType context)
    throws Exception
  {
    System.setProperty("org.jooq.no-tips", "true");
    System.setProperty("org.jooq.no-logo", "true");

    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();

    QLogback.configure(context);

    final var configFile =
      new IdServerConfigurationFiles()
        .parse(context.parameterValue(CONFIGURATION_FILE));

    final var configuration =
      IdServerConfigurations.ofFile(
        Locale.getDefault(),
        Clock.systemUTC(),
        configFile
      );

    final var servers =
      ServiceLoader.load(IdServerFactoryType.class)
        .findFirst()
        .orElseThrow(IdMCmdServer::noService);

    try (var server = servers.createServer(configuration)) {
      server.start();

      while (true) {
        try {
          Thread.sleep(1_000L);
        } catch (final InterruptedException e) {
          break;
        }
      }
    }

    return SUCCESS;
  }

  @Override
  public QCommandMetadata metadata()
  {
    return this.metadata;
  }
}

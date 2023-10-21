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

import com.io7m.anethum.slf4j.ParseStatusLogging;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.server.api.IdServerConfigurations;
import com.io7m.idstore.server.api.IdServerFactoryType;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationParsers;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QCommandStatus;
import com.io7m.quarrel.core.QCommandType;
import com.io7m.quarrel.core.QParameterNamed1;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QStringType.QConstant;
import com.io7m.quarrel.ext.logback.QLogback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.stream.Stream;

import static com.io7m.quarrel.core.QCommandStatus.SUCCESS;

/**
 * The "initial-admin" command.
 */

public final class IdMCmdInitialAdmin implements QCommandType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdMCmdInitialAdmin.class);

  private static final QParameterNamed1<Path> CONFIGURATION_FILE =
    new QParameterNamed1<>(
      "--configuration",
      List.of(),
      new QConstant("The configuration file."),
      Optional.empty(),
      Path.class
    );

  private static final QParameterNamed1<UUID> INITIAL_ADMIN =
    new QParameterNamed1<>(
      "--admin-id",
      List.of(),
      new QConstant("The ID of the initial administrator."),
      Optional.empty(),
      UUID.class
    );

  private static final QParameterNamed1<String> INITIAL_ADMIN_NAME =
    new QParameterNamed1<>(
      "--admin-username",
      List.of(),
      new QConstant("The initial administrator to create."),
      Optional.empty(),
      String.class
    );

  private static final QParameterNamed1<String> INITIAL_ADMIN_PASSWORD =
    new QParameterNamed1<>(
      "--admin-password",
      List.of(),
      new QConstant("The password of the initial administrator."),
      Optional.empty(),
      String.class
    );

  private static final QParameterNamed1<String> INITIAL_ADMIN_EMAIL =
    new QParameterNamed1<>(
      "--admin-email",
      List.of(),
      new QConstant("The email address of the initial administrator."),
      Optional.empty(),
      String.class
    );

  private static final QParameterNamed1<String> INITIAL_ADMIN_REALNAME =
    new QParameterNamed1<>(
      "--admin-realname",
      List.of(),
      new QConstant("The real name of the initial administrator."),
      Optional.empty(),
      String.class
    );

  private final QCommandMetadata metadata;

  /**
   * Construct a command.
   */

  public IdMCmdInitialAdmin()
  {
    this.metadata = new QCommandMetadata(
      "initial-admin",
      new QConstant("Create or update the initial administrator."),
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
      Stream.of(
        CONFIGURATION_FILE,
        INITIAL_ADMIN,
        INITIAL_ADMIN_EMAIL,
        INITIAL_ADMIN_NAME,
        INITIAL_ADMIN_PASSWORD,
        INITIAL_ADMIN_REALNAME
      ),
      QLogback.parameters().stream()
    ).toList();
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

    final var configurationFile =
      context.parameterValue(CONFIGURATION_FILE);

    final var parsers =
      new IdServerConfigurationParsers();
    final var configFile =
      parsers.parseFile(
        configurationFile,
        status -> ParseStatusLogging.logWithAll(LOG, status)
      );

    final var configuration =
      IdServerConfigurations.ofFile(
        Locale.getDefault(),
        Clock.systemUTC(),
        configFile
      );

    final var servers =
      ServiceLoader.load(IdServerFactoryType.class)
        .findFirst()
        .orElseThrow(IdMCmdInitialAdmin::noService);

    try (var server = servers.createServer(configuration)) {
      server.createOrUpdateInitialAdmin(
        context.parameterValue(INITIAL_ADMIN),
        new IdName(context.parameterValue(INITIAL_ADMIN_NAME)),
        new IdEmail(context.parameterValue(INITIAL_ADMIN_EMAIL)),
        new IdRealName(context.parameterValue(INITIAL_ADMIN_REALNAME)),
        context.parameterValue(INITIAL_ADMIN_PASSWORD)
      );
    }

    return SUCCESS;
  }

  @Override
  public QCommandMetadata metadata()
  {
    return this.metadata;
  }
}

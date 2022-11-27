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

package com.io7m.idstore.server.main.internal;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.io7m.claypot.core.CLPAbstractCommand;
import com.io7m.claypot.core.CLPCommandContextType;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.server.api.IdServerConfigurations;
import com.io7m.idstore.server.api.IdServerFactoryType;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationFiles;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.nio.file.Path;
import java.time.Clock;
import java.util.Locale;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.UUID;

import static com.io7m.claypot.core.CLPCommandType.Status.SUCCESS;

/**
 * The "initialize" command.
 */

@Parameters(commandDescription = "Initialize the database.")
public final class IdSCmdInitialize extends CLPAbstractCommand
{
  @Parameter(
    names = "--configuration",
    description = "The configuration file",
    required = true
  )
  private Path configurationFile;

  @Parameter(
    names = "--admin-id",
    description = "The ID of the initial administrator",
    required = false
  )
  private UUID adminId;

  @Parameter(
    names = "--admin-username",
    description = "The initial administrator to create.",
    required = true
  )
  private String adminUsername;

  @Parameter(
    names = "--admin-password",
    description = "The password of the initial administrator.",
    required = true
  )
  private String adminPassword;

  @Parameter(
    names = "--admin-email",
    description = "The email address of the initial administrator.",
    required = true
  )
  private String adminEmail;

  @Parameter(
    names = "--admin-realname",
    description = "The real name of the initial administrator.",
    required = true
  )
  private String adminRealname;

  /**
   * Construct a command.
   *
   * @param inContext The command context
   */

  public IdSCmdInitialize(
    final CLPCommandContextType inContext)
  {
    super(inContext);
  }

  @Override
  protected Status executeActual()
    throws Exception
  {
    System.setProperty("org.jooq.no-tips", "true");
    System.setProperty("org.jooq.no-logo", "true");

    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();

    final var configFile =
      new IdServerConfigurationFiles()
        .parse(this.configurationFile);

    final var configuration =
      IdServerConfigurations.ofFile(
        Locale.getDefault(),
        Clock.systemUTC(),
        configFile
      );

    final var servers =
      ServiceLoader.load(IdServerFactoryType.class)
        .findFirst()
        .orElseThrow(IdSCmdInitialize::noService);

    try (var server = servers.createServer(configuration)) {
      server.setup(
        Optional.ofNullable(this.adminId),
        new IdName(this.adminUsername),
        new IdEmail(this.adminEmail),
        new IdRealName(this.adminRealname),
        this.adminPassword
      );
    }

    return SUCCESS;
  }

  private static IllegalStateException noService()
  {
    return new IllegalStateException(
      "No services available of %s".formatted(IdServerFactoryType.class)
    );
  }

  @Override
  public String name()
  {
    return "initialize";
  }
}

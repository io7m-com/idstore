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

package com.io7m.idstore.server.main.internal;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.io7m.claypot.core.CLPAbstractCommand;
import com.io7m.claypot.core.CLPCommandContextType;
import com.io7m.idstore.server.api.IdServerConfigurations;
import com.io7m.idstore.server.api.IdServerFactoryType;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationFiles;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.nio.file.Path;
import java.time.Clock;
import java.util.Locale;
import java.util.ServiceLoader;

import static com.io7m.claypot.core.CLPCommandType.Status.SUCCESS;

/**
 * The "server" command.
 */

@Parameters(commandDescription = "Start the server.")
public final class IdSCmdServer extends CLPAbstractCommand
{
  @Parameter(
    names = "--configuration",
    description = "The configuration file",
    required = true
  )
  private Path configurationFile;

  /**
   * Construct a command.
   *
   * @param inContext The command context
   */

  public IdSCmdServer(
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
        .orElseThrow(IdSCmdServer::noService);

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

  private static IllegalStateException noService()
  {
    return new IllegalStateException(
      "No services available of %s".formatted(IdServerFactoryType.class)
    );
  }

  @Override
  public String name()
  {
    return "server";
  }
}

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


package com.io7m.idstore.server.admin_v1;

import com.io7m.idstore.model.IdUserDomain;
import com.io7m.idstore.server.http.IdHTTPRequestTimeFilter;
import com.io7m.idstore.server.service.clock.IdServerClock;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationService;
import com.io7m.idstore.server.service.telemetry.api.IdMetricsServiceType;
import com.io7m.idstore.server.service.tls.IdTLSContextServiceType;
import com.io7m.idstore.tls.IdTLSEnabled;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import io.helidon.common.tls.TlsConfig;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.http.HttpRouting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;

import static java.net.StandardSocketOptions.SO_REUSEADDR;
import static java.net.StandardSocketOptions.SO_REUSEPORT;


/**
 * The Admin API v1 server.
 */

public final class IdA1Server
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdA1Server.class);

  private IdA1Server()
  {

  }

  /**
   * Create an admin API v1 server.
   *
   * @param services The service directory
   *
   * @return A server
   *
   * @throws Exception On errors
   */

  public static WebServer createAdminAPIServer(
    final RPServiceDirectoryType services)
    throws Exception
  {
    final var configurationService =
      services.requireService(IdServerConfigurationService.class);
    final var tlsService =
      services.requireService(IdTLSContextServiceType.class);
    final var configuration =
      configurationService.configuration();
    final var httpConfig =
      configuration.adminApiAddress();
    final var address =
      InetSocketAddress.createUnresolved(
        httpConfig.listenAddress(),
        httpConfig.listenPort()
      );

    final var routing =
      HttpRouting.builder()
        .addFilter(new IdHTTPRequestTimeFilter(
          services.requireService(IdMetricsServiceType.class),
          IdUserDomain.ADMIN,
          services.requireService(IdServerClock.class)
        ))
        .get("/",
             new IdA1HandlerVersions(services))
        .post("/admin/1/0/login",
             new IdA1HandlerLogin(services))
        .post("/admin/1/0/command",
             new IdA1HandlerCommand(services))
        .get("/health",
             new IdA1HandlerHealth(services));

    final var webServerBuilder =
      WebServerConfig.builder();

    if (httpConfig.tlsConfiguration() instanceof final IdTLSEnabled enabled) {
      final var tlsContext =
        tlsService.create(
          "UserAPI",
          enabled.keyStore(),
          enabled.trustStore()
        );

      webServerBuilder.tls(
        TlsConfig.builder()
          .enabled(true)
          .sslContext(tlsContext.context())
          .build()
      );
    }

    final var webServer =
      webServerBuilder
        .port(httpConfig.listenPort())
        .address(InetAddress.getByName(httpConfig.listenAddress()))
        .routing(routing)
        .listenerSocketOptions(Map.ofEntries(
          Map.entry(SO_REUSEADDR, Boolean.TRUE),
          Map.entry(SO_REUSEPORT, Boolean.TRUE)
        ))
        .build();

    webServer.start();
    LOG.info("[{}] Admin API server started", address);
    return webServer;
  }
}

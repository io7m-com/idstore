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


package com.io7m.idstore.server.user_view;

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
 * A user view server.
 */

public final class IdUVServer
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdUVServer.class);

  private IdUVServer()
  {

  }

  /**
   * Create a user view server.
   *
   * @param services The service directory
   *
   * @return A server
   *
   * @throws Exception On errors
   */

  public static WebServer createUserViewServer(
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
      configuration.userViewAddress();
    final var address =
      InetSocketAddress.createUnresolved(
        httpConfig.listenAddress(),
        httpConfig.listenPort()
      );

    final var routing =
      createRouting(services);

    final var webServerBuilder =
      WebServerConfig.builder();

    if (httpConfig.tlsConfiguration() instanceof final IdTLSEnabled enabled) {
      final var tlsContext =
        tlsService.create(
          "UserView",
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
      webServerBuilder.port(httpConfig.listenPort())
        .address(InetAddress.getByName(httpConfig.listenAddress()))
        .routing(routing)
        .listenerSocketOptions(Map.ofEntries(
          Map.entry(SO_REUSEADDR, Boolean.TRUE),
          Map.entry(SO_REUSEPORT, Boolean.TRUE)
        ))
        .build();

    webServer.start();
    LOG.info("[{}] User View server started", address);
    return webServer;
  }

  private static HttpRouting.Builder createRouting(
    final RPServiceDirectoryType services)
  {
    final var router = HttpRouting.builder();
    router.addFilter(new IdHTTPRequestTimeFilter(
      services.requireService(IdMetricsServiceType.class),
      IdUserDomain.USER,
      services.requireService(IdServerClock.class)
    ));

    router.get("/", new IdUVMain(services));

    {
      final var handler = new IdUVLogout(services);
      router.get("/logout", handler);
      router.post("/logout", handler);
    }

    {
      final var handler = new IdUVLogin(services);
      router.get("/login", handler);
      router.post("/login", handler);
      router.get("/login/*", handler);
      router.post("/login/*", handler);
    }

    {
      final var handler = new IdUVCSS(services);
      router.get("/css", handler);
      router.get("/css/*", handler);
    }

    {
      final var handler = new IdUVLogo(services);
      router.get("/logo", handler);
      router.get("/logo/*", handler);
      router.get("/favicon.ico", handler);
    }

    {
      final var handler = new IdUVEmailAdd(services);
      router.get("/email-add", handler);
      router.post("/email-add", handler);
      router.get("/email-add/*", handler);
      router.post("/email-add/*", handler);
    }

    {
      final var handler = new IdUVEmailAddRun(services);
      router.get("/email-add-run", handler);
      router.post("/email-add-run", handler);
      router.get("/email-add-run/*", handler);
      router.post("/email-add-run/*", handler);
    }

    {
      final var handler = new IdUVEmailRemoveRun(services);
      router.get("/email-remove-run", handler);
      router.post("/email-remove-run", handler);
      router.get("/email-remove-run/*", handler);
      router.post("/email-remove-run/*", handler);
    }

    {
      final var handler = new IdUVEmailVerificationPermit(services);
      router.get("/email-verification-permit", handler);
      router.post("/email-verification-permit", handler);
      router.get("/email-verification-permit/*", handler);
      router.post("/email-verification-permit/*", handler);
    }

    {
      final var handler = new IdUVEmailVerificationDeny(services);
      router.get("/email-verification-deny", handler);
      router.post("/email-verification-deny", handler);
      router.get("/email-verification-deny/*", handler);
      router.post("/email-verification-deny/*", handler);
    }

    {
      final var handler = new IdUVRealnameUpdate(services);
      router.get("/realname-update", handler);
      router.post("/realname-update", handler);
      router.get("/realname-update/*", handler);
      router.post("/realname-update/*", handler);
    }

    {
      final var handler = new IdUVRealnameUpdateRun(services);
      router.get("/realname-update-run", handler);
      router.post("/realname-update-run", handler);
      router.get("/realname-update-run/*", handler);
      router.post("/realname-update-run/*", handler);
    }

    {
      final var handler = new IdUVPasswordReset(services);
      router.get("/password-reset", handler);
      router.post("/password-reset", handler);
      router.get("/password-reset/*", handler);
      router.post("/password-reset/*", handler);
    }

    {
      final var handler = new IdUVPasswordResetRun(services);
      router.get("/password-reset-run", handler);
      router.post("/password-reset-run", handler);
      router.get("/password-reset-run/*", handler);
      router.post("/password-reset-run/*", handler);
    }

    {
      final var handler = new IdUVPasswordResetConfirm(services);
      router.get("/password-reset-confirm", handler);
      router.post("/password-reset-confirm", handler);
      router.get("/password-reset-confirm/*", handler);
      router.post("/password-reset-confirm/*", handler);
    }

    {
      final var handler = new IdUVPasswordResetConfirmRun(services);
      router.get("/password-reset-confirm-run", handler);
      router.post("/password-reset-confirm-run", handler);
      router.get("/password-reset-confirm-run/*", handler);
      router.post("/password-reset-confirm-run/*", handler);
    }

    {
      final var handler = new IdUVPasswordUpdate(services);
      router.get("/password-update", handler);
      router.post("/password-update", handler);
      router.get("/password-update/*", handler);
      router.post("/password-update/*", handler);
    }

    {
      final var handler = new IdUVPasswordUpdateRun(services);
      router.get("/password-update-run", handler);
      router.post("/password-update-run", handler);
      router.get("/password-update-run/*", handler);
      router.post("/password-update-run/*", handler);
    }

    return router;
  }
}

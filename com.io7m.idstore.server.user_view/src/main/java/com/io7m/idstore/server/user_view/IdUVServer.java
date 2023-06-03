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

import com.io7m.idstore.server.http.IdPlainErrorHandler;
import com.io7m.idstore.server.http.IdServletHolders;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationService;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.NullSessionDataStore;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

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

  public static Server createUserViewServer(
    final RPServiceDirectoryType services)
    throws Exception
  {
    final var configurationService =
      services.requireService(IdServerConfigurationService.class);
    final var configuration =
      configurationService.configuration();
    final var httpConfig =
      configuration.userViewAddress();
    final var address =
      InetSocketAddress.createUnresolved(
        httpConfig.listenAddress(),
        httpConfig.listenPort()
      );

    final var server =
      new Server(address);

    /*
     * Add a request customizer that properly handles headers such as
     * X-Forwarded-For and so on. Without this, running the idstore server
     * behind a reverse proxy would result in rate-limiting decisions being
     * applied to the address of the proxy rather than the address of the
     * client making the request.
     */

    for (final var connector : server.getConnectors()) {
      for (final var factory : connector.getConnectionFactories()) {
        if (factory instanceof final HttpConfiguration.ConnectionFactory http) {
          http.getHttpConfiguration()
            .addCustomizer(new ForwardedRequestCustomizer());
        }
      }
    }

    /*
     * Configure all the servlets.
     */

    final var servlets =
      createServletHolders(services);

    /*
     * Set up a session handler that allows for Servlets to have sessions
     * that can survive server restarts.
     */

    final var sessionIds = new DefaultSessionIdManager(server);
    server.setSessionIdManager(sessionIds);

    final var sessionHandler = new SessionHandler();
    sessionHandler.setSessionCookie("IDSTORE_USER_VIEW_SESSION");
    sessionHandler.setMaxInactiveInterval(
      Math.toIntExact(
        configuration.sessions()
          .userSessionExpiration()
          .toSeconds())
    );

    final var sessionCache = new DefaultSessionCache(sessionHandler);
    sessionCache.setSessionDataStore(new NullSessionDataStore());

    sessionHandler.setSessionCache(sessionCache);
    sessionHandler.setSessionIdManager(sessionIds);
    sessionHandler.setHandler(servlets);

    /*
     * Enable gzip.
     */

    final var gzip = new GzipHandler();
    gzip.setHandler(sessionHandler);

    server.setErrorHandler(new IdPlainErrorHandler());
    server.setRequestLog((request, response) -> {

    });
    server.setHandler(gzip);
    server.start();
    LOG.info("[{}] User view server started", address);
    return server;
  }

  private static ServletContextHandler createServletHolders(
    final RPServiceDirectoryType services)
  {
    final var servletHolders =
      new IdServletHolders(services);
    final var servlets =
      new ServletContextHandler();

    createUserViewServerServlets(servletHolders, servlets);
    return servlets;
  }

  private static void createUserViewServerServlets(
    final IdServletHolders servletHolders,
    final ServletContextHandler servlets)
  {
    servlets.addServlet(
      servletHolders.create(
        IdUVMain.class,
        IdUVMain::new),
      "/"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUVLogout.class,
        IdUVLogout::new),
      "/logout"
    );

    servlets.addServlet(
      servletHolders.create(
        IdUVLogin.class,
        IdUVLogin::new),
      "/login"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUVLogin.class,
        IdUVLogin::new),
      "/login/*"
    );

    servlets.addServlet(
      servletHolders.create(
        IdUVCSS.class,
        IdUVCSS::new),
      "/css/*"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUVCSS.class,
        IdUVCSS::new),
      "/css"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUVLogo.class,
        IdUVLogo::new),
      "/logo/*"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUVLogo.class,
        IdUVLogo::new),
      "/logo"
    );

    servlets.addServlet(
      servletHolders.create(
        IdUVLogo.class,
        IdUVLogo::new),
      "/favicon.ico"
    );

    servlets.addServlet(
      servletHolders.create(
        IdUVEmailAdd.class,
        IdUVEmailAdd::new),
      "/email-add"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUVEmailAdd.class,
        IdUVEmailAdd::new),
      "/email-add/*"
    );

    servlets.addServlet(
      servletHolders.create(
        IdUVEmailAddRun.class,
        IdUVEmailAddRun::new),
      "/email-add-run"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUVEmailAddRun.class,
        IdUVEmailAddRun::new),
      "/email-add-run/*"
    );

    servlets.addServlet(
      servletHolders.create(
        IdUVEmailRemoveRun.class,
        IdUVEmailRemoveRun::new),
      "/email-remove-run"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUVEmailRemoveRun.class,
        IdUVEmailRemoveRun::new),
      "/email-remove-run/*"
    );

    servlets.addServlet(
      servletHolders.create(
        IdUVEmailVerificationPermit.class,
        IdUVEmailVerificationPermit::new),
      "/email-verification-permit"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUVEmailVerificationPermit.class,
        IdUVEmailVerificationPermit::new),
      "/email-verification-permit/*"
    );

    servlets.addServlet(
      servletHolders.create(
        IdUVEmailVerificationDeny.class,
        IdUVEmailVerificationDeny::new),
      "/email-verification-deny"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUVEmailVerificationDeny.class,
        IdUVEmailVerificationDeny::new),
      "/email-verification-deny/*"
    );

    servlets.addServlet(
      servletHolders.create(
        IdUVRealnameUpdate.class,
        IdUVRealnameUpdate::new),
      "/realname-update"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUVRealnameUpdate.class,
        IdUVRealnameUpdate::new),
      "/realname-update/*"
    );

    servlets.addServlet(
      servletHolders.create(
        IdUVRealnameUpdateRun.class,
        IdUVRealnameUpdateRun::new),
      "/realname-update-run"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUVRealnameUpdateRun.class,
        IdUVRealnameUpdateRun::new),
      "/realname-update-run/*"
    );

    servlets.addServlet(
      servletHolders.create(
        IdUVPasswordReset.class,
        IdUVPasswordReset::new),
      "/password-reset"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUVPasswordReset.class,
        IdUVPasswordReset::new),
      "/password-reset/*"
    );

    servlets.addServlet(
      servletHolders.create(
        IdUVPasswordResetRun.class,
        IdUVPasswordResetRun::new),
      "/password-reset-run"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUVPasswordResetRun.class,
        IdUVPasswordResetRun::new),
      "/password-reset-run/*"
    );

    servlets.addServlet(
      servletHolders.create(
        IdUVPasswordResetConfirm.class,
        IdUVPasswordResetConfirm::new),
      "/password-reset-confirm"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUVPasswordResetConfirm.class,
        IdUVPasswordResetConfirm::new),
      "/password-reset-confirm/*"
    );

    servlets.addServlet(
      servletHolders.create(
        IdUVPasswordResetConfirmRun.class,
        IdUVPasswordResetConfirmRun::new),
      "/password-reset-confirm-run"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUVPasswordResetConfirmRun.class,
        IdUVPasswordResetConfirmRun::new),
      "/password-reset-confirm-run/*"
    );

    servlets.addServlet(
      servletHolders.create(
        IdUVPasswordUpdate.class,
        IdUVPasswordUpdate::new),
      "/password-update"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUVPasswordUpdate.class,
        IdUVPasswordUpdate::new),
      "/password-update/*"
    );

    servlets.addServlet(
      servletHolders.create(
        IdUVPasswordUpdateRun.class,
        IdUVPasswordUpdateRun::new),
      "/password-update-run"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUVPasswordUpdateRun.class,
        IdUVPasswordUpdateRun::new),
      "/password-update-run/*"
    );
  }
}

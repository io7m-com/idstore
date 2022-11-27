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


package com.io7m.idstore.server.user_view;

import com.io7m.idstore.server.http.IdCommonCSSServlet;
import com.io7m.idstore.server.http.IdCommonLogoServlet;
import com.io7m.idstore.server.http.IdPlainErrorHandler;
import com.io7m.idstore.server.http.IdRequestUniqueIDs;
import com.io7m.idstore.server.http.IdServletHolders;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationService;
import com.io7m.idstore.services.api.IdServiceDirectoryType;
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
import java.util.Arrays;

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
    final IdServiceDirectoryType services)
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

    /*
     * Add a connector listener that adds unique identifiers to all requests.
     */

    Arrays.stream(server.getConnectors()).forEach(
      connector -> connector.addBean(new IdRequestUniqueIDs(services))
    );

    server.setErrorHandler(new IdPlainErrorHandler());
    server.setRequestLog((request, response) -> {

    });
    server.setHandler(gzip);
    server.start();
    LOG.info("[{}] User view server started", address);
    return server;
  }

  private static ServletContextHandler createServletHolders(
    final IdServiceDirectoryType services)
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
        IdUViewMain.class,
        IdUViewMain::new),
      "/"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUViewLogout.class,
        IdUViewLogout::new),
      "/logout"
    );

    servlets.addServlet(
      servletHolders.create(
        IdUViewLogin.class,
        IdUViewLogin::new),
      "/login"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUViewLogin.class,
        IdUViewLogin::new),
      "/login/*"
    );

    servlets.addServlet(
      servletHolders.create(
        IdCommonCSSServlet.class,
        IdCommonCSSServlet::new),
      "/css/*"
    );
    servlets.addServlet(
      servletHolders.create(
        IdCommonCSSServlet.class,
        IdCommonCSSServlet::new),
      "/css"
    );
    servlets.addServlet(
      servletHolders.create(
        IdCommonLogoServlet.class,
        IdCommonLogoServlet::new),
      "/logo/*"
    );
    servlets.addServlet(
      servletHolders.create(
        IdCommonLogoServlet.class,
        IdCommonLogoServlet::new),
      "/logo"
    );

    servlets.addServlet(
      servletHolders.create(
        IdCommonLogoServlet.class,
        IdCommonLogoServlet::new),
      "/favicon.ico"
    );

    servlets.addServlet(
      servletHolders.create(
        IdUViewEmailAdd.class,
        IdUViewEmailAdd::new),
      "/email-add"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUViewEmailAdd.class,
        IdUViewEmailAdd::new),
      "/email-add/*"
    );

    servlets.addServlet(
      servletHolders.create(
        IdUViewEmailAddRun.class,
        IdUViewEmailAddRun::new),
      "/email-add-run"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUViewEmailAddRun.class,
        IdUViewEmailAddRun::new),
      "/email-add-run/*"
    );

    servlets.addServlet(
      servletHolders.create(
        IdUViewEmailRemoveRun.class,
        IdUViewEmailRemoveRun::new),
      "/email-remove-run"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUViewEmailRemoveRun.class,
        IdUViewEmailRemoveRun::new),
      "/email-remove-run/*"
    );

    servlets.addServlet(
      servletHolders.create(
        IdUViewEmailVerificationPermit.class,
        IdUViewEmailVerificationPermit::new),
      "/email-verification-permit"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUViewEmailVerificationPermit.class,
        IdUViewEmailVerificationPermit::new),
      "/email-verification-permit/*"
    );

    servlets.addServlet(
      servletHolders.create(
        IdUViewEmailVerificationDeny.class,
        IdUViewEmailVerificationDeny::new),
      "/email-verification-deny"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUViewEmailVerificationDeny.class,
        IdUViewEmailVerificationDeny::new),
      "/email-verification-deny/*"
    );

    servlets.addServlet(
      servletHolders.create(
        IdUViewRealnameUpdate.class,
        IdUViewRealnameUpdate::new),
      "/realname-update"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUViewRealnameUpdate.class,
        IdUViewRealnameUpdate::new),
      "/realname-update/*"
    );

    servlets.addServlet(
      servletHolders.create(
        IdUViewRealnameUpdateRun.class,
        IdUViewRealnameUpdateRun::new),
      "/realname-update-run"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUViewRealnameUpdateRun.class,
        IdUViewRealnameUpdateRun::new),
      "/realname-update-run/*"
    );

    servlets.addServlet(
      servletHolders.create(
        IdUViewPasswordReset.class,
        IdUViewPasswordReset::new),
      "/password-reset"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUViewPasswordReset.class,
        IdUViewPasswordReset::new),
      "/password-reset/*"
    );

    servlets.addServlet(
      servletHolders.create(
        IdUViewPasswordResetRun.class,
        IdUViewPasswordResetRun::new),
      "/password-reset-run"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUViewPasswordResetRun.class,
        IdUViewPasswordResetRun::new),
      "/password-reset-run/*"
    );

    servlets.addServlet(
      servletHolders.create(
        IdUViewPasswordResetConfirm.class,
        IdUViewPasswordResetConfirm::new),
      "/password-reset-confirm"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUViewPasswordResetConfirm.class,
        IdUViewPasswordResetConfirm::new),
      "/password-reset-confirm/*"
    );

    servlets.addServlet(
      servletHolders.create(
        IdUViewPasswordResetConfirmRun.class,
        IdUViewPasswordResetConfirmRun::new),
      "/password-reset-confirm-run"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUViewPasswordResetConfirmRun.class,
        IdUViewPasswordResetConfirmRun::new),
      "/password-reset-confirm-run/*"
    );

    servlets.addServlet(
      servletHolders.create(
        IdUViewPasswordUpdate.class,
        IdUViewPasswordUpdate::new),
      "/password-update"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUViewPasswordUpdate.class,
        IdUViewPasswordUpdate::new),
      "/password-update/*"
    );

    servlets.addServlet(
      servletHolders.create(
        IdUViewPasswordUpdateRun.class,
        IdUViewPasswordUpdateRun::new),
      "/password-update-run"
    );
    servlets.addServlet(
      servletHolders.create(
        IdUViewPasswordUpdateRun.class,
        IdUViewPasswordUpdateRun::new),
      "/password-update-run/*"
    );
  }
}

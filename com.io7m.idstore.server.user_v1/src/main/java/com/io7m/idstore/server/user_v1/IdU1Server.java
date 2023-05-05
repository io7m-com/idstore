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

package com.io7m.idstore.server.user_v1;

import com.io7m.idstore.server.http.IdPlainErrorHandler;
import com.io7m.idstore.server.http.IdRequestUniqueIDs;
import com.io7m.idstore.server.http.IdServletHolders;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationService;
import com.io7m.repetoir.core.RPServiceDirectoryType;
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
 * A user API v1 server.
 */

public final class IdU1Server
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdU1Server.class);

  private IdU1Server()
  {

  }

  /**
   * Create a user API v1 server.
   *
   * @param services The service directory
   *
   * @return A server
   *
   * @throws Exception On errors
   */

  public static Server createUserAPIServer(
    final RPServiceDirectoryType services)
    throws Exception
  {
    final var configurationService =
      services.requireService(IdServerConfigurationService.class);
    final var configuration =
      configurationService.configuration();
    final var httpConfig =
      configuration.userApiAddress();
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
    sessionHandler.setSessionCookie("IDSTORE_USER_API_SESSION");
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
    LOG.info("[{}] User API server started", address);
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
        IdU1Versions.class,
        IdU1Versions::new),
      "/"
    );
    servlets.addServlet(
      servletHolders.create(
        IdU1VersionServlet.class,
        IdU1VersionServlet::new),
      "/version"
    );
    servlets.addServlet(
      servletHolders.create(
        IdU1Login.class,
        IdU1Login::new),
      "/user/1/0/login"
    );
    servlets.addServlet(
      servletHolders.create(
        IdU1CommandServlet.class,
        IdU1CommandServlet::new),
      "/user/1/0/command"
    );
  }
}

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

package com.io7m.idstore.server.internal;

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.protocol.admin_v1.IdA1Messages;
import com.io7m.idstore.protocol.user_v1.IdU1Messages;
import com.io7m.idstore.protocol.versions.IdVMessages;
import com.io7m.idstore.server.api.IdServerConfiguration;
import com.io7m.idstore.server.api.IdServerException;
import com.io7m.idstore.server.api.IdServerType;
import com.io7m.idstore.server.api.events.IdServerEventReady;
import com.io7m.idstore.server.api.events.IdServerEventType;
import com.io7m.idstore.server.internal.admin_v1.IdA1CommandServlet;
import com.io7m.idstore.server.internal.admin_v1.IdA1Login;
import com.io7m.idstore.server.internal.admin_v1.IdA1Sends;
import com.io7m.idstore.server.internal.admin_v1.IdA1Versions;
import com.io7m.idstore.server.internal.common.IdCommonCSSServlet;
import com.io7m.idstore.server.internal.common.IdCommonLogoServlet;
import com.io7m.idstore.server.internal.freemarker.IdFMTemplateService;
import com.io7m.idstore.server.internal.user_v1.IdU1CommandServlet;
import com.io7m.idstore.server.internal.user_v1.IdU1Login;
import com.io7m.idstore.server.internal.user_v1.IdU1Sends;
import com.io7m.idstore.server.internal.user_v1.IdU1Versions;
import com.io7m.idstore.server.internal.user_view.IdUViewEmailAdd;
import com.io7m.idstore.server.internal.user_view.IdUViewEmailAddRun;
import com.io7m.idstore.server.internal.user_view.IdUViewEmailRemoveRun;
import com.io7m.idstore.server.internal.user_view.IdUViewEmailVerificationDeny;
import com.io7m.idstore.server.internal.user_view.IdUViewEmailVerificationPermit;
import com.io7m.idstore.server.internal.user_view.IdUViewLogin;
import com.io7m.idstore.server.internal.user_view.IdUViewLogout;
import com.io7m.idstore.server.internal.user_view.IdUViewMain;
import com.io7m.idstore.server.internal.user_view.IdUViewRealnameUpdate;
import com.io7m.idstore.server.internal.user_view.IdUViewRealnameUpdateRun;
import com.io7m.idstore.server.logging.IdServerRequestLog;
import com.io7m.idstore.services.api.IdServiceDirectory;
import com.io7m.idstore.services.api.IdServiceDirectoryType;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.FileSessionDataStore;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The main server implementation.
 */

public final class IdServer implements IdServerType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdServer.class);

  private final IdServerConfiguration configuration;
  private final CloseableCollectionType<IdServerException> resources;
  private final AtomicBoolean closed;
  private IdDatabaseType database;
  private final SubmissionPublisher<IdServerEventType> events;

  /**
   * The main server implementation.
   *
   * @param inConfiguration The server configuration
   */

  public IdServer(
    final IdServerConfiguration inConfiguration)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.resources =
      CloseableCollection.create(
        () -> {
          return new IdServerException(
            "Server creation failed.",
            "server-creation"
          );
        }
      );

    this.events =
      new SubmissionPublisher<>();
    this.closed =
      new AtomicBoolean(false);
  }

  @Override
  public void start()
    throws IdServerException
  {
    if (this.closed.get()) {
      throw new IllegalStateException("Server is closed!");
    }

    try {
      this.database = this.resources.add(this.createDatabase());

      final var services = this.createServiceDirectory(this.database);
      this.resources.add(services);

      final var userAPIServer = this.createUserAPIServer(services);
      this.resources.add(userAPIServer::stop);

      final var userViewServer = this.createUserViewServer(services);
      this.resources.add(userViewServer::stop);

      final var adminAPIServer = this.createAdminAPIServer(services);
      this.resources.add(adminAPIServer::stop);

      this.events.submit(new IdServerEventReady(this.configuration.now()));
    } catch (final IdDatabaseException e) {
      try {
        this.close();
      } catch (final IdServerException ex) {
        e.addSuppressed(ex);
      }
      throw new IdServerException(e.getMessage(), e, "database");
    } catch (final Exception e) {
      try {
        this.close();
      } catch (final IdServerException ex) {
        e.addSuppressed(ex);
      }
      throw new IdServerException(e.getMessage(), e, "startup");
    }
  }

  @Override
  public IdDatabaseType database()
  {
    return Optional.ofNullable(this.database)
      .orElseThrow(() -> {
        return new IllegalStateException("Server is not started.");
      });
  }

  @Override
  public Flow.Publisher<IdServerEventType> events()
  {
    return this.events;
  }

  private IdServiceDirectory createServiceDirectory(
    final IdDatabaseType inDatabase)
    throws IOException
  {
    final var services = new IdServiceDirectory();
    services.register(IdDatabaseType.class, inDatabase);

    final var eventBus = new IdServerEventBusService(this.events);
    services.register(IdServerEventBusService.class, eventBus);

    final var strings = new IdServerStrings(this.configuration.locale());
    services.register(IdServerStrings.class, strings);

    services.register(
      IdServerMailService.class,
      IdServerMailService.create(
        this.configuration.clock(),
        eventBus,
        this.configuration.mailConfiguration())
    );

    services.register(
      IdUserSessionService.class,
      new IdUserSessionService()
    );

    final var templates = IdFMTemplateService.create();
    services.register(IdFMTemplateService.class, templates);

    services.register(
      IdServerBrandingService.class,
      IdServerBrandingService.create(
        strings, templates, this.configuration.branding())
    );

    final var versionMessages = new IdVMessages();
    services.register(IdVMessages.class, versionMessages);

    final var idU1Messages = new IdU1Messages();
    services.register(IdU1Messages.class, idU1Messages);
    services.register(IdU1Sends.class, new IdU1Sends(idU1Messages));

    final var idA1Messages = new IdA1Messages();
    services.register(IdA1Messages.class, idA1Messages);
    services.register(IdA1Sends.class, new IdA1Sends(idA1Messages));

    final var clock = new IdServerClock(this.configuration.clock());
    services.register(IdServerClock.class, clock);

    final var config = new IdServerConfigurationService(this.configuration);
    services.register(IdServerConfigurationService.class, config);

    services.register(IdRequestLimits.class, new IdRequestLimits(strings));
    return services;
  }

  private Server createUserViewServer(
    final IdServiceDirectoryType services)
    throws Exception
  {
    final var httpConfig =
      this.configuration.userViewAddress();
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

    final var servletHolders =
      new IdServletHolders(services);
    final var servlets =
      new ServletContextHandler();

    servlets.addServlet(
      servletHolders.create(IdUViewMain.class, IdUViewMain::new),
      "/"
    );
    servlets.addServlet(
      servletHolders.create(IdUViewLogout.class, IdUViewLogout::new),
      "/logout"
    );

    servlets.addServlet(
      servletHolders.create(IdUViewLogin.class, IdUViewLogin::new),
      "/login"
    );
    servlets.addServlet(
      servletHolders.create(IdUViewLogin.class, IdUViewLogin::new),
      "/login/*"
    );

    servlets.addServlet(
      servletHolders.create(IdCommonCSSServlet.class, IdCommonCSSServlet::new),
      "/css/*"
    );
    servlets.addServlet(
      servletHolders.create(IdCommonCSSServlet.class, IdCommonCSSServlet::new),
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
      servletHolders.create(IdUViewEmailAdd.class, IdUViewEmailAdd::new),
      "/email-add"
    );
    servlets.addServlet(
      servletHolders.create(IdUViewEmailAdd.class, IdUViewEmailAdd::new),
      "/email-add/*"
    );

    servlets.addServlet(
      servletHolders.create(IdUViewEmailAddRun.class, IdUViewEmailAddRun::new),
      "/email-add-run"
    );
    servlets.addServlet(
      servletHolders.create(IdUViewEmailAddRun.class, IdUViewEmailAddRun::new),
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

    /*
     * Set up a session handler that allows for Servlets to have sessions
     * that can survive server restarts.
     */

    final var sessionIds = new DefaultSessionIdManager(server);
    server.setSessionIdManager(sessionIds);

    final var sessionHandler = new SessionHandler();
    sessionHandler.setSessionCookie("IDSTORE_USER_VIEW_SESSION");

    final var sessionStore = new FileSessionDataStore();
    sessionStore.setStoreDir(httpConfig.sessionDirectory().toFile());

    final var sessionCache = new DefaultSessionCache(sessionHandler);
    sessionCache.setSessionDataStore(sessionStore);

    sessionHandler.setSessionCache(sessionCache);
    sessionHandler.setSessionIdManager(sessionIds);
    sessionHandler.setHandler(servlets);

    /*
     * Set up an MBean container so that the statistics handler can export
     * statistics to JMX.
     */

    final var mbeanContainer =
      new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
    server.addBean(mbeanContainer);

    /*
     * Set up a statistics handler that wraps everything.
     */

    final var statsHandler = new StatisticsHandler();
    statsHandler.setHandler(sessionHandler);

    /*
     * Add a connector listener that adds unique identifiers to all requests.
     */

    Arrays.stream(server.getConnectors()).forEach(
      connector -> connector.addBean(new IdServerRequestDecoration(services))
    );

    server.setErrorHandler(new IdErrorHandler());
    server.setRequestLog(new IdServerRequestLog(services, "user_view"));
    server.setHandler(statsHandler);
    server.start();
    LOG.info("[{}] User view server started", address);
    return server;
  }

  private Server createUserAPIServer(
    final IdServiceDirectoryType services)
    throws Exception
  {
    final var httpConfig =
      this.configuration.userApiAddress();
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

    final var servletHolders =
      new IdServletHolders(services);
    final var servlets =
      new ServletContextHandler();

    servlets.addServlet(
      servletHolders.create(IdU1Versions.class, IdU1Versions::new),
      "/"
    );
    servlets.addServlet(
      servletHolders.create(IdU1Login.class, IdU1Login::new),
      "/user/1/0/login"
    );
    servlets.addServlet(
      servletHolders.create(IdU1CommandServlet.class, IdU1CommandServlet::new),
      "/user/1/0/command"
    );

    /*
     * Set up a session handler that allows for Servlets to have sessions
     * that can survive server restarts.
     */

    final var sessionIds = new DefaultSessionIdManager(server);
    server.setSessionIdManager(sessionIds);

    final var sessionHandler = new SessionHandler();
    sessionHandler.setSessionCookie("IDSTORE_USER_API_SESSION");

    final var sessionStore = new FileSessionDataStore();
    sessionStore.setStoreDir(httpConfig.sessionDirectory().toFile());

    final var sessionCache = new DefaultSessionCache(sessionHandler);
    sessionCache.setSessionDataStore(sessionStore);

    sessionHandler.setSessionCache(sessionCache);
    sessionHandler.setSessionIdManager(sessionIds);
    sessionHandler.setHandler(servlets);

    /*
     * Set up an MBean container so that the statistics handler can export
     * statistics to JMX.
     */

    final var mbeanContainer =
      new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
    server.addBean(mbeanContainer);

    /*
     * Set up a statistics handler that wraps everything.
     */

    final var statsHandler = new StatisticsHandler();
    statsHandler.setHandler(sessionHandler);

    /*
     * Add a connector listener that adds unique identifiers to all requests.
     */

    Arrays.stream(server.getConnectors()).forEach(
      connector -> connector.addBean(new IdServerRequestDecoration(services))
    );

    server.setErrorHandler(new IdErrorHandler());
    server.setRequestLog(new IdServerRequestLog(services, "user_api"));
    server.setHandler(statsHandler);
    server.start();
    LOG.info("[{}] User API server started", address);
    return server;
  }

  private Server createAdminAPIServer(
    final IdServiceDirectoryType services)
    throws Exception
  {
    final var httpConfig =
      this.configuration.adminApiAddress();
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

    final var servletHolders =
      new IdServletHolders(services);
    final var servlets =
      new ServletContextHandler();

    servlets.addServlet(
      servletHolders.create(IdA1Versions.class, IdA1Versions::new),
      "/"
    );
    servlets.addServlet(
      servletHolders.create(IdA1Login.class, IdA1Login::new),
      "/admin/1/0/login"
    );
    servlets.addServlet(
      servletHolders.create(IdA1CommandServlet.class, IdA1CommandServlet::new),
      "/admin/1/0/command"
    );

    /*
     * Set up a session handler that allows for Servlets to have sessions
     * that can survive server restarts.
     */

    final var sessionIds = new DefaultSessionIdManager(server);
    server.setSessionIdManager(sessionIds);

    final var sessionHandler = new SessionHandler();
    sessionHandler.setSessionCookie("IDSTORE_ADMIN_API_SESSION");

    final var sessionStore = new FileSessionDataStore();
    sessionStore.setStoreDir(httpConfig.sessionDirectory().toFile());

    final var sessionCache = new DefaultSessionCache(sessionHandler);
    sessionCache.setSessionDataStore(sessionStore);

    sessionHandler.setSessionCache(sessionCache);
    sessionHandler.setSessionIdManager(sessionIds);
    sessionHandler.setHandler(servlets);

    /*
     * Set up an MBean container so that the statistics handler can export
     * statistics to JMX.
     */

    final var mbeanContainer =
      new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
    server.addBean(mbeanContainer);

    /*
     * Set up a statistics handler that wraps everything.
     */

    final var statsHandler = new StatisticsHandler();
    statsHandler.setHandler(sessionHandler);

    /*
     * Add a connector listener that adds unique identifiers to all requests.
     */

    Arrays.stream(server.getConnectors()).forEach(
      connector -> connector.addBean(new IdServerRequestDecoration(services))
    );

    server.setErrorHandler(new IdErrorHandler());
    server.setRequestLog(new IdServerRequestLog(services, "admin_api"));
    server.setHandler(statsHandler);
    server.start();
    LOG.info("[{}] Admin API server started", address);
    return server;
  }

  private IdDatabaseType createDatabase()
    throws IdDatabaseException
  {
    return this.configuration.databases()
      .open(
        this.configuration.databaseConfiguration(),
        event -> {

        });
  }

  @Override
  public void close()
    throws IdServerException
  {
    if (this.closed.compareAndSet(false, true)) {
      this.resources.close();
    }
  }
}

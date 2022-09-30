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

import com.io7m.idstore.database.api.IdDatabaseAdminsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseConfiguration;
import com.io7m.idstore.database.api.IdDatabaseCreate;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.database.api.IdDatabaseUpgrade;
import com.io7m.idstore.error_codes.IdErrorCode;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.protocol.admin.cb1.IdACB1Messages;
import com.io7m.idstore.protocol.user.cb1.IdUCB1Messages;
import com.io7m.idstore.protocol.versions.IdVMessages;
import com.io7m.idstore.server.api.IdServerConfiguration;
import com.io7m.idstore.server.api.IdServerException;
import com.io7m.idstore.server.api.IdServerType;
import com.io7m.idstore.server.api.events.IdServerEventReady;
import com.io7m.idstore.server.api.events.IdServerEventType;
import com.io7m.idstore.server.internal.admin_v1.IdA1CommandServlet;
import com.io7m.idstore.server.internal.admin_v1.IdA1Login;
import com.io7m.idstore.server.internal.admin_v1.IdA1Versions;
import com.io7m.idstore.server.internal.admin_v1.IdACB1Sends;
import com.io7m.idstore.server.internal.common.IdCommonCSSServlet;
import com.io7m.idstore.server.internal.common.IdCommonLogoServlet;
import com.io7m.idstore.server.internal.freemarker.IdFMTemplateService;
import com.io7m.idstore.server.internal.user_v1.IdU1CommandServlet;
import com.io7m.idstore.server.internal.user_v1.IdU1Login;
import com.io7m.idstore.server.internal.user_v1.IdU1Versions;
import com.io7m.idstore.server.internal.user_v1.IdUCB1Sends;
import com.io7m.idstore.server.internal.user_view.IdUViewEmailAdd;
import com.io7m.idstore.server.internal.user_view.IdUViewEmailAddRun;
import com.io7m.idstore.server.internal.user_view.IdUViewEmailRemoveRun;
import com.io7m.idstore.server.internal.user_view.IdUViewEmailVerificationDeny;
import com.io7m.idstore.server.internal.user_view.IdUViewEmailVerificationPermit;
import com.io7m.idstore.server.internal.user_view.IdUViewLogin;
import com.io7m.idstore.server.internal.user_view.IdUViewLogout;
import com.io7m.idstore.server.internal.user_view.IdUViewMain;
import com.io7m.idstore.server.internal.user_view.IdUViewPasswordReset;
import com.io7m.idstore.server.internal.user_view.IdUViewPasswordResetConfirm;
import com.io7m.idstore.server.internal.user_view.IdUViewPasswordResetConfirmRun;
import com.io7m.idstore.server.internal.user_view.IdUViewPasswordResetRun;
import com.io7m.idstore.server.internal.user_view.IdUViewRealnameUpdate;
import com.io7m.idstore.server.internal.user_view.IdUViewRealnameUpdateRun;
import com.io7m.idstore.server.logging.IdServerRequestLog;
import com.io7m.idstore.services.api.IdServiceDirectory;
import com.io7m.idstore.services.api.IdServiceDirectoryType;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
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
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * The main server implementation.
 */

public final class IdServer implements IdServerType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdServer.class);

  private CloseableCollectionType<IdServerException> resources;
  private IdDatabaseType database;
  private IdServerTelemetryService telemetry;
  private final AtomicBoolean closed;
  private final IdServerConfiguration configuration;
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
      createResourceCollection();
    this.events =
      new SubmissionPublisher<>();
    this.closed =
      new AtomicBoolean(false);
  }

  private static CloseableCollectionType<IdServerException> createResourceCollection()
  {
    return CloseableCollection.create(
      () -> {
        return new IdServerException(
          new IdErrorCode("server-creation"),
          "Server creation failed."
        );
      }
    );
  }

  @Override
  public void start()
    throws IdServerException
  {
    this.closed.set(false);
    this.resources = createResourceCollection();

    this.telemetry =
      IdServerTelemetryService.create(this.configuration);

    final var startupSpan =
      this.telemetry.tracer()
        .spanBuilder("IdServer.start")
        .setSpanKind(SpanKind.INTERNAL)
        .startSpan();

    try {
      this.database =
        this.resources.add(this.createDatabase(this.telemetry.openTelemetry()));

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
      startupSpan.recordException(e);

      try {
        this.close();
      } catch (final IdServerException ex) {
        e.addSuppressed(ex);
      }
      throw new IdServerException(
        new IdErrorCode("database"),
        e.getMessage(),
        e);
    } catch (final Exception e) {
      startupSpan.recordException(e);

      try {
        this.close();
      } catch (final IdServerException ex) {
        e.addSuppressed(ex);
      }
      throw new IdServerException(
        new IdErrorCode("startup"),
        e.getMessage(),
        e
      );
    } finally {
      startupSpan.end();
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
    services.register(IdServerTelemetryService.class, this.telemetry);
    services.register(IdDatabaseType.class, inDatabase);

    final var eventBus = new IdServerEventBusService(this.events);
    services.register(IdServerEventBusService.class, eventBus);

    final var strings = new IdServerStrings(this.configuration.locale());
    services.register(IdServerStrings.class, strings);

    final var mailService =
      IdServerMailService.create(
        this.configuration.clock(),
        this.telemetry,
        eventBus,
        this.configuration.mailConfiguration()
      );
    services.register(IdServerMailService.class, mailService);

    services.register(
      IdUserSessionService.class,
      new IdUserSessionService()
    );

    final var templates = IdFMTemplateService.create();
    services.register(IdFMTemplateService.class, templates);

    final var brandingService =
      IdServerBrandingService.create(
        strings, templates, this.configuration.branding());
    services.register(IdServerBrandingService.class, brandingService);

    final var versionMessages = new IdVMessages();
    services.register(IdVMessages.class, versionMessages);

    final var idA1Messages = new IdACB1Messages();
    services.register(IdACB1Messages.class, idA1Messages);
    services.register(IdACB1Sends.class, new IdACB1Sends(idA1Messages));

    final var idU1Messages = new IdUCB1Messages();
    services.register(IdUCB1Messages.class, idU1Messages);
    services.register(IdUCB1Sends.class, new IdUCB1Sends(idU1Messages));

    final var clock = new IdServerClock(this.configuration.clock());
    services.register(IdServerClock.class, clock);

    final var config = new IdServerConfigurationService(this.configuration);
    services.register(IdServerConfigurationService.class, config);

    final var maintenance =
      IdServerMaintenanceService.create(clock, inDatabase);
    services.register(IdServerMaintenanceService.class, maintenance);

    final var userPasswordRateLimitService =
      IdRateLimitPasswordResetService.create(
        this.telemetry,
        this.configuration.rateLimit()
          .passwordResetRateLimit()
          .toSeconds(),
        SECONDS
      );

    services.register(
      IdRateLimitPasswordResetService.class,
      userPasswordRateLimitService
    );

    final var emailVerificationRateLimitService =
      IdRateLimitEmailVerificationService.create(
        this.telemetry,
        this.configuration.rateLimit()
          .emailVerificationRateLimit()
          .toSeconds(),
        SECONDS
      );

    services.register(
      IdRateLimitEmailVerificationService.class,
      emailVerificationRateLimitService
    );

    final var userPasswordResetService =
      IdUserPasswordResetService.create(
        this.telemetry,
        brandingService,
        templates,
        mailService,
        this.configuration,
        clock,
        this.database,
        strings,
        userPasswordRateLimitService
      );
    services.register(
      IdUserPasswordResetService.class,
      userPasswordResetService
    );

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

    createUserViewServerServlets(servletHolders, servlets);

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

  private static void createUserViewServerServlets(
    final IdServletHolders servletHolders,
    final ServletContextHandler servlets)
  {
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

  private IdDatabaseType createDatabase(
    final OpenTelemetry openTelemetry)
    throws IdDatabaseException
  {
    return this.configuration.databases()
      .open(
        this.configuration.databaseConfiguration(),
        openTelemetry,
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

  @Override
  public void setup(
    final Optional<UUID> adminId,
    final IdName adminName,
    final IdEmail adminEmail,
    final IdRealName adminRealName,
    final String adminPassword)
    throws IdServerException
  {
    Objects.requireNonNull(adminId, "adminId");
    Objects.requireNonNull(adminName, "adminName");
    Objects.requireNonNull(adminEmail, "adminEmail");
    Objects.requireNonNull(adminRealName, "adminRealName");
    Objects.requireNonNull(adminPassword, "adminPassword");

    try {
      this.closed.set(false);
      this.resources = createResourceCollection();

      this.telemetry =
        IdServerTelemetryService.create(this.configuration);

      final var baseConfiguration =
        this.configuration.databaseConfiguration();

      final var setupConfiguration =
        new IdDatabaseConfiguration(
          baseConfiguration.user(),
          baseConfiguration.password(),
          baseConfiguration.address(),
          baseConfiguration.port(),
          baseConfiguration.databaseName(),
          IdDatabaseCreate.CREATE_DATABASE,
          IdDatabaseUpgrade.UPGRADE_DATABASE,
          baseConfiguration.clock()
        );

      final var db =
        this.resources.add(
          this.configuration.databases()
            .open(
              setupConfiguration,
              this.telemetry.openTelemetry(),
              event -> {
              }));

      final var password =
        IdPasswordAlgorithmPBKDF2HmacSHA256.create()
          .createHashed(adminPassword);

      try (var connection = db.openConnection(IDSTORE)) {
        try (var transaction = connection.openTransaction()) {
          final var admins =
            transaction.queries(IdDatabaseAdminsQueriesType.class);

          admins.adminCreateInitial(
            adminId.orElse(UUID.randomUUID()),
            adminName,
            adminRealName,
            adminEmail,
            OffsetDateTime.now(baseConfiguration.clock()),
            password
          );
          transaction.commit();
        }
      }
    } catch (final IdDatabaseException | IdPasswordException e) {
      throw new IdServerException(e.errorCode(), e.getMessage(), e);
    }
  }
}

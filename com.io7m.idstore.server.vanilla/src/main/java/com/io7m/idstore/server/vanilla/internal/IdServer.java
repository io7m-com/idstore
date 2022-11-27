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

package com.io7m.idstore.server.vanilla.internal;

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
import com.io7m.idstore.protocol.admin.cb.IdACB1Messages;
import com.io7m.idstore.protocol.user.cb.IdUCB1Messages;
import com.io7m.idstore.server.admin_v1.IdA1Server;
import com.io7m.idstore.server.admin_v1.IdACB1Sends;
import com.io7m.idstore.server.api.IdServerConfiguration;
import com.io7m.idstore.server.api.IdServerException;
import com.io7m.idstore.server.api.IdServerType;
import com.io7m.idstore.server.controller.IdServerStrings;
import com.io7m.idstore.server.controller.admin.IdAdminLoginService;
import com.io7m.idstore.server.controller.user.IdUserLoginService;
import com.io7m.idstore.server.controller.user_pwreset.IdUserPasswordResetService;
import com.io7m.idstore.server.controller.user_pwreset.IdUserPasswordResetServiceType;
import com.io7m.idstore.server.service.branding.IdServerBrandingService;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.clock.IdServerClock;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationService;
import com.io7m.idstore.server.service.mail.IdServerMailService;
import com.io7m.idstore.server.service.mail.IdServerMailServiceType;
import com.io7m.idstore.server.service.ratelimit.IdRateLimitEmailVerificationService;
import com.io7m.idstore.server.service.ratelimit.IdRateLimitEmailVerificationServiceType;
import com.io7m.idstore.server.service.ratelimit.IdRateLimitPasswordResetService;
import com.io7m.idstore.server.service.ratelimit.IdRateLimitPasswordResetServiceType;
import com.io7m.idstore.server.service.reqlimit.IdRequestLimits;
import com.io7m.idstore.server.service.sessions.IdSessionAdminService;
import com.io7m.idstore.server.service.sessions.IdSessionUserService;
import com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryNoOp;
import com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceFactoryType;
import com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType;
import com.io7m.idstore.server.service.templating.IdFMTemplateService;
import com.io7m.idstore.server.service.templating.IdFMTemplateServiceType;
import com.io7m.idstore.server.service.verdant.IdVerdantMessages;
import com.io7m.idstore.server.user_v1.IdU1Server;
import com.io7m.idstore.server.user_v1.IdUCB1Sends;
import com.io7m.idstore.server.user_view.IdUVServer;
import com.io7m.idstore.services.api.IdServiceDirectory;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import org.eclipse.jetty.server.Server;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * The basic server frontend.
 */

public final class IdServer implements IdServerType
{
  private final IdServerConfiguration configuration;
  private CloseableCollectionType<IdServerException> resources;
  private final AtomicBoolean stopped;
  private IdServerTelemetryServiceType telemetry;
  private IdDatabaseType database;

  /**
   * The basic server frontend.
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
    this.stopped =
      new AtomicBoolean(true);
  }

  @Override
  public void start()
    throws IdServerException
  {
    try {
      if (this.stopped.compareAndSet(true, false)) {
        this.resources = createResourceCollection();
        this.telemetry = this.createTelemetry();

        final var startupSpan =
          this.telemetry.tracer()
            .spanBuilder("IdServer.start")
            .setSpanKind(SpanKind.INTERNAL)
            .startSpan();

        try {
          this.database =
            this.resources.add(this.createDatabase(this.telemetry.openTelemetry()));
          final var services =
            this.resources.add(this.createServiceDirectory(this.database));

          final Server userView = IdUVServer.createUserViewServer(services);
          this.resources.add(userView::stop);

          final Server userAPI = IdU1Server.createUserAPIServer(services);
          this.resources.add(userAPI::stop);

          final Server adminAPI = IdA1Server.createAdminAPIServer(services);
          this.resources.add(adminAPI::stop);
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
    } catch (final Throwable e) {
      this.close();
      throw e;
    }
  }

  private IdServiceDirectory createServiceDirectory(
    final IdDatabaseType newDatabase)
    throws IOException
  {
    final var services = new IdServiceDirectory();
    services.register(IdServerTelemetryServiceType.class, this.telemetry);
    services.register(IdDatabaseType.class, newDatabase);

    final var strings = new IdServerStrings(this.configuration.locale());
    services.register(IdServerStrings.class, strings);

    final var mailService =
      IdServerMailService.create(
        this.telemetry,
        this.configuration.mailConfiguration()
      );
    services.register(IdServerMailServiceType.class, mailService);

    final var sessionAdminService =
      new IdSessionAdminService(
      this.telemetry.openTelemetry(),
      this.configuration.sessions().adminSessionExpiration()
      );

    services.register(
      IdSessionAdminService.class,
      sessionAdminService
    );

    final var sessionUserService =
      new IdSessionUserService(
      this.telemetry.openTelemetry(),
      this.configuration.sessions().userSessionExpiration()
      );

    services.register(
      IdSessionUserService.class,
      sessionUserService
    );

    final var config = new IdServerConfigurationService(this.configuration);
    services.register(IdServerConfigurationService.class, config);

    final var clock = new IdServerClock(this.configuration.clock());
    services.register(IdServerClock.class, clock);

    services.register(
      IdUserLoginService.class,
      new IdUserLoginService(clock, strings, sessionUserService, config)
    );

    services.register(
      IdAdminLoginService.class,
      new IdAdminLoginService(clock, strings, sessionAdminService)
    );

    final var templates = IdFMTemplateService.create();
    services.register(IdFMTemplateServiceType.class, templates);

    final var brandingService =
      IdServerBrandingService.create(templates, this.configuration.branding());
    services.register(IdServerBrandingServiceType.class, brandingService);

    final var vMessages = new IdVerdantMessages();
    services.register(IdVerdantMessages.class, vMessages);

    final var idA1Messages = new IdACB1Messages();
    services.register(IdACB1Messages.class, idA1Messages);
    services.register(IdACB1Sends.class, new IdACB1Sends(idA1Messages));

    final var idU1Messages = new IdUCB1Messages();
    services.register(IdUCB1Messages.class, idU1Messages);
    services.register(IdUCB1Sends.class, new IdUCB1Sends(idU1Messages));

    final var userPasswordRateLimitService =
      IdRateLimitPasswordResetService.create(
        this.telemetry,
        this.configuration.rateLimit()
          .passwordResetRateLimit()
          .toSeconds(),
        SECONDS
      );

    services.register(
      IdRateLimitPasswordResetServiceType.class,
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
      IdRateLimitEmailVerificationServiceType.class,
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
      IdUserPasswordResetServiceType.class,
      userPasswordResetService
    );

    services.register(IdRequestLimits.class, new IdRequestLimits(size -> {
      return strings.format("requestTooLarge", size);
    }));
    return services;
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

  private IdServerTelemetryServiceType createTelemetry()
  {
    return this.configuration.openTelemetry()
      .flatMap(config -> {
        final var loader =
          ServiceLoader.load(IdServerTelemetryServiceFactoryType.class);
        return loader.findFirst().map(f -> f.create(config));
      }).orElseGet(IdServerTelemetryNoOp::noop);
  }

  @Override
  public IdDatabaseType database()
  {
    if (this.stopped.get()) {
      throw new IllegalStateException("Server is not started.");
    }

    return this.database;
  }

  @Override
  public void close()
    throws IdServerException
  {
    if (this.stopped.compareAndSet(false, true)) {
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

    if (this.stopped.compareAndSet(true, false)) {
      try {
        this.resources = createResourceCollection();
        this.telemetry = this.createTelemetry();

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
      } finally {
        this.close();
      }
    } else {
      throw new IllegalStateException("Server must be closed before setup.");
    }
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
}

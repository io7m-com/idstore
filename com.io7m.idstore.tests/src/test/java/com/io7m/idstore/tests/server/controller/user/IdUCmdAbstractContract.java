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

package com.io7m.idstore.tests.server.controller.user;

import com.io7m.idstore.database.api.IdDatabaseTransactionType;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdNonEmptyList;
import com.io7m.idstore.model.IdPassword;
import com.io7m.idstore.model.IdPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.server.api.IdServerConfigurations;
import com.io7m.idstore.server.controller.user.IdUCommandContext;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.clock.IdServerClock;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationFiles;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationService;
import com.io7m.idstore.server.service.mail.IdServerMailServiceType;
import com.io7m.idstore.server.service.ratelimit.IdRateLimitEmailVerificationServiceType;
import com.io7m.idstore.server.service.sessions.IdSessionSecretIdentifier;
import com.io7m.idstore.server.service.sessions.IdSessionUser;
import com.io7m.idstore.server.service.telemetry.api.IdEventServiceType;
import com.io7m.idstore.server.service.telemetry.api.IdMetricsServiceType;
import com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryNoOp;
import com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType;
import com.io7m.idstore.server.service.templating.IdFMTemplateServiceType;
import com.io7m.idstore.strings.IdStrings;
import com.io7m.idstore.tests.IdFakeClock;
import com.io7m.idstore.tests.IdTestDirectories;
import com.io7m.idstore.tests.server.api.IdServerConfigurationsTest;
import com.io7m.repetoir.core.RPServiceDirectory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.internal.verification.Times;

import java.nio.file.Path;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.mockito.Mockito.mock;

public abstract class IdUCmdAbstractContract
{
  private RPServiceDirectory services;
  private IdDatabaseTransactionType transaction;
  private IdFakeClock clock;
  private IdServerClock serverClock;
  private IdStrings strings;
  private OffsetDateTime timeStart;
  private IdFMTemplateServiceType templates;
  private Path directory;
  private IdServerConfigurationService configurations;
  private IdServerMailServiceType mail;
  private IdServerBrandingServiceType branding;
  private IdRateLimitEmailVerificationServiceType rateLimit;
  private IdEventServiceType eventService;
  private IdMetricsServiceType metrics;

  protected final Times once()
  {
    return new Times(1);
  }

  protected final Times twice()
  {
    return new Times(2);
  }

  protected final IdPassword password()
  {
    try {
      return IdPasswordAlgorithmPBKDF2HmacSHA256.create().createHashed("x");
    } catch (final IdPasswordException e) {
      throw new IllegalStateException(e);
    }
  }

  protected final OffsetDateTime timeStart()
  {
    return this.timeStart;
  }

  protected final IdUser createUser(
    final String name)
  {
    return new IdUser(
      UUID.randomUUID(),
      new IdName(name),
      new IdRealName("User " + name),
      IdNonEmptyList.single(new IdEmail(name + "@example.com")),
      OffsetDateTime.now(),
      OffsetDateTime.now(),
      this.password()
    );
  }

  protected final IdEventServiceType events()
  {
    return this.eventService;
  }

  @BeforeEach
  protected final void commandSetup()
    throws Exception
  {
    this.directory =
      IdTestDirectories.createTempDirectory();

    final var file =
      IdTestDirectories.resourceOf(
        IdServerConfigurationsTest.class,
        this.directory,
        "server-config-0.xml"
      );

    final var configFile =
      new IdServerConfigurationFiles()
        .parse(file);

    final var configuration =
      IdServerConfigurations.ofFile(
        Locale.getDefault(),
        Clock.systemUTC(),
        configFile
      );

    this.services =
      new RPServiceDirectory();
    this.transaction =
      mock(IdDatabaseTransactionType.class);

    this.clock =
      new IdFakeClock();
    this.serverClock =
      new IdServerClock(this.clock);
    this.timeStart =
      this.serverClock.now();
    this.strings =
      IdStrings.create(Locale.ROOT);
    this.templates =
      mock(IdFMTemplateServiceType.class);
    this.metrics =
      mock(IdMetricsServiceType.class);
    this.configurations =
      new IdServerConfigurationService(this.metrics, configuration);
    this.mail =
      mock(IdServerMailServiceType.class);
    this.branding =
      mock(IdServerBrandingServiceType.class);
    this.rateLimit =
      mock(IdRateLimitEmailVerificationServiceType.class);
    this.eventService =
      mock(IdEventServiceType.class);

    this.services.register(
      IdServerClock.class,
      this.serverClock
    );

    this.services.register(
      IdEventServiceType.class,
      this.eventService
    );

    this.services.register(
      IdStrings.class,
      this.strings
    );

    this.services.register(
      IdServerTelemetryServiceType.class,
      IdServerTelemetryNoOp.noop()
    );

    this.services.register(
      IdFMTemplateServiceType.class,
      this.templates
    );

    this.services.register(
      IdServerConfigurationService.class,
      this.configurations
    );

    this.services.register(
      IdServerMailServiceType.class,
      this.mail
    );

    this.services.register(
      IdServerBrandingServiceType.class,
      this.branding
    );

    this.services.register(
      IdRateLimitEmailVerificationServiceType.class,
      this.rateLimit
    );
  }

  @AfterEach
  protected final void commandTearDown()
    throws Exception
  {
    this.services.close();
  }

  protected final RPServiceDirectory services()
  {
    return this.services;
  }

  protected final IdDatabaseTransactionType transaction()
  {
    return this.transaction;
  }

  protected final IdUCommandContext createContextAndSession(
    final IdUser user)
  {
    return this.createContext(
      new IdSessionUser(user.id(), IdSessionSecretIdentifier.generate()),
      user
    );
  }

  protected final IdFMTemplateServiceType templates()
  {
    return this.templates;
  }

  protected final IdServerConfigurationService configurations()
  {
    return this.configurations;
  }

  protected final IdServerMailServiceType mail()
  {
    return this.mail;
  }

  protected final IdServerBrandingServiceType branding()
  {
    return this.branding;
  }

  protected final IdRateLimitEmailVerificationServiceType rateLimit()
  {
    return this.rateLimit;
  }

  protected final IdUCommandContext createContext(
    final IdSessionUser session,
    final IdUser user)
  {
    return new IdUCommandContext(
      this.services,
      UUID.randomUUID(),
      this.transaction,
      session,
      user,
      "localhost",
      "NCSA Mosaic"
    );
  }

  protected final IdUser createUserAndSessionWithEmails(
    final String name)
  {
    final var user0 = this.createUser(name);
    return new IdUser(
      user0.id(),
      user0.idName(),
      user0.realName(),
      IdNonEmptyList.ofList(
        List.of(
          user0.emails().first(),
          new IdEmail(name + "-e1@example.com"),
          new IdEmail(name + "-e2@example.com"),
          new IdEmail(name + "-e3@example.com"),
          new IdEmail(name + "-e4@example.com")
        )
      ),
      user0.timeCreated(),
      user0.timeUpdated(),
      user0.password()
    );
  }
}

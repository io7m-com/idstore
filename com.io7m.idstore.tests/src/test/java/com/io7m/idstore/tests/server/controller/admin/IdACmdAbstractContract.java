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

package com.io7m.idstore.tests.server.controller.admin;

import com.io7m.idstore.database.api.IdDatabaseTransactionType;
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdAdminPermissionSet;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdNonEmptyList;
import com.io7m.idstore.model.IdPassword;
import com.io7m.idstore.model.IdPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.server.api.IdServerConfiguration;
import com.io7m.idstore.server.api.IdServerConfigurations;
import com.io7m.idstore.server.controller.IdServerStrings;
import com.io7m.idstore.server.controller.admin.IdACommandContext;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.clock.IdServerClock;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationFiles;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationService;
import com.io7m.idstore.server.service.mail.IdServerMailServiceType;
import com.io7m.idstore.server.service.maintenance.IdClosedForMaintenanceService;
import com.io7m.idstore.server.service.sessions.IdSessionAdmin;
import com.io7m.idstore.server.service.sessions.IdSessionSecretIdentifier;
import com.io7m.idstore.server.service.telemetry.api.IdMetricsServiceType;
import com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryNoOp;
import com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType;
import com.io7m.idstore.server.service.templating.IdFMTemplateServiceType;
import com.io7m.idstore.tests.IdFakeClock;
import com.io7m.idstore.tests.IdTestDirectories;
import com.io7m.repetoir.core.RPServiceDirectory;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.internal.verification.Times;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

import static org.mockito.Mockito.mock;

public abstract class IdACmdAbstractContract
{
  private RPServiceDirectory services;
  private IdDatabaseTransactionType transaction;
  private IdFakeClock clock;
  private IdServerClock serverClock;
  private IdServerStrings strings;
  private OffsetDateTime timeStart;
  private Path directory;
  private Path configFile;
  private IdServerConfiguration configuration;
  private IdServerConfigurationService configurationService;
  private IdMetricsServiceType metrics;
  private IdFMTemplateServiceType templates;
  private IdServerMailServiceType mail;
  private IdServerBrandingServiceType branding;
  private IdClosedForMaintenanceService maintenance;

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
      return IdPasswordAlgorithmPBKDF2HmacSHA256.create()
        .createHashed("x")
        .withExpirationDate(OffsetDateTime.parse("1970-01-01T00:30:02Z"));
    } catch (final IdPasswordException e) {
      throw new IllegalStateException(e);
    }
  }

  protected final OffsetDateTime timeStart()
  {
    return this.timeStart;
  }

  protected final IdAdmin createAdmin(
    final String name,
    final IdAdminPermissionSet permissions)
  {
    return new IdAdmin(
      UUID.randomUUID(),
      new IdName(name),
      new IdRealName("Admin " + name),
      IdNonEmptyList.single(new IdEmail(name + "@example.com")),
      OffsetDateTime.now(),
      OffsetDateTime.now(),
      this.password(),
      permissions
    );
  }

  protected final IdUser createUser(
    final String name)
  {
    return new IdUser(
      UUID.randomUUID(),
      new IdName(name),
      new IdRealName("Admin " + name),
      IdNonEmptyList.single(new IdEmail(name + "@example.com")),
      OffsetDateTime.now(),
      OffsetDateTime.now(),
      this.password()
    );
  }

  @BeforeEach
  protected final void commandSetup()
    throws Exception
  {
    this.directory =
      IdTestDirectories.createTempDirectory();

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
      new IdServerStrings(Locale.ROOT);

    this.configFile =
      IdTestDirectories.resourceOf(
        IdACmdAbstractContract.class,
        this.directory,
        "server-config-0.xml"
      );

    this.configuration =
      IdServerConfigurations.ofFile(
        Locale.ROOT,
        this.clock,
        new IdServerConfigurationFiles().parse(this.configFile)
      );

    this.maintenance =
      mock(IdClosedForMaintenanceService.class);
    this.branding =
      mock(IdServerBrandingServiceType.class);
    this.metrics =
      mock(IdMetricsServiceType.class);
    this.templates =
      mock(IdFMTemplateServiceType.class);
    this.mail =
      mock(IdServerMailServiceType.class);
    this.configurationService =
      new IdServerConfigurationService(this.metrics, this.configuration);

    this.services.register(
      IdClosedForMaintenanceService.class,
      this.maintenance
    );
    this.services.register(
      IdServerBrandingServiceType.class,
      this.branding
    );
    this.services.register(
      IdServerMailServiceType.class,
      this.mail
    );
    this.services.register(
      IdFMTemplateServiceType.class,
      this.templates
    );
    this.services.register(
      IdServerConfigurationService.class,
      this.configurationService
    );
    this.services.register(
      IdServerClock.class,
      this.serverClock
    );
    this.services.register(
      IdServerStrings.class,
      this.strings
    );
    this.services.register(
      IdServerTelemetryServiceType.class,
      IdServerTelemetryNoOp.noop()
    );
  }

  @AfterEach
  protected final void commandTearDown()
    throws Exception
  {
    IdTestDirectories.deleteDirectory(this.directory);
    this.services.close();
  }

  protected final RPServiceDirectoryType services()
  {
    return this.services;
  }

  protected final IdDatabaseTransactionType transaction()
  {
    return this.transaction;
  }

  protected final IdClosedForMaintenanceService maintenance()
  {
    return this.maintenance;
  }

  protected final IdACommandContext createContextAndSession(
    final IdAdmin admin)
  {
    return this.createContext(
      new IdSessionAdmin(admin.id(), IdSessionSecretIdentifier.generate()),
      admin
    );
  }

  protected final IdACommandContext createContext(
    final IdSessionAdmin session,
    final IdAdmin admin)
  {
    return new IdACommandContext(
      this.services,
      UUID.randomUUID(),
      this.transaction,
      session,
      "localhost",
      "NCSA Mosaic",
      admin
    );
  }

  protected final IdServerBrandingServiceType branding()
  {
    return this.branding;
  }

  protected final IdFMTemplateServiceType templates()
  {
    return this.templates;
  }

  protected final IdServerMailServiceType mail()
  {
    return this.mail;
  }
}

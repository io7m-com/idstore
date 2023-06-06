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

import com.io7m.idstore.database.api.IdDatabaseAdminsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseTransactionType;
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdAdminPermissionSet;
import com.io7m.idstore.model.IdBan;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdNonEmptyList;
import com.io7m.idstore.model.IdPassword;
import com.io7m.idstore.model.IdPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.server.api.IdServerConfigurations;
import com.io7m.idstore.server.controller.IdServerStrings;
import com.io7m.idstore.server.controller.admin.IdAdminLoginService;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.service.clock.IdServerClock;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationFiles;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationService;
import com.io7m.idstore.server.service.events.IdEventAdminLoggedIn;
import com.io7m.idstore.server.service.events.IdEventAdminLoginAuthenticationFailed;
import com.io7m.idstore.server.service.events.IdEventAdminLoginRateLimitExceeded;
import com.io7m.idstore.server.service.events.IdEventServiceType;
import com.io7m.idstore.server.service.ratelimit.IdRateLimitAdminLoginServiceType;
import com.io7m.idstore.server.service.sessions.IdSessionAdminService;
import com.io7m.idstore.tests.IdFakeClock;
import com.io7m.idstore.tests.IdTestDirectories;
import com.io7m.idstore.tests.server.api.IdServerConfigurationsTest;
import com.io7m.idstore.tests.server.service.IdServiceContract;
import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.verification.Times;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_NONEXISTENT;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.AUTHENTICATION_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.BANNED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.RATE_LIMIT_EXCEEDED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SQL_ERROR;
import static java.util.Optional.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public final class IdAdminLoginServiceTest extends IdServiceContract<IdAdminLoginService>
{
  private IdFakeClock clock;
  private IdServerClock serverClock;
  private IdServerStrings strings;
  private IdSessionAdminService sessions;
  private IdAdminLoginService login;
  private IdDatabaseTransactionType transaction;
  private IdDatabaseAdminsQueriesType admins;
  private IdEventServiceType events;
  private IdRateLimitAdminLoginServiceType rateLimit;
  private Path directory;
  private IdServerConfigurationService configurationService;

  private static Times once()
  {
    return new Times(1);
  }

  private IdPassword password()
  {
    try {
      return IdPasswordAlgorithmPBKDF2HmacSHA256.create().createHashed("x");
    } catch (final IdPasswordException e) {
      throw new IllegalStateException(e);
    }
  }

  private IdAdmin createAdmin(
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

  @BeforeEach
  public void setup()
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

    this.clock =
      new IdFakeClock();
    this.serverClock =
      new IdServerClock(this.clock);
    this.strings =
      new IdServerStrings(Locale.ROOT);
    this.sessions =
      new IdSessionAdminService(
        OpenTelemetry.noop().getMeter("com.io7m.idstore"),
        Duration.ofDays(1L)
      );
    this.events =
      mock(IdEventServiceType.class);
    this.rateLimit =
      mock(IdRateLimitAdminLoginServiceType.class);
    this.configurationService =
      new IdServerConfigurationService(configuration);

    this.login =
      new IdAdminLoginService(
        this.serverClock,
        this.strings,
        this.sessions,
        this.rateLimit,
        this.events
      );

    this.transaction =
      mock(IdDatabaseTransactionType.class);
    this.admins =
      mock(IdDatabaseAdminsQueriesType.class);

    when(this.transaction.queries(IdDatabaseAdminsQueriesType.class))
      .thenReturn(this.admins);
  }

  @AfterEach
  public void tearDown()
    throws IOException
  {
    IdTestDirectories.deleteDirectory(this.directory);
  }

  /**
   * Nonexistent users cannot log in.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserNonexistent()
    throws Exception
  {
    when(this.rateLimit.isAllowedByRateLimit(any()))
      .thenReturn(Boolean.TRUE);
    when(this.admins.adminGetForNameRequire(any()))
      .thenThrow(new IdDatabaseException(
        "",
        ADMIN_NONEXISTENT,
        Map.of(),
        empty()));

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        this.login.adminLogin(
          this.transaction,
          UUID.randomUUID(),
          "www.example.com",
          "nonexistent",
          "password",
          Map.of()
        );
      });

    assertEquals(AUTHENTICATION_ERROR, ex.errorCode());

    verify(this.admins, once()).adminGetForNameRequire(any());
    verifyNoMoreInteractions(this.admins);
    verifyNoMoreInteractions(this.events);
  }

  /**
   * Database errors fail logins.
   *
   * @throws Exception On errors
   */

  @Test
  public void testDatabaseError0()
    throws Exception
  {
    when(this.rateLimit.isAllowedByRateLimit(any()))
      .thenReturn(Boolean.TRUE);
    when(this.admins.adminGetForNameRequire(any()))
      .thenThrow(new IdDatabaseException("", SQL_ERROR, Map.of(), empty()));

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        this.login.adminLogin(
          this.transaction,
          UUID.randomUUID(),
          "www.example.com",
          "nonexistent",
          "password",
          Map.of()
        );
      });

    assertEquals(SQL_ERROR, ex.errorCode());

    verify(this.admins, once()).adminGetForNameRequire(any());
    verifyNoMoreInteractions(this.admins);
    verifyNoMoreInteractions(this.events);
  }

  /**
   * Banned users cannot log in.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserBanned()
    throws Exception
  {
    when(this.rateLimit.isAllowedByRateLimit(any()))
      .thenReturn(Boolean.TRUE);

    final var admin =
      this.createAdmin("admin", IdAdminPermissionSet.empty());
    final var ban =
      new IdBan(admin.id(), "No reason.", Optional.empty());

    when(this.admins.adminGetForNameRequire(any()))
      .thenReturn(admin);
    when(this.admins.adminBanGet(any()))
      .thenReturn(Optional.of(ban));

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        this.login.adminLogin(
          this.transaction,
          UUID.randomUUID(),
          "www.example.com",
          "admin",
          "password",
          Map.of()
        );
      });

    assertEquals(BANNED, ex.errorCode());

    verify(this.admins, once()).adminGetForNameRequire(any());
    verify(this.admins, once()).adminBanGet(any());
    verifyNoMoreInteractions(this.admins);
    verifyNoMoreInteractions(this.events);
  }

  /**
   * Banned users cannot log in.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserBannedNotExpired()
    throws Exception
  {
    when(this.rateLimit.isAllowedByRateLimit(any()))
      .thenReturn(Boolean.TRUE);

    final var admin =
      this.createAdmin("admin", IdAdminPermissionSet.empty());
    final var ban =
      new IdBan(
        admin.id(),
        "No reason.",
        Optional.of(this.serverClock.now().plusHours(1L)));

    when(this.admins.adminGetForNameRequire(any()))
      .thenReturn(admin);
    when(this.admins.adminBanGet(any()))
      .thenReturn(Optional.of(ban));

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        this.login.adminLogin(
          this.transaction,
          UUID.randomUUID(),
          "www.example.com",
          "admin",
          "password",
          Map.of()
        );
      });

    assertEquals(BANNED, ex.errorCode());

    verify(this.admins, once()).adminGetForNameRequire(any());
    verify(this.admins, once()).adminBanGet(any());
    verifyNoMoreInteractions(this.admins);
    verifyNoMoreInteractions(this.events);
  }

  /**
   * Incorrect passwords fail.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserWrongPassword()
    throws Exception
  {
    when(this.rateLimit.isAllowedByRateLimit(any()))
      .thenReturn(Boolean.TRUE);

    final var admin =
      this.createAdmin("admin", IdAdminPermissionSet.empty());

    when(this.admins.adminGetForNameRequire(any()))
      .thenReturn(admin);
    when(this.admins.adminBanGet(any()))
      .thenReturn(Optional.empty());

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        this.login.adminLogin(
          this.transaction,
          UUID.randomUUID(),
          "www.example.com",
          "admin",
          "not the password",
          Map.of()
        );
      });

    assertEquals(AUTHENTICATION_ERROR, ex.errorCode());

    verify(this.admins, once()).adminGetForNameRequire(any());
    verify(this.admins, once()).adminBanGet(any());
    verifyNoMoreInteractions(this.admins);

    verify(this.events, once())
      .emit(new IdEventAdminLoginAuthenticationFailed(
        "www.example.com",
        admin.id()));
    verifyNoMoreInteractions(this.events);
  }

  /**
   * Correct passwords succeed.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserCorrectPassword()
    throws Exception
  {
    when(this.rateLimit.isAllowedByRateLimit(any()))
      .thenReturn(Boolean.TRUE);

    final var admin =
      this.createAdmin("admin", IdAdminPermissionSet.empty());

    when(this.admins.adminGetForNameRequire(any()))
      .thenReturn(admin);
    when(this.admins.adminBanGet(any()))
      .thenReturn(Optional.empty());

    final var loggedIn =
      this.login.adminLogin(
        this.transaction,
        UUID.randomUUID(),
        "www.example.com",
        "admin",
        "x",
        Map.of()
      );

    assertTrue(this.sessions.findSession(loggedIn.session().id()).isPresent());
    assertEquals(admin.withRedactedPassword(), loggedIn.admin());

    verify(this.admins, once()).adminGetForNameRequire(any());
    verify(this.admins, once()).adminBanGet(any());
    verify(this.admins, once()).adminLogin(any(), any());
    verifyNoMoreInteractions(this.admins);

    verify(this.events, once())
      .emit(new IdEventAdminLoggedIn(admin.id()));
    verifyNoMoreInteractions(this.events);
  }

  /**
   * Rate limiting rejects logins.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserRateLimited()
    throws Exception
  {
    when(this.rateLimit.isAllowedByRateLimit(any()))
      .thenReturn(Boolean.FALSE);

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        this.login.adminLogin(
          this.transaction,
          UUID.randomUUID(),
          "127.0.0.1",
          "admin",
          "x",
          Map.of()
        );
      });

    assertEquals(RATE_LIMIT_EXCEEDED, ex.errorCode());
    verifyNoMoreInteractions(this.admins);

    verify(this.events, once())
      .emit(new IdEventAdminLoginRateLimitExceeded("127.0.0.1", "admin"));

    verifyNoMoreInteractions(this.events);
  }

  @Override
  protected IdAdminLoginService createInstanceA()
  {
    return new IdAdminLoginService(
      this.serverClock,
      this.strings,
      this.sessions,
      this.rateLimit,
      this.events
    );
  }

  @Override
  protected IdAdminLoginService createInstanceB()
  {
    return new IdAdminLoginService(
      this.serverClock,
      this.strings,
      this.sessions,
      this.rateLimit,
      this.events
    );
  }
}

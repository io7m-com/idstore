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

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseTransactionType;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.model.IdBan;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdNonEmptyList;
import com.io7m.idstore.model.IdPassword;
import com.io7m.idstore.model.IdPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.server.api.IdServerConfigurations;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.controller.user.IdUserLoginService;
import com.io7m.idstore.server.service.clock.IdServerClock;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationParsers;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationService;
import com.io7m.idstore.server.service.ratelimit.IdRateLimitUserLoginServiceType;
import com.io7m.idstore.server.service.sessions.IdSessionUserService;
import com.io7m.idstore.server.service.telemetry.api.IdEventServiceType;
import com.io7m.idstore.server.service.telemetry.api.IdEventUserLoggedIn;
import com.io7m.idstore.server.service.telemetry.api.IdEventUserLoginAuthenticationFailed;
import com.io7m.idstore.server.service.telemetry.api.IdEventUserLoginRateLimitExceeded;
import com.io7m.idstore.server.service.telemetry.api.IdMetricsService;
import com.io7m.idstore.server.service.telemetry.api.IdMetricsServiceType;
import com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryNoOp;
import com.io7m.idstore.strings.IdStrings;
import com.io7m.idstore.tests.IdFakeClock;
import com.io7m.idstore.tests.IdTestDirectories;
import com.io7m.idstore.tests.server.api.IdServerConfigurationsTest;
import com.io7m.idstore.tests.server.service.IdServiceContract;
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

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.AUTHENTICATION_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.BANNED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.RATE_LIMIT_EXCEEDED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SQL_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.USER_NONEXISTENT;
import static java.util.Optional.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public final class IdUserLoginServiceTest extends IdServiceContract<IdUserLoginService>
{
  private IdFakeClock clock;
  private IdServerClock serverClock;
  private IdStrings strings;
  private IdSessionUserService sessions;
  private IdUserLoginService login;
  private IdDatabaseTransactionType transaction;
  private IdDatabaseUsersQueriesType users;
  private IdServerConfigurationService configurationService;
  private Path directory;
  private IdRateLimitUserLoginServiceType rateLimit;
  private IdEventServiceType events;
  private IdMetricsServiceType metrics;

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

  private IdUser createUser(
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
      new IdServerConfigurationParsers()
        .parseFile(file);

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
      IdStrings.create(Locale.ROOT);
    this.sessions =
      new IdSessionUserService(
        new IdMetricsService(IdServerTelemetryNoOp.noop()),
        Duration.ofDays(1L)
      );
    this.rateLimit =
      mock(IdRateLimitUserLoginServiceType.class);
    this.events =
      mock(IdEventServiceType.class);

    when(this.rateLimit.isAllowedByRateLimit(any()))
      .thenReturn(Boolean.TRUE);

    this.metrics =
      mock(IdMetricsServiceType.class);
    this.configurationService =
      new IdServerConfigurationService(this.metrics, configuration);
    this.login =
      new IdUserLoginService(
        this.serverClock,
        this.strings,
        this.sessions,
        this.configurationService,
        this.rateLimit,
        this.events
      );
    this.transaction =
      mock(IdDatabaseTransactionType.class);
    this.users =
      mock(IdDatabaseUsersQueriesType.class);

    when(this.transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(this.users);
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
    when(this.users.userGetForNameRequire(any()))
      .thenThrow(new IdDatabaseException("", USER_NONEXISTENT, Map.of(), empty()));

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        this.login.userLogin(
          this.transaction,
          UUID.randomUUID(),
          "127.0.0.1",
          "nonexistent",
          "password",
          Map.of()
        );
      });

    assertEquals(AUTHENTICATION_ERROR, ex.errorCode());

    verify(this.users, once()).userGetForNameRequire(any());
    verifyNoMoreInteractions(this.users);
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
    when(this.users.userGetForNameRequire(any()))
      .thenThrow(new IdDatabaseException("", SQL_ERROR, Map.of(), empty()));

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        this.login.userLogin(
          this.transaction,
          UUID.randomUUID(),
          "127.0.0.1",
          "nonexistent",
          "password",
          Map.of()
        );
      });

    assertEquals(SQL_ERROR, ex.errorCode());

    verify(this.users, once()).userGetForNameRequire(any());
    verifyNoMoreInteractions(this.users);
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
    final var admin =
      this.createUser("user");
    final var ban =
      new IdBan(admin.id(), "No reason.", empty());

    when(this.users.userGetForNameRequire(any()))
      .thenReturn(admin);
    when(this.users.userBanGet(any()))
      .thenReturn(Optional.of(ban));

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        this.login.userLogin(
          this.transaction,
          UUID.randomUUID(),
          "127.0.0.1",
          "user",
          "password",
          Map.of()
        );
      });

    assertEquals(BANNED, ex.errorCode());

    verify(this.users, once()).userGetForNameRequire(any());
    verify(this.users, once()).userBanGet(any());
    verifyNoMoreInteractions(this.users);
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
    final var admin =
      this.createUser("user");
    final var ban =
      new IdBan(
        admin.id(),
        "No reason.",
        Optional.of(this.serverClock.now().plusHours(1L)));

    when(this.users.userGetForNameRequire(any()))
      .thenReturn(admin);
    when(this.users.userBanGet(any()))
      .thenReturn(Optional.of(ban));

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        this.login.userLogin(
          this.transaction,
          UUID.randomUUID(),
          "127.0.0.1",
          "user",
          "password",
          Map.of()
        );
      });

    assertEquals(BANNED, ex.errorCode());

    verify(this.users, once()).userGetForNameRequire(any());
    verify(this.users, once()).userBanGet(any());
    verifyNoMoreInteractions(this.users);
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
    final var user =
      this.createUser("user");

    when(this.users.userGetForNameRequire(any()))
      .thenReturn(user);
    when(this.users.userBanGet(any()))
      .thenReturn(empty());

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        this.login.userLogin(
          this.transaction,
          UUID.randomUUID(),
          "127.0.0.1",
          "user",
          "not the password",
          Map.of()
        );
      });

    assertEquals(AUTHENTICATION_ERROR, ex.errorCode());

    verify(this.users, once()).userGetForNameRequire(any());
    verify(this.users, once()).userBanGet(any());
    verifyNoMoreInteractions(this.users);

    verify(this.events, once())
      .emit(new IdEventUserLoginAuthenticationFailed("127.0.0.1", user.id()));

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
    final var admin =
      this.createUser("user");

    when(this.users.userGetForNameRequire(any()))
      .thenReturn(admin);
    when(this.users.userBanGet(any()))
      .thenReturn(empty());
    when(this.rateLimit.isAllowedByRateLimit(any()))
      .thenReturn(Boolean.FALSE);

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        this.login.userLogin(
          this.transaction,
          UUID.randomUUID(),
          "127.0.0.1",
          "user",
          "x",
          Map.of()
        );
      });

    assertEquals(RATE_LIMIT_EXCEEDED, ex.errorCode());

    verifyNoMoreInteractions(this.users);
    verify(this.events, once())
      .emit(new IdEventUserLoginRateLimitExceeded("127.0.0.1", "user"));
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
    final var user =
      this.createUser("user");

    when(this.users.userGetForNameRequire(any()))
      .thenReturn(user);
    when(this.users.userBanGet(any()))
      .thenReturn(empty());

    final var loggedIn =
      this.login.userLogin(
        this.transaction,
        UUID.randomUUID(),
        "127.0.0.1",
        "user",
        "x",
        Map.of()
      );

    assertTrue(this.sessions.findSession(loggedIn.session().id()).isPresent());
    assertEquals(user.withRedactedPassword(), loggedIn.user());

    verify(this.users, once()).userGetForNameRequire(any());
    verify(this.users, once()).userBanGet(any());
    verify(this.users, once()).userLogin(any(), any(), anyInt());
    verifyNoMoreInteractions(this.users);

    verify(this.events, once())
      .emit(new IdEventUserLoggedIn(user.id()));
    verifyNoMoreInteractions(this.events);
  }

  @Override
  protected IdUserLoginService createInstanceA()
  {
    return new IdUserLoginService(
      this.serverClock,
      this.strings,
      this.sessions,
      this.configurationService,
      this.rateLimit,
      this.events
    );
  }

  @Override
  protected IdUserLoginService createInstanceB()
  {
    return new IdUserLoginService(
      this.serverClock,
      this.strings,
      this.sessions,
      this.configurationService,
      this.rateLimit,
      this.events
    );
  }
}

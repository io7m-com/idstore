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


package com.io7m.idstore.tests.server.controller.user_pwreset;

import com.io7m.idstore.database.api.IdDatabaseConnectionType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseTransactionType;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdNonEmptyList;
import com.io7m.idstore.model.IdPasswordAlgorithmRedacted;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdToken;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.model.IdUserPasswordReset;
import com.io7m.idstore.server.api.IdServerConfiguration;
import com.io7m.idstore.server.api.IdServerConfigurations;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.controller.user_pwreset.IdUserPasswordResetService;
import com.io7m.idstore.server.controller.user_pwreset.IdUserPasswordResetServiceType;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.clock.IdServerClock;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationFiles;
import com.io7m.idstore.server.service.mail.IdServerMailServiceType;
import com.io7m.idstore.server.service.ratelimit.IdRateLimitPasswordResetServiceType;
import com.io7m.idstore.server.service.telemetry.api.IdEventServiceType;
import com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryNoOp;
import com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType;
import com.io7m.idstore.server.service.templating.IdFMEmailPasswordResetData;
import com.io7m.idstore.server.service.templating.IdFMTemplateServiceType;
import com.io7m.idstore.server.service.templating.IdFMTemplateType;
import com.io7m.idstore.strings.IdStrings;
import com.io7m.idstore.tests.IdFakeClock;
import com.io7m.idstore.tests.IdTestDirectories;
import com.io7m.idstore.tests.server.api.IdServerConfigurationsTest;
import com.io7m.idstore.tests.server.service.IdServiceContract;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.internal.verification.Times;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.HTTP_PARAMETER_INVALID;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.HTTP_PARAMETER_NONEXISTENT;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.IO_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.MAIL_SYSTEM_FAILURE;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PASSWORD_RESET_MISMATCH;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PASSWORD_RESET_NONEXISTENT;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.RATE_LIMIT_EXCEEDED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SQL_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.USER_NONEXISTENT;
import static java.util.Optional.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class IdUserPasswordResetServiceTest
  extends IdServiceContract<IdUserPasswordResetServiceType>
{
  private static final IdUser FAKE_USER;

  static {
    try {
      FAKE_USER = new IdUser(
        UUID.randomUUID(),
        new IdName("person"),
        new IdRealName("A Real Name"),
        IdNonEmptyList.single(new IdEmail("someone@example.com")),
        OffsetDateTime.now(),
        OffsetDateTime.now(),
        IdPasswordAlgorithmRedacted.create().createHashed("a")
      );
    } catch (final IdPasswordException e) {
      throw new RuntimeException(e);
    }
  }

  private IdDatabaseType database;
  private IdFMTemplateServiceType templating;
  private IdFMTemplateType<IdFMEmailPasswordResetData> emailTemplate;
  private IdFakeClock clock;
  private IdRateLimitPasswordResetServiceType rateLimit;
  private IdServerBrandingServiceType branding;
  private IdServerClock serverClock;
  private IdServerConfiguration configuration;
  private IdServerMailServiceType mailService;
  private IdStrings strings;
  private IdServerTelemetryServiceType telemetry;
  private Path directory;
  private IdEventServiceType events;

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

    this.configuration =
      IdServerConfigurations.ofFile(
        Locale.getDefault(),
        Clock.systemUTC(),
        configFile
      );

    this.telemetry =
      IdServerTelemetryNoOp.noop();
    this.branding =
      Mockito.mock(IdServerBrandingServiceType.class);
    this.templating =
      Mockito.mock(IdFMTemplateServiceType.class);
    this.mailService =
      Mockito.mock(IdServerMailServiceType.class);
    this.clock =
      new IdFakeClock();
    this.serverClock =
      new IdServerClock(this.clock);
    this.database =
      Mockito.mock(IdDatabaseType.class);
    this.strings =
      IdStrings.create(Locale.ROOT);
    this.rateLimit =
      Mockito.mock(IdRateLimitPasswordResetServiceType.class);
    this.events =
      Mockito.mock(IdEventServiceType.class);

    this.emailTemplate =
      Mockito.mock(IdFMTemplateType.class);

    Mockito.when(this.templating.emailPasswordResetTemplate())
      .thenReturn(this.emailTemplate);
    Mockito.when(this.branding.title())
      .thenReturn("idstore");
  }

  @AfterEach
  public void tearDown()
    throws IOException
  {
    IdTestDirectories.deleteDirectory(this.directory);
  }

  @Override
  protected IdUserPasswordResetServiceType createInstanceA()
  {
    return IdUserPasswordResetService.create(
      this.telemetry,
      this.branding,
      this.templating,
      this.mailService,
      this.configuration,
      this.serverClock,
      this.database,
      this.strings,
      this.rateLimit,
      this.events
    );
  }

  @Override
  protected IdUserPasswordResetServiceType createInstanceB()
  {
    return IdUserPasswordResetService.create(
      this.telemetry,
      this.branding,
      this.templating,
      this.mailService,
      this.configuration,
      this.serverClock,
      this.database,
      this.strings,
      this.rateLimit,
      this.events
    );
  }

  /**
   * An email or username must be provided.
   *
   * @throws Exception On errors
   */

  @Test
  public void testResetBeginNoUsernameOrEmail()
    throws Exception
  {
    final var resets = this.createInstanceA();

    Mockito.when(this.rateLimit.isAllowedByRateLimit("127.0.0.1"))
      .thenReturn(Boolean.TRUE);

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        resets.resetBegin(
          "127.0.0.1",
          "NCSA Mosaic",
          UUID.randomUUID(),
          Optional.empty(),
          Optional.empty()
        );
      });

    assertEquals(HTTP_PARAMETER_NONEXISTENT, ex.errorCode());
  }

  /**
   * Rate limiting prevents frequent resets.
   *
   * @throws Exception On errors
   */

  @Test
  public void testResetBeginRateLimited()
    throws Exception
  {
    final var resets = this.createInstanceA();

    Mockito.when(this.rateLimit.isAllowedByRateLimit("127.0.0.1"))
      .thenReturn(Boolean.FALSE);

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        resets.resetBegin(
          "127.0.0.1",
          "NCSA Mosaic",
          UUID.randomUUID(),
          Optional.of("someone@example.com"),
          Optional.empty()
        );
      });

    assertEquals(RATE_LIMIT_EXCEEDED, ex.errorCode());
  }

  /**
   * A valid username must be provided.
   *
   * @throws Exception On errors
   */

  @Test
  public void testResetBeginUsernameInvalid()
    throws Exception
  {
    final var resets = this.createInstanceA();

    Mockito.when(this.rateLimit.isAllowedByRateLimit("127.0.0.1"))
      .thenReturn(Boolean.TRUE);

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        resets.resetBegin(
          "127.0.0.1",
          "NCSA Mosaic",
          UUID.randomUUID(),
          Optional.empty(),
          Optional.of("not valid in the slightest $")
        );
      });

    assertEquals(HTTP_PARAMETER_INVALID, ex.errorCode());
  }

  /**
   * An existing username must be provided.
   *
   * @throws Exception On errors
   */

  @Test
  public void testResetBeginUsernameNonexistent()
    throws Exception
  {
    final var resets =
      this.createInstanceA();

    final var connection =
      Mockito.mock(IdDatabaseConnectionType.class);
    final var transaction =
      Mockito.mock(IdDatabaseTransactionType.class);
    final var users =
      Mockito.mock(IdDatabaseUsersQueriesType.class);

    Mockito.when(this.database.openConnection(IDSTORE))
      .thenReturn(connection);
    Mockito.when(connection.openTransaction())
      .thenReturn(transaction);
    Mockito.when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);
    Mockito.when(this.rateLimit.isAllowedByRateLimit("127.0.0.1"))
      .thenReturn(Boolean.TRUE);

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        resets.resetBegin(
          "127.0.0.1",
          "NCSA Mosaic",
          UUID.randomUUID(),
          Optional.empty(),
          Optional.of("nonexistent")
        );
      });

    assertEquals(USER_NONEXISTENT, ex.errorCode());
  }

  /**
   * An existing email must be provided.
   *
   * @throws Exception On errors
   */

  @Test
  public void testResetBeginEmailNonexistent()
    throws Exception
  {
    final var resets =
      this.createInstanceA();

    final var connection =
      Mockito.mock(IdDatabaseConnectionType.class);
    final var transaction =
      Mockito.mock(IdDatabaseTransactionType.class);
    final var users =
      Mockito.mock(IdDatabaseUsersQueriesType.class);

    Mockito.when(this.database.openConnection(IDSTORE))
      .thenReturn(connection);
    Mockito.when(connection.openTransaction())
      .thenReturn(transaction);
    Mockito.when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);
    Mockito.when(this.rateLimit.isAllowedByRateLimit("127.0.0.1"))
      .thenReturn(Boolean.TRUE);

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        resets.resetBegin(
          "127.0.0.1",
          "NCSA Mosaic",
          UUID.randomUUID(),
          Optional.of("nonexistent@example.com"),
          Optional.empty()
        );
      });

    assertEquals(USER_NONEXISTENT, ex.errorCode());
  }


  /**
   * An email is sent if a username exists and everything else succeeds.
   *
   * @throws Exception On errors
   */

  @Test
  public void testResetBeginUsernameOK()
    throws Exception
  {
    final var resets =
      this.createInstanceA();

    final var connection =
      Mockito.mock(IdDatabaseConnectionType.class);
    final var transaction =
      Mockito.mock(IdDatabaseTransactionType.class);
    final var users =
      Mockito.mock(IdDatabaseUsersQueriesType.class);

    Mockito.when(this.database.openConnection(IDSTORE))
      .thenReturn(connection);
    Mockito.when(connection.openTransaction())
      .thenReturn(transaction);
    Mockito.when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);
    Mockito.when(this.rateLimit.isAllowedByRateLimit("127.0.0.1"))
      .thenReturn(Boolean.TRUE);
    Mockito.when(users.userGetForName(new IdName("person")))
      .thenReturn(Optional.of(FAKE_USER));

    Mockito.when(this.mailService.sendMail(
      Mockito.any(),
      Mockito.any(),
      Mockito.any(),
      Mockito.any(),
      Mockito.any(),
      Mockito.any()
    )).thenReturn(CompletableFuture.completedFuture(null));

    final var requestId = UUID.randomUUID();
    resets.resetBegin(
      "127.0.0.1",
      "NCSA Mosaic",
      requestId,
      Optional.empty(),
      Optional.of("person")
    );

    Mockito.verify(this.mailService, new Times(1))
      .sendMail(
        Mockito.any(),
        Mockito.eq(requestId),
        Mockito.eq(FAKE_USER.emails().first()),
        Mockito.any(),
        Mockito.any(),
        Mockito.any()
      );
  }

  /**
   * An email is sent if an email exists and everything else succeeds.
   *
   * @throws Exception On errors
   */

  @Test
  public void testResetBeginEmailOK()
    throws Exception
  {
    final var resets =
      this.createInstanceA();

    final var connection =
      Mockito.mock(IdDatabaseConnectionType.class);
    final var transaction =
      Mockito.mock(IdDatabaseTransactionType.class);
    final var users =
      Mockito.mock(IdDatabaseUsersQueriesType.class);

    Mockito.when(this.database.openConnection(IDSTORE))
      .thenReturn(connection);
    Mockito.when(connection.openTransaction())
      .thenReturn(transaction);
    Mockito.when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);
    Mockito.when(this.rateLimit.isAllowedByRateLimit("127.0.0.1"))
      .thenReturn(Boolean.TRUE);
    Mockito.when(users.userGetForEmail(FAKE_USER.emails().first()))
      .thenReturn(Optional.of(FAKE_USER));

    Mockito.when(this.mailService.sendMail(
      Mockito.any(),
      Mockito.any(),
      Mockito.any(),
      Mockito.any(),
      Mockito.any(),
      Mockito.any()
    )).thenReturn(CompletableFuture.completedFuture(null));

    final var requestId = UUID.randomUUID();
    resets.resetBegin(
      "127.0.0.1",
      "NCSA Mosaic",
      requestId,
      Optional.of(FAKE_USER.emails().first().value()),
      Optional.empty()
    );

    Mockito.verify(this.mailService, new Times(1))
      .sendMail(
        Mockito.any(),
        Mockito.eq(requestId),
        Mockito.eq(FAKE_USER.emails().first()),
        Mockito.any(),
        Mockito.any(),
        Mockito.any()
      );
  }

  /**
   * If the mail system fails, the operation fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testResetBeginMailSystemFails()
    throws Exception
  {
    final var resets =
      this.createInstanceA();

    final var connection =
      Mockito.mock(IdDatabaseConnectionType.class);
    final var transaction =
      Mockito.mock(IdDatabaseTransactionType.class);
    final var users =
      Mockito.mock(IdDatabaseUsersQueriesType.class);

    Mockito.when(this.database.openConnection(IDSTORE))
      .thenReturn(connection);
    Mockito.when(connection.openTransaction())
      .thenReturn(transaction);
    Mockito.when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);
    Mockito.when(this.rateLimit.isAllowedByRateLimit("127.0.0.1"))
      .thenReturn(Boolean.TRUE);
    Mockito.when(users.userGetForName(new IdName("person")))
      .thenReturn(Optional.of(FAKE_USER));

    final var exception =
      new IOException("Something failed.");

    Mockito.when(this.mailService.sendMail(
      Mockito.any(),
      Mockito.any(),
      Mockito.any(),
      Mockito.any(),
      Mockito.any(),
      Mockito.any()
    )).thenReturn(CompletableFuture.failedFuture(exception));

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        resets.resetBegin(
          "127.0.0.1",
          "NCSA Mosaic",
          UUID.randomUUID(),
          Optional.empty(),
          Optional.of("person")
        );
      });

    assertEquals(MAIL_SYSTEM_FAILURE, ex.errorCode());
    assertEquals(exception, ex.getCause().getCause());
  }

  /**
   * If the template fails, the operation fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testResetBeginTemplateFails()
    throws Exception
  {
    final var resets =
      this.createInstanceA();

    final var connection =
      Mockito.mock(IdDatabaseConnectionType.class);
    final var transaction =
      Mockito.mock(IdDatabaseTransactionType.class);
    final var users =
      Mockito.mock(IdDatabaseUsersQueriesType.class);

    Mockito.when(this.database.openConnection(IDSTORE))
      .thenReturn(connection);
    Mockito.when(connection.openTransaction())
      .thenReturn(transaction);
    Mockito.when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);
    Mockito.when(this.rateLimit.isAllowedByRateLimit("127.0.0.1"))
      .thenReturn(Boolean.TRUE);
    Mockito.when(users.userGetForName(new IdName("person")))
      .thenReturn(Optional.of(FAKE_USER));

    Mockito.doThrow(new IOException("Template failed."))
      .when(this.emailTemplate)
      .process(Mockito.any(), Mockito.any());

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        resets.resetBegin(
          "127.0.0.1",
          "NCSA Mosaic",
          UUID.randomUUID(),
          Optional.empty(),
          Optional.of("person")
        );
      });

    assertEquals(IO_ERROR, ex.errorCode());
  }

  /**
   * If the database fails, the operation fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testResetBeginDatabaseFails()
    throws Exception
  {
    final var resets =
      this.createInstanceA();

    final var connection =
      Mockito.mock(IdDatabaseConnectionType.class);
    final var transaction =
      Mockito.mock(IdDatabaseTransactionType.class);
    final var users =
      Mockito.mock(IdDatabaseUsersQueriesType.class);

    Mockito.when(this.database.openConnection(IDSTORE))
      .thenReturn(connection);
    Mockito.when(connection.openTransaction())
      .thenReturn(transaction);
    Mockito.when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);
    Mockito.when(this.rateLimit.isAllowedByRateLimit("127.0.0.1"))
      .thenReturn(Boolean.TRUE);
    Mockito.when(users.userGetForName(new IdName("person")))
      .thenThrow(new IdDatabaseException("Ouch", SQL_ERROR, Map.of(), empty()));

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        resets.resetBegin(
          "127.0.0.1",
          "NCSA Mosaic",
          UUID.randomUUID(),
          Optional.empty(),
          Optional.of("person")
        );
      });

    assertEquals(SQL_ERROR, ex.errorCode());
  }

  /**
   * If the correct token is provided, and the token has not expired, the check
   * succeeds.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCheckOK()
    throws Exception
  {
    final var resets =
      this.createInstanceA();

    final var connection =
      Mockito.mock(IdDatabaseConnectionType.class);
    final var transaction =
      Mockito.mock(IdDatabaseTransactionType.class);
    final var users =
      Mockito.mock(IdDatabaseUsersQueriesType.class);

    Mockito.when(this.database.openConnection(IDSTORE))
      .thenReturn(connection);
    Mockito.when(connection.openTransaction())
      .thenReturn(transaction);
    Mockito.when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);

    final var token =
      IdToken.generate();

    final var reset =
      new IdUserPasswordReset(
        FAKE_USER.id(),
        token,
        OffsetDateTime.now().plusYears(1L)
      );

    Mockito.when(users.userPasswordResetGetForToken(token))
      .thenReturn(Optional.of(reset));

    final var requestId = UUID.randomUUID();
    resets.resetCheck(
      "127.0.0.1",
      "NCSA Mosaic",
      requestId,
      Optional.of(token.value())
    );
  }

  /**
   * If a nonexistent token is provided the check fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCheckNonexistent()
    throws Exception
  {
    final var resets =
      this.createInstanceA();

    final var connection =
      Mockito.mock(IdDatabaseConnectionType.class);
    final var transaction =
      Mockito.mock(IdDatabaseTransactionType.class);
    final var users =
      Mockito.mock(IdDatabaseUsersQueriesType.class);

    Mockito.when(this.database.openConnection(IDSTORE))
      .thenReturn(connection);
    Mockito.when(connection.openTransaction())
      .thenReturn(transaction);
    Mockito.when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);

    final var token =
      IdToken.generate();

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        resets.resetCheck(
          "127.0.0.1",
          "NCSA Mosaic",
          UUID.randomUUID(),
          Optional.of(token.value())
        );
      });

    assertEquals(PASSWORD_RESET_NONEXISTENT, ex.errorCode());
  }

  /**
   * If an expired token is provided the check fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCheckExpired()
    throws Exception
  {
    final var resets =
      this.createInstanceA();

    final var connection =
      Mockito.mock(IdDatabaseConnectionType.class);
    final var transaction =
      Mockito.mock(IdDatabaseTransactionType.class);
    final var users =
      Mockito.mock(IdDatabaseUsersQueriesType.class);

    Mockito.when(this.database.openConnection(IDSTORE))
      .thenReturn(connection);
    Mockito.when(connection.openTransaction())
      .thenReturn(transaction);
    Mockito.when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);

    final var token =
      IdToken.generate();

    final var reset =
      new IdUserPasswordReset(
        FAKE_USER.id(),
        token,
        this.serverClock.now()
          .minusYears(1L)
      );

    Mockito.when(users.userPasswordResetGetForToken(token))
      .thenReturn(Optional.of(reset));

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        resets.resetCheck(
          "127.0.0.1",
          "NCSA Mosaic",
          UUID.randomUUID(),
          Optional.of(token.value())
        );
      });

    assertEquals(PASSWORD_RESET_NONEXISTENT, ex.errorCode());
  }

  /**
   * If the database fails, the check fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCheckDatabaseFails()
    throws Exception
  {
    final var resets =
      this.createInstanceA();

    final var connection =
      Mockito.mock(IdDatabaseConnectionType.class);
    final var transaction =
      Mockito.mock(IdDatabaseTransactionType.class);
    final var users =
      Mockito.mock(IdDatabaseUsersQueriesType.class);

    Mockito.when(this.database.openConnection(IDSTORE))
      .thenReturn(connection);
    Mockito.when(connection.openTransaction())
      .thenReturn(transaction);
    Mockito.when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);
    Mockito.when(users.userPasswordResetGetForToken(Mockito.any()))
      .thenThrow(new IdDatabaseException("Ouch", SQL_ERROR, Map.of(), empty()));

    final var token =
      IdToken.generate();

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        resets.resetCheck(
          "127.0.0.1",
          "NCSA Mosaic",
          UUID.randomUUID(),
          Optional.of(token.value())
        );
      });

    assertEquals(SQL_ERROR, ex.errorCode());
  }

  /**
   * If a token isn't provided, the check fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCheckTokenMissing()
    throws Exception
  {
    final var resets =
      this.createInstanceA();

    final var connection =
      Mockito.mock(IdDatabaseConnectionType.class);
    final var transaction =
      Mockito.mock(IdDatabaseTransactionType.class);
    final var users =
      Mockito.mock(IdDatabaseUsersQueriesType.class);

    Mockito.when(this.database.openConnection(IDSTORE))
      .thenReturn(connection);
    Mockito.when(connection.openTransaction())
      .thenReturn(transaction);
    Mockito.when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);
    Mockito.when(users.userPasswordResetGetForToken(Mockito.any()))
      .thenThrow(new IdDatabaseException("Ouch", SQL_ERROR, Map.of(), empty()));

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        resets.resetCheck(
          "127.0.0.1",
          "NCSA Mosaic",
          UUID.randomUUID(),
          Optional.empty()
        );
      });

    assertEquals(HTTP_PARAMETER_NONEXISTENT, ex.errorCode());
  }

  /**
   * If a token isn't valid (well-formed), the check fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCheckTokenInvalid()
    throws Exception
  {
    final var resets =
      this.createInstanceA();

    final var connection =
      Mockito.mock(IdDatabaseConnectionType.class);
    final var transaction =
      Mockito.mock(IdDatabaseTransactionType.class);
    final var users =
      Mockito.mock(IdDatabaseUsersQueriesType.class);

    Mockito.when(this.database.openConnection(IDSTORE))
      .thenReturn(connection);
    Mockito.when(connection.openTransaction())
      .thenReturn(transaction);
    Mockito.when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);
    Mockito.when(users.userPasswordResetGetForToken(Mockito.any()))
      .thenThrow(new IdDatabaseException("Ouch", SQL_ERROR, Map.of(), empty()));

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        resets.resetCheck(
          "127.0.0.1",
          "NCSA Mosaic",
          UUID.randomUUID(),
          Optional.of("this isn't what a token looks like")
        );
      });

    assertEquals(HTTP_PARAMETER_INVALID, ex.errorCode());
  }

  /**
   * If the correct token is provided, and the token has not expired, the
   * confirmation succeeds.
   *
   * @throws Exception On errors
   */

  @Test
  public void testConfirmOK()
    throws Exception
  {
    final var resets =
      this.createInstanceA();

    final var connection =
      Mockito.mock(IdDatabaseConnectionType.class);
    final var transaction =
      Mockito.mock(IdDatabaseTransactionType.class);
    final var users =
      Mockito.mock(IdDatabaseUsersQueriesType.class);

    Mockito.when(this.database.openConnection(IDSTORE))
      .thenReturn(connection);
    Mockito.when(connection.openTransaction())
      .thenReturn(transaction);
    Mockito.when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);

    final var token =
      IdToken.generate();

    final var reset =
      new IdUserPasswordReset(
        FAKE_USER.id(),
        token,
        OffsetDateTime.now().plusYears(1L)
      );

    Mockito.when(users.userPasswordResetGetForToken(token))
      .thenReturn(Optional.of(reset));

    resets.resetConfirm(
      "127.0.0.1",
      "NCSA Mosaic",
      UUID.randomUUID(),
      Optional.of("abcd"),
      Optional.of("abcd"),
      Optional.of(token.value())
    );
  }

  /**
   * Missing tokens fail confirmations.
   *
   * @throws Exception On errors
   */

  @Test
  public void testConfirmMissingToken()
    throws Exception
  {
    final var resets =
      this.createInstanceA();

    final var connection =
      Mockito.mock(IdDatabaseConnectionType.class);
    final var transaction =
      Mockito.mock(IdDatabaseTransactionType.class);
    final var users =
      Mockito.mock(IdDatabaseUsersQueriesType.class);

    Mockito.when(this.database.openConnection(IDSTORE))
      .thenReturn(connection);
    Mockito.when(connection.openTransaction())
      .thenReturn(transaction);
    Mockito.when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        resets.resetConfirm(
          "127.0.0.1",
          "NCSA Mosaic",
          UUID.randomUUID(),
          Optional.of("abcd"),
          Optional.of("abcd"),
          Optional.empty()
        );
      });

    assertEquals(HTTP_PARAMETER_NONEXISTENT, ex.errorCode());
  }

  /**
   * Missing passwords fail confirmations.
   *
   * @throws Exception On errors
   */

  @Test
  public void testConfirmMissingPassword0()
    throws Exception
  {
    final var resets =
      this.createInstanceA();

    final var connection =
      Mockito.mock(IdDatabaseConnectionType.class);
    final var transaction =
      Mockito.mock(IdDatabaseTransactionType.class);
    final var users =
      Mockito.mock(IdDatabaseUsersQueriesType.class);

    Mockito.when(this.database.openConnection(IDSTORE))
      .thenReturn(connection);
    Mockito.when(connection.openTransaction())
      .thenReturn(transaction);
    Mockito.when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        resets.resetConfirm(
          "127.0.0.1",
          "NCSA Mosaic",
          UUID.randomUUID(),
          Optional.empty(),
          Optional.of("abcd"),
          Optional.of(IdToken.generate().value())
        );
      });

    assertEquals(HTTP_PARAMETER_NONEXISTENT, ex.errorCode());
  }

  /**
   * Missing passwords fail confirmations.
   *
   * @throws Exception On errors
   */

  @Test
  public void testConfirmMissingPassword1()
    throws Exception
  {
    final var resets =
      this.createInstanceA();

    final var connection =
      Mockito.mock(IdDatabaseConnectionType.class);
    final var transaction =
      Mockito.mock(IdDatabaseTransactionType.class);
    final var users =
      Mockito.mock(IdDatabaseUsersQueriesType.class);

    Mockito.when(this.database.openConnection(IDSTORE))
      .thenReturn(connection);
    Mockito.when(connection.openTransaction())
      .thenReturn(transaction);
    Mockito.when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        resets.resetConfirm(
          "127.0.0.1",
          "NCSA Mosaic",
          UUID.randomUUID(),
          Optional.of("abcd"),
          Optional.empty(),
          Optional.of(IdToken.generate().value())
        );
      });

    assertEquals(HTTP_PARAMETER_NONEXISTENT, ex.errorCode());
  }

  /**
   * Invalid tokens fail confirmations.
   *
   * @throws Exception On errors
   */

  @Test
  public void testConfirmInvalidToken()
    throws Exception
  {
    final var resets =
      this.createInstanceA();

    final var connection =
      Mockito.mock(IdDatabaseConnectionType.class);
    final var transaction =
      Mockito.mock(IdDatabaseTransactionType.class);
    final var users =
      Mockito.mock(IdDatabaseUsersQueriesType.class);

    Mockito.when(this.database.openConnection(IDSTORE))
      .thenReturn(connection);
    Mockito.when(connection.openTransaction())
      .thenReturn(transaction);
    Mockito.when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        resets.resetConfirm(
          "127.0.0.1",
          "NCSA Mosaic",
          UUID.randomUUID(),
          Optional.of("abcd"),
          Optional.of("abcd"),
          Optional.of("this isn't what a token looks like.")
        );
      });

    assertEquals(HTTP_PARAMETER_INVALID, ex.errorCode());
  }

  /**
   * Mismatches passwords fail confirmations.
   *
   * @throws Exception On errors
   */

  @Test
  public void testConfirmMismatchedPasswords()
    throws Exception
  {
    final var resets =
      this.createInstanceA();

    final var connection =
      Mockito.mock(IdDatabaseConnectionType.class);
    final var transaction =
      Mockito.mock(IdDatabaseTransactionType.class);
    final var users =
      Mockito.mock(IdDatabaseUsersQueriesType.class);

    Mockito.when(this.database.openConnection(IDSTORE))
      .thenReturn(connection);
    Mockito.when(connection.openTransaction())
      .thenReturn(transaction);
    Mockito.when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        resets.resetConfirm(
          "127.0.0.1",
          "NCSA Mosaic",
          UUID.randomUUID(),
          Optional.of("abcd"),
          Optional.of("abcde"),
          Optional.of(IdToken.generate().value())
        );
      });

    assertEquals(PASSWORD_RESET_MISMATCH, ex.errorCode());
  }

  /**
   * If no token exists in the database, confirmation fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testConfirmTokenMissing()
    throws Exception
  {
    final var resets =
      this.createInstanceA();

    final var connection =
      Mockito.mock(IdDatabaseConnectionType.class);
    final var transaction =
      Mockito.mock(IdDatabaseTransactionType.class);
    final var users =
      Mockito.mock(IdDatabaseUsersQueriesType.class);

    Mockito.when(this.database.openConnection(IDSTORE))
      .thenReturn(connection);
    Mockito.when(connection.openTransaction())
      .thenReturn(transaction);
    Mockito.when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);

    final var token =
      IdToken.generate();

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        resets.resetConfirm(
          "127.0.0.1",
          "NCSA Mosaic",
          UUID.randomUUID(),
          Optional.of("abcd"),
          Optional.of("abcd"),
          Optional.of(token.value())
        );
      });

    assertEquals(PASSWORD_RESET_NONEXISTENT, ex.errorCode());
  }

  /**
   * If an expired token exists in the database, confirmation fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testConfirmTokenExpired()
    throws Exception
  {
    final var resets =
      this.createInstanceA();

    final var connection =
      Mockito.mock(IdDatabaseConnectionType.class);
    final var transaction =
      Mockito.mock(IdDatabaseTransactionType.class);
    final var users =
      Mockito.mock(IdDatabaseUsersQueriesType.class);

    Mockito.when(this.database.openConnection(IDSTORE))
      .thenReturn(connection);
    Mockito.when(connection.openTransaction())
      .thenReturn(transaction);
    Mockito.when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);

    final var token =
      IdToken.generate();

    final var reset =
      new IdUserPasswordReset(
        FAKE_USER.id(),
        token,
        this.serverClock.now()
          .minusYears(1L)
      );

    Mockito.when(users.userPasswordResetGetForToken(token))
      .thenReturn(Optional.of(reset));

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        resets.resetConfirm(
          "127.0.0.1",
          "NCSA Mosaic",
          UUID.randomUUID(),
          Optional.of("abcd"),
          Optional.of("abcd"),
          Optional.of(token.value())
        );
      });

    assertEquals(PASSWORD_RESET_NONEXISTENT, ex.errorCode());
  }

  /**
   * If the database fails, confirmation fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testConfirmDatabaseFails()
    throws Exception
  {
    final var resets =
      this.createInstanceA();

    final var connection =
      Mockito.mock(IdDatabaseConnectionType.class);
    final var transaction =
      Mockito.mock(IdDatabaseTransactionType.class);
    final var users =
      Mockito.mock(IdDatabaseUsersQueriesType.class);

    Mockito.when(this.database.openConnection(IDSTORE))
      .thenReturn(connection);
    Mockito.when(connection.openTransaction())
      .thenReturn(transaction);
    Mockito.when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);

    final var token =
      IdToken.generate();

    Mockito.when(users.userPasswordResetGetForToken(token))
      .thenThrow(new IdDatabaseException("Ouch", SQL_ERROR, Map.of(), empty()));

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        resets.resetConfirm(
          "127.0.0.1",
          "NCSA Mosaic",
          UUID.randomUUID(),
          Optional.of("abcd"),
          Optional.of("abcd"),
          Optional.of(token.value())
        );
      });

    assertEquals(SQL_ERROR, ex.errorCode());
  }
}

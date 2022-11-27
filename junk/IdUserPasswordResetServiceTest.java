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

package com.io7m.idstore.tests;

import com.io7m.idstore.database.api.IdDatabaseConfiguration;
import com.io7m.idstore.database.api.IdDatabaseCreate;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.database.api.IdDatabaseUpgrade;
import com.io7m.idstore.server.api.IdServerBrandingConfiguration;
import com.io7m.idstore.server.api.IdServerConfiguration;
import com.io7m.idstore.server.api.IdServerHTTPServiceConfiguration;
import com.io7m.idstore.server.api.IdServerHistoryConfiguration;
import com.io7m.idstore.server.api.IdServerMailConfiguration;
import com.io7m.idstore.server.api.IdServerMailTransportSMTP;
import com.io7m.idstore.server.api.IdServerRateLimitConfiguration;
import com.io7m.idstore.server.api.IdServerSessionConfiguration;
import com.io7m.idstore.server.controller.IdServerStrings;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.controller.user_pwreset.IdUserPasswordResetService;
import com.io7m.idstore.server.service.branding.IdServerBrandingService;
import com.io7m.idstore.server.service.clock.IdServerClock;
import com.io7m.idstore.server.service.mail.IdServerMailService;
import com.io7m.idstore.server.service.ratelimit.IdRateLimitPasswordResetService;
import com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryNoOp;
import com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType;
import com.io7m.idstore.server.service.templating.IdFMTemplateService;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.server.SMTPServer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.HTTP_PARAMETER_INVALID;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.HTTP_PARAMETER_NONEXISTENT;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PASSWORD_RESET_MISMATCH;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PASSWORD_RESET_NONEXISTENT;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.RATE_LIMIT_EXCEEDED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.USER_NONEXISTENT;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class IdUserPasswordResetServiceTest
  extends IdWithDatabaseContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdUserPasswordResetServiceTest.class);

  private IdFakeClock clock;
  private Path directory;
  private IdServerConfiguration serverConfiguration;
  private IdServerTelemetryServiceType telemetry;
  private IdRateLimitPasswordResetService rateLimit;
  private SMTPServer smtp;
  private ConcurrentLinkedQueue<MimeMessage> emailsReceived;

  private static IdUserPasswordResetService create(
    final Clock clock,
    final IdDatabaseType database,
    final IdServerTelemetryServiceType telemetry,
    final IdRateLimitPasswordResetService rateLimit,
    final IdServerConfiguration configuration)
    throws IOException
  {
    final var clockService =
      new IdServerClock(clock);

    final var strings =
      new IdServerStrings(Locale.getDefault());
    final var templates =
      IdFMTemplateService.create();

    final var mail =
      IdServerMailService.create(
        telemetry,
        configuration.mailConfiguration()
      );

    final var branding =
      IdServerBrandingService.create(
        templates,
        configuration.branding()
      );

    return IdUserPasswordResetService.create(
      telemetry,
      branding,
      templates,
      mail,
      configuration,
      clockService,
      database,
      strings,
      rateLimit
    );
  }

  @BeforeEach
  public void setup()
    throws IOException
  {
    this.directory =
      IdTestDirectories.createTempDirectory();
    this.clock =
      new IdFakeClock();
    this.telemetry =
      IdServerTelemetryNoOp.noop();
    this.rateLimit =
      IdRateLimitPasswordResetService.create(this.telemetry, 10L, MINUTES);

    this.serverConfiguration =
      new IdServerConfiguration(
        Locale.getDefault(),
        this.clock,
        DATABASES,
        new IdDatabaseConfiguration(
          "unused",
          "unused",
          "unused",
          10000,
          "unused",
          IdDatabaseCreate.DO_NOT_CREATE_DATABASE,
          IdDatabaseUpgrade.DO_NOT_UPGRADE_DATABASE,
          new IdFakeClock()
        ),
        new IdServerMailConfiguration(
          new IdServerMailTransportSMTP("localhost", 25000),
          Optional.empty(),
          "no-reply@example.com",
          Duration.of(10L, ChronoUnit.MINUTES)
        ),
        new IdServerHTTPServiceConfiguration(
          "127.0.0.1",
          50000,
          URI.create("http://localhost")
        ),
        new IdServerHTTPServiceConfiguration(
          "127.0.0.1",
          50001,
          URI.create("http://localhost")
        ),
        new IdServerHTTPServiceConfiguration(
          "127.0.0.1",
          50000,
          URI.create("http://localhost")
        ),
        new IdServerSessionConfiguration(
          Duration.of(30L, ChronoUnit.MINUTES),
          Duration.of(30L, ChronoUnit.MINUTES)
        ),
        new IdServerBrandingConfiguration(
          "idstore",
          Optional.empty(),
          Optional.empty(),
          Optional.empty()
        ),
        new IdServerHistoryConfiguration(1000, 1000),
        new IdServerRateLimitConfiguration(
          Duration.of(10L, ChronoUnit.MINUTES),
          Duration.of(10L, ChronoUnit.MINUTES)
        ),
        Optional.empty()
      );

    this.emailsReceived = new ConcurrentLinkedQueue<>();
    this.smtp =
      SMTPServer.port(25000)
        .messageHandler((messageContext, source, destination, data) -> {
          LOG.debug(
            "received mail: {} {} {}",
            source,
            destination,
            Integer.valueOf(data.length)
          );

          try {
            final var message =
              new MimeMessage(
                Session.getDefaultInstance(new Properties()),
                new ByteArrayInputStream(data)
              );

            this.emailsReceived.add(message);
          } catch (final MessagingException e) {
            throw new IllegalStateException(e);
          }
        })
        .build();
    this.smtp.start();
  }

  @AfterEach
  public void tearDown()
    throws IOException
  {
    this.smtp.stop();
    IdTestDirectories.deleteDirectory(this.directory);
  }

  @Test
  public void testBeginNoParameters()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var service =
      create(
        this.clock,
        this.database(),
        this.telemetry,
        this.rateLimit,
        this.serverConfiguration);

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        service.resetBegin(
          "127.0.0.1",
          "agent",
          UUID.randomUUID(),
          Optional.empty(),
          Optional.empty()
        );
      });

    assertEquals(HTTP_PARAMETER_NONEXISTENT, ex.errorCode());
  }

  @Test
  public void testBeginInvalidUsername()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var service =
      create(
        this.clock,
        this.database(),
        this.telemetry,
        this.rateLimit,
        this.serverConfiguration);

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        service.resetBegin(
          "127.0.0.1",
          "agent",
          UUID.randomUUID(),
          Optional.empty(),
          Optional.of("not a valid name")
        );
      });

    assertEquals(HTTP_PARAMETER_INVALID, ex.errorCode());
  }

  @Test
  public void testBeginInvalidEmail()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var service =
      create(
        this.clock,
        this.database(),
        this.telemetry,
        this.rateLimit,
        this.serverConfiguration);

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        service.resetBegin(
          "127.0.0.1",
          "agent",
          UUID.randomUUID(),
          Optional.of("Not a valid mail"),
          Optional.empty()
        );
      });

    assertEquals(HTTP_PARAMETER_INVALID, ex.errorCode());
  }

  @Test
  public void testBeginRateLimited0()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var user =
      this.databaseCreateUserInitial(adminId, "someone", "12345678");

    final var service =
      create(
        this.clock,
        this.database(),
        this.telemetry,
        this.rateLimit,
        this.serverConfiguration);

    this.rateLimit.isAllowedByRateLimit("127.0.0.1");

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        service.resetBegin(
          "127.0.0.1",
          "agent",
          UUID.randomUUID(),
          Optional.empty(),
          Optional.of("someone")
        );
      });

    assertEquals(RATE_LIMIT_EXCEEDED, ex.errorCode());
  }

  @Test
  public void testBeginUserNonexistent()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var service =
      create(
        this.clock,
        this.database(),
        this.telemetry,
        this.rateLimit,
        this.serverConfiguration);

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        service.resetBegin(
          "127.0.0.1",
          "agent",
          UUID.randomUUID(),
          Optional.empty(),
          Optional.of("someone")
        );
      });

    assertEquals(USER_NONEXISTENT, ex.errorCode());
  }

  @Test
  public void testBeginUserEmailNonexistent()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var service =
      create(
        this.clock,
        this.database(),
        this.telemetry,
        this.rateLimit,
        this.serverConfiguration);

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        service.resetBegin(
          "127.0.0.1",
          "agent",
          UUID.randomUUID(),
          Optional.of("nonexistent@example.com"),
          Optional.empty()
        );
      });

    assertEquals(USER_NONEXISTENT, ex.errorCode());
  }

  @Test
  public void testBeginUserNameOK()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var user =
      this.databaseCreateUserInitial(adminId, "someone", "12345678");

    final var service =
      create(
        this.clock,
        this.database(),
        this.telemetry,
        this.rateLimit,
        this.serverConfiguration);

    service.resetBegin(
      "127.0.0.1",
      "agent",
      UUID.randomUUID(),
      Optional.empty(),
      Optional.of("someone")
    );

    final var email = this.emailsReceived.poll();
    assertNotNull(email.getHeader("X-IDStore-PasswordReset-Token")[0]);
    assertNotNull(email.getHeader("X-IDStore-PasswordReset-From-Request")[0]);
    assertNotNull(email.getHeader("X-IDStore-PasswordReset-Confirm")[0]);
  }

  @Test
  public void testBeginEmailOK()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var user =
      this.databaseCreateUserInitial(adminId, "someone", "12345678");

    final var service =
      create(
        this.clock,
        this.database(),
        this.telemetry,
        this.rateLimit,
        this.serverConfiguration);

    service.resetBegin(
      "127.0.0.1",
      "agent",
      UUID.randomUUID(),
      Optional.of(user + "@example.com"),
      Optional.empty()
    );

    final var email = this.emailsReceived.poll();
    assertNotNull(email.getHeader("X-IDStore-PasswordReset-Token")[0]);
    assertNotNull(email.getHeader("X-IDStore-PasswordReset-From-Request")[0]);
    assertNotNull(email.getHeader("X-IDStore-PasswordReset-Confirm")[0]);
  }

  @Test
  public void testBeginMailSystemFails()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var user =
      this.databaseCreateUserInitial(adminId, "someone", "12345678");

    final var service =
      create(
        this.clock,
        this.database(),
        this.telemetry,
        this.rateLimit,
        this.serverConfiguration);

    this.smtp.stop();

    assertThrows(IdCommandExecutionFailure.class, () -> {
      service.resetBegin(
        "127.0.0.1",
        "agent",
        UUID.randomUUID(),
        Optional.empty(),
        Optional.of("someone")
      );
    });
  }

  @Test
  public void testCheckOK()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var user =
      this.databaseCreateUserInitial(adminId, "someone", "12345678");

    final var service =
      create(
        this.clock,
        this.database(),
        this.telemetry,
        this.rateLimit,
        this.serverConfiguration);

    service.resetBegin(
      "127.0.0.1",
      "agent",
      UUID.randomUUID(),
      Optional.empty(),
      Optional.of("someone")
    );

    final var email =
      this.emailsReceived.poll();
    final var token =
      email.getHeader("X-IDStore-PasswordReset-Token")[0];

    final var result =
      service.resetCheck(
        "127.0.0.1",
        "agent",
        UUID.randomUUID(),
        Optional.of(token)
      );

    assertEquals(token, result.value());
  }

  @Test
  public void testCheckExpired()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var user =
      this.databaseCreateUserInitial(adminId, "someone", "12345678");

    final var service =
      create(
        this.clock,
        this.database(),
        this.telemetry,
        this.rateLimit,
        this.serverConfiguration);

    service.resetBegin(
      "127.0.0.1",
      "agent",
      UUID.randomUUID(),
      Optional.empty(),
      Optional.of("someone")
    );

    final var email =
      this.emailsReceived.poll();
    final var token =
      email.getHeader("X-IDStore-PasswordReset-Token")[0];

    this.clock.setTime(this.clock.instant().plus(100L, DAYS));

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        service.resetCheck(
          "127.0.0.1",
          "agent",
          UUID.randomUUID(),
          Optional.of(token)
        );
      });

    assertEquals(PASSWORD_RESET_NONEXISTENT, ex.errorCode());
  }

  @Test
  public void testCheckNonexistent()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var user =
      this.databaseCreateUserInitial(adminId, "someone", "12345678");

    final var service =
      create(
        this.clock,
        this.database(),
        this.telemetry,
        this.rateLimit,
        this.serverConfiguration);

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        service.resetCheck(
          "127.0.0.1",
          "agent",
          UUID.randomUUID(),
          Optional.of("D16CDE97B6E058AE9B0984B5204F5F75")
        );
      });

    assertEquals(PASSWORD_RESET_NONEXISTENT, ex.errorCode());
  }

  @Test
  public void testCheckMissingParameters()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var user =
      this.databaseCreateUserInitial(adminId, "someone", "12345678");

    final var service =
      create(
        this.clock,
        this.database(),
        this.telemetry,
        this.rateLimit,
        this.serverConfiguration);

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        service.resetCheck(
          "127.0.0.1",
          "agent",
          UUID.randomUUID(),
          Optional.empty()
        );
      });

    assertEquals(HTTP_PARAMETER_NONEXISTENT, ex.errorCode());
  }

  @Test
  public void testConfirmMissingParameters()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var user =
      this.databaseCreateUserInitial(adminId, "someone", "12345678");

    final var service =
      create(
        this.clock,
        this.database(),
        this.telemetry,
        this.rateLimit,
        this.serverConfiguration);

    {
      final var ex =
        assertThrows(IdCommandExecutionFailure.class, () -> {
          service.resetConfirm(
            "127.0.0.1",
            "agent",
            UUID.randomUUID(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
          );
        });
      assertEquals(HTTP_PARAMETER_NONEXISTENT, ex.errorCode());
    }

    {
      final var ex =
        assertThrows(IdCommandExecutionFailure.class, () -> {
          service.resetConfirm(
            "127.0.0.1",
            "agent",
            UUID.randomUUID(),
            Optional.of("12345678"),
            Optional.empty(),
            Optional.empty()
          );
        });
      assertEquals(HTTP_PARAMETER_NONEXISTENT, ex.errorCode());
    }

    {
      final var ex =
        assertThrows(IdCommandExecutionFailure.class, () -> {
          service.resetConfirm(
            "127.0.0.1",
            "agent",
            UUID.randomUUID(),
            Optional.empty(),
            Optional.of("12345678"),
            Optional.empty()
          );
        });
      assertEquals(HTTP_PARAMETER_NONEXISTENT, ex.errorCode());
    }

    {
      final var ex =
        assertThrows(IdCommandExecutionFailure.class, () -> {
          service.resetConfirm(
            "127.0.0.1",
            "agent",
            UUID.randomUUID(),
            Optional.empty(),
            Optional.empty(),
            Optional.of("D16CDE97B6E058AE9B0984B5204F5F75")
          );
        });
      assertEquals(HTTP_PARAMETER_NONEXISTENT, ex.errorCode());
    }

    {
      final var ex =
        assertThrows(IdCommandExecutionFailure.class, () -> {
          service.resetConfirm(
            "127.0.0.1",
            "agent",
            UUID.randomUUID(),
            Optional.of("1234"),
            Optional.of("1234"),
            Optional.empty()
          );
        });
      assertEquals(HTTP_PARAMETER_NONEXISTENT, ex.errorCode());
    }
  }

  @Test
  public void testConfirmInvalidParameters()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var user =
      this.databaseCreateUserInitial(adminId, "someone", "12345678");

    final var service =
      create(
        this.clock,
        this.database(),
        this.telemetry,
        this.rateLimit,
        this.serverConfiguration);

    {
      final var ex =
        assertThrows(IdCommandExecutionFailure.class, () -> {
          service.resetConfirm(
            "127.0.0.1",
            "agent",
            UUID.randomUUID(),
            Optional.of("12345678"),
            Optional.of("12345678"),
            Optional.of("not valid")
          );
        });
      assertEquals(HTTP_PARAMETER_INVALID, ex.errorCode());
    }
  }

  @Test
  public void testConfirmInvalidPasswordMismatch()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var user =
      this.databaseCreateUserInitial(adminId, "someone", "12345678");

    final var service =
      create(
        this.clock,
        this.database(),
        this.telemetry,
        this.rateLimit,
        this.serverConfiguration);

    {
      final var ex =
        assertThrows(IdCommandExecutionFailure.class, () -> {
          service.resetConfirm(
            "127.0.0.1",
            "agent",
            UUID.randomUUID(),
            Optional.of("12345678"),
            Optional.of("12345679"),
            Optional.of("D16CDE97B6E058AE9B0984B5204F5F75")
          );
        });
      assertEquals(PASSWORD_RESET_MISMATCH, ex.errorCode());
    }
  }

  @Test
  public void testConfirmOK()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var user =
      this.databaseCreateUserInitial(adminId, "someone", "12345678");

    final var service =
      create(
        this.clock,
        this.database(),
        this.telemetry,
        this.rateLimit,
        this.serverConfiguration);

    service.resetBegin(
      "127.0.0.1",
      "agent",
      UUID.randomUUID(),
      Optional.empty(),
      Optional.of("someone")
    );

    final var email =
      this.emailsReceived.poll();
    final var token =
      email.getHeader("X-IDStore-PasswordReset-Token")[0];

    final var result =
      service.resetCheck(
        "127.0.0.1",
        "agent",
        UUID.randomUUID(),
        Optional.of(token)
      );

    assertEquals(token, result.value());

    service.resetConfirm(
      "127.0.0.1",
      "agent",
      UUID.randomUUID(),
      Optional.of("abcd"),
      Optional.of("abcd"),
      Optional.of(token)
    );
  }

  @Test
  public void testConfirmExpired()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var user =
      this.databaseCreateUserInitial(adminId, "someone", "12345678");

    final var service =
      create(
        this.clock,
        this.database(),
        this.telemetry,
        this.rateLimit,
        this.serverConfiguration);

    service.resetBegin(
      "127.0.0.1",
      "agent",
      UUID.randomUUID(),
      Optional.empty(),
      Optional.of("someone")
    );

    final var email =
      this.emailsReceived.poll();
    final var token =
      email.getHeader("X-IDStore-PasswordReset-Token")[0];

    final var result =
      service.resetCheck(
        "127.0.0.1",
        "agent",
        UUID.randomUUID(),
        Optional.of(token)
      );

    assertEquals(token, result.value());

    this.clock.setTime(this.clock.instant().plus(10L, DAYS));

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        service.resetConfirm(
          "127.0.0.1",
          "agent",
          UUID.randomUUID(),
          Optional.of("abcd"),
          Optional.of("abcd"),
          Optional.of(token)
        );
      });

    assertEquals(PASSWORD_RESET_NONEXISTENT, ex.errorCode());
  }
}

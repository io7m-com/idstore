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


package com.io7m.idstore.tests.extensions;

import com.io7m.idstore.database.api.IdDatabaseRole;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.database.postgres.IdDatabases;
import com.io7m.idstore.error_codes.IdException;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.server.api.IdServerBrandingConfiguration;
import com.io7m.idstore.server.api.IdServerConfiguration;
import com.io7m.idstore.server.api.IdServerException;
import com.io7m.idstore.server.api.IdServerHTTPServiceConfiguration;
import com.io7m.idstore.server.api.IdServerHistoryConfiguration;
import com.io7m.idstore.server.api.IdServerMailConfiguration;
import com.io7m.idstore.server.api.IdServerMailTransportSMTP;
import com.io7m.idstore.server.api.IdServerMaintenanceConfiguration;
import com.io7m.idstore.server.api.IdServerPasswordExpirationConfiguration;
import com.io7m.idstore.server.api.IdServerRateLimitConfiguration;
import com.io7m.idstore.server.api.IdServerSessionConfiguration;
import com.io7m.idstore.server.api.IdServerType;
import com.io7m.idstore.server.vanilla.IdServers;
import com.io7m.idstore.tests.extensions.IdTestDatabases.IdDatabaseFixture;
import com.io7m.idstore.tls.IdTLSDisabled;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.jmulticlose.core.ClosingResourceFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.server.SMTPServer;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Functions to construct test servers.
 */

public final class IdTestServers
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdTestServers.class);

  private static final IdDatabases DATABASES =
    new IdDatabases();

  private static final IdServers SERVERS =
    new IdServers();

  private IdTestServers()
  {

  }

  /**
   * A server fixture.
   *
   * @param mailServer   The mail server
   * @param mailReceived The received mail
   * @param server       The server
   * @param resources    The closeable resources
   */

  public record IdTestServerFixture(
    SMTPServer mailServer,
    ConcurrentLinkedQueue<MimeMessage> mailReceived,
    IdServerType server,
    CloseableCollectionType<ClosingResourceFailedException> resources)
    implements AutoCloseable
  {
    @Override
    public void close()
      throws Exception
    {
      this.resources.close();
    }

    /**
     * Create an initial admin.
     *
     * @param userName The name
     * @param password The password
     *
     * @return The admin ID
     *
     * @throws IdServerException On errors
     */

    public UUID createAdminInitial(
      final String userName,
      final String password)
      throws IdServerException
    {
      final var uuid = UUID.randomUUID();
      this.server.createOrUpdateInitialAdmin(
        uuid,
        new IdName(userName),
        new IdEmail(userName + "@example.com"),
        new IdRealName(userName),
        password
      );
      return uuid;
    }

    /**
     * Create a user.
     *
     * @param admin    The admin performing the creation
     * @param userName The name
     *
     * @return The user ID
     *
     * @throws IdServerException On errors
     */

    public UUID createUser(
      final UUID admin,
      final String userName)
      throws IdException
    {
      final var uuid = UUID.randomUUID();

      try (var connection =
             this.server.database().openConnection(IdDatabaseRole.IDSTORE)) {
        try (var transaction = connection.openTransaction()) {
          final var users =
            transaction.queries(IdDatabaseUsersQueriesType.class);

          transaction.adminIdSet(admin);
          users.userCreate(
            uuid,
            new IdName(userName),
            new IdRealName(userName),
            new IdEmail("%s@example.com".formatted(userName)),
            IdTestDatabases.timeNow(),
            IdPasswordAlgorithmPBKDF2HmacSHA256.create()
              .createHashed("12345678")
          );
          transaction.commit();
        }
      }

      return uuid;
    }

    /**
     * Get the user with the given ID.
     *
     * @param userId The user
     *
     * @return The user
     *
     * @throws IdException On errors
     */

    public IdUser getUser(
      final UUID userId)
      throws IdException
    {
      try (var connection =
             this.server.database().openConnection(IdDatabaseRole.IDSTORE)) {
        try (var transaction = connection.openTransaction()) {
          final var users =
            transaction.queries(IdDatabaseUsersQueriesType.class);

          return users.userGet(userId).orElseThrow();
        }
      }
    }
  }

  /**
   * Create a test server fixture.
   *
   * @param databaseFixture The database fixture
   * @param smtpServerPort  The SMTP server port
   * @param userAPIPort     The user API port
   * @param userViewPort    The user view port
   * @param adminAPIPort    The admin API port
   *
   * @return A test server fixture
   *
   * @throws Exception On errors
   */

  public static IdTestServerFixture create(
    final IdDatabaseFixture databaseFixture,
    final int smtpServerPort,
    final int userAPIPort,
    final int userViewPort,
    final int adminAPIPort)
    throws Exception
  {
    final var rateLimitConfiguration =
      new IdServerRateLimitConfiguration(
        Duration.ofMinutes(5L),
        Duration.ofMinutes(5L),
        Duration.ofSeconds(1L),
        Duration.ofSeconds(1L),
        Duration.ofSeconds(1L),
        Duration.ofSeconds(1L)
      );

    return createWithRateLimitConfiguration(
      databaseFixture,
      rateLimitConfiguration,
      smtpServerPort,
      userAPIPort,
      userViewPort,
      adminAPIPort
    );
  }

  /**
   * Create a test server fixture with a specific rate limit configuration.
   *
   * @param databaseFixture        The database fixture
   * @param rateLimitConfiguration The rate limit configuration
   * @param smtpServerPort         The SMTP server port
   * @param userAPIPort            The user API port
   * @param userViewPort           The user view port
   * @param adminAPIPort           The admin API port
   *
   * @return A test server fixture
   *
   * @throws Exception On errors
   */

  public static IdTestServerFixture createWithRateLimitConfiguration(
    final IdDatabaseFixture databaseFixture,
    final IdServerRateLimitConfiguration rateLimitConfiguration,
    final int smtpServerPort,
    final int userAPIPort,
    final int userViewPort,
    final int adminAPIPort)
    throws Exception
  {
    final var resources =
      CloseableCollection.create();

    final var mailConfiguration =
      new IdServerMailConfiguration(
        new IdServerMailTransportSMTP("localhost", smtpServerPort),
        Optional.empty(),
        "test@example.com",
        Duration.ofHours(1L)
      );

    final var emailsReceived =
      new ConcurrentLinkedQueue<MimeMessage>();

    final var smtp =
      SMTPServer.port(smtpServerPort)
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

            emailsReceived.add(message);
          } catch (final MessagingException e) {
            throw new IllegalStateException(e);
          }
        })
        .build();

    resources.add(smtp::stop);
    smtp.start();

    final var userApiConfiguration =
      new IdServerHTTPServiceConfiguration(
        "localhost",
        userAPIPort,
        URI.create("http://localhost:%d/".formatted(userAPIPort)),
        IdTLSDisabled.TLS_DISABLED
      );

    final var userViewConfiguration =
      new IdServerHTTPServiceConfiguration(
        "localhost",
        userViewPort,
        URI.create("http://localhost:%d/".formatted(userViewPort)),
        IdTLSDisabled.TLS_DISABLED
      );

    final var adminApiConfiguration =
      new IdServerHTTPServiceConfiguration(
        "localhost",
        adminAPIPort,
        URI.create("http://localhost:%d/".formatted(adminAPIPort)),
        IdTLSDisabled.TLS_DISABLED
      );

    final var sessionConfiguration =
      new IdServerSessionConfiguration(
        Duration.ofHours(1L),
        Duration.ofHours(1L)
      );

    final var brandingConfiguration =
      new IdServerBrandingConfiguration(
        "idstore",
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
      );

    final var historyConfiguration =
      new IdServerHistoryConfiguration(
        10,
        100
      );

    final var serverConfiguration =
      new IdServerConfiguration(
        Locale.ROOT,
        Clock.systemUTC(),
        DATABASES,
        databaseFixture.databaseConfiguration(),
        mailConfiguration,
        userApiConfiguration,
        userViewConfiguration,
        adminApiConfiguration,
        sessionConfiguration,
        brandingConfiguration,
        historyConfiguration,
        rateLimitConfiguration,
        new IdServerMaintenanceConfiguration(
          Optional.empty()
        ),
        new IdServerPasswordExpirationConfiguration(
          Optional.empty(),
          Optional.empty()
        ),
        Optional.empty()
      );

    final var server =
      resources.add(SERVERS.createServer(serverConfiguration));

    server.start();
    return new IdTestServerFixture(smtp, emailsReceived, server, resources);
  }
}

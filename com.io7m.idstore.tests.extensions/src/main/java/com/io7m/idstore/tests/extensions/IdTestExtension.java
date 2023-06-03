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

import com.io7m.idstore.database.api.IdDatabaseConfiguration;
import com.io7m.idstore.database.api.IdDatabaseCreate;
import com.io7m.idstore.database.api.IdDatabaseUpgrade;
import com.io7m.idstore.database.postgres.IdDatabases;
import com.io7m.idstore.server.api.IdServerBrandingConfiguration;
import com.io7m.idstore.server.api.IdServerConfiguration;
import com.io7m.idstore.server.api.IdServerHTTPServiceConfiguration;
import com.io7m.idstore.server.api.IdServerHistoryConfiguration;
import com.io7m.idstore.server.api.IdServerMailConfiguration;
import com.io7m.idstore.server.api.IdServerMailTransportSMTP;
import com.io7m.idstore.server.api.IdServerPasswordExpirationConfiguration;
import com.io7m.idstore.server.api.IdServerRateLimitConfiguration;
import com.io7m.idstore.server.api.IdServerSessionConfiguration;
import com.io7m.idstore.server.api.IdServerType;
import com.io7m.idstore.server.vanilla.IdServers;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.jmulticlose.core.ClosingResourceFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.server.SMTPServer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

/**
 * An extension that provides a working idstore server and database.
 */

public final class IdTestExtension
  implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback,
  ExtensionContext.Store.CloseableResource,
  ParameterResolver
{
  /**
   * The PostgreSQL server version.
   */

  public static final String POSTGRES_VERSION =
    "15.2";

  private static final Logger LOG =
    LoggerFactory.getLogger(IdTestExtension.class);

  private static final IdDatabases DATABASES =
    new IdDatabases();

  private static final IdServers SERVERS =
    new IdServers();

  private static final PostgreSQLContainer<?> CONTAINER =
    new PostgreSQLContainer<>(
      DockerImageName.parse("postgres")
        .withTag(POSTGRES_VERSION))
      .withDatabaseName("idstore")
      .withUsername("postgres")
      .withPassword("12345678");

  private CloseableCollectionType<ClosingResourceFailedException> resources;
  private CloseableCollectionType<ClosingResourceFailedException> perTestResources;
  private boolean started;
  private IdDatabaseConfiguration databaseConfiguration;
  private IdServerMailConfiguration mailConfiguration;
  private SMTPServer smtp;
  private ConcurrentLinkedQueue<MimeMessage> emailsReceived;
  private IdServerHTTPServiceConfiguration userApiConfiguration;
  private IdServerHTTPServiceConfiguration userViewConfiguration;
  private IdServerHTTPServiceConfiguration adminApiConfiguration;
  private IdServerSessionConfiguration sessionConfiguration;
  private IdServerBrandingConfiguration brandingConfiguration;
  private IdServerHistoryConfiguration historyConfiguration;
  private IdServerRateLimitConfiguration rateLimitConfiguration;
  private IdServerConfiguration serverConfiguration;
  private IdServerType server;
  private IdTestMailQueueType emailsReceivedQueue;

  /**
   * An extension that provides a working idstore server and database.
   */

  public IdTestExtension()
  {
    this.resources =
      CloseableCollection.create();
    this.perTestResources =
      CloseableCollection.create();
  }

  @Override
  public void beforeAll(
    final ExtensionContext context)
    throws Exception
  {
    if (!this.started) {
      this.started = true;

      context.getRoot()
        .getStore(GLOBAL)
        .put(IdTestExtension.class.getCanonicalName(), this);

      CONTAINER.start();
      CONTAINER.addEnv("PGPASSWORD", "12345678");
    }
  }

  @Override
  public void close()
    throws Throwable
  {
    LOG.debug("tearing down database container");
    this.resources.close();
  }

  @Override
  public void beforeEach(
    final ExtensionContext context)
    throws Exception
  {
    LOG.debug("setting up database");

    this.resources = CloseableCollection.create();
    this.resources.add(CONTAINER::stop);

    final var r0 =
      CONTAINER.execInContainer(
        "dropdb",
        "-w",
        "-U",
        "postgres",
        "idstore"
      );
    LOG.debug("stderr: {}", r0.getStderr());

    final var r1 =
      CONTAINER.execInContainer(
        "createdb",
        "-w",
        "-U",
        "postgres",
        "idstore"
      );

    LOG.debug("stderr: {}", r0.getStderr());
    assertEquals(0, r1.getExitCode());

    this.databaseConfiguration =
      new IdDatabaseConfiguration(
        "postgres",
        "12345678",
        CONTAINER.getHost(),
        CONTAINER.getFirstMappedPort().intValue(),
        "idstore",
        IdDatabaseCreate.CREATE_DATABASE,
        IdDatabaseUpgrade.UPGRADE_DATABASE,
        Clock.systemUTC()
      );

    this.perTestResources =
      CloseableCollection.create();

    this.mailConfiguration =
      new IdServerMailConfiguration(
        new IdServerMailTransportSMTP("localhost", 32025),
        Optional.empty(),
        "test@example.com",
        Duration.ofHours(1L)
      );

    this.emailsReceived =
      new ConcurrentLinkedQueue<>();

    this.emailsReceivedQueue =
      (IdTestMailQueueType) () -> this.emailsReceived;

    this.smtp =
      SMTPServer.port(32025)
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
    this.perTestResources.add(this.smtp::stop);

    this.userApiConfiguration =
      new IdServerHTTPServiceConfiguration(
        "localhost",
        32000,
        URI.create("http://localhost:32000/")
      );

    this.userViewConfiguration =
      new IdServerHTTPServiceConfiguration(
        "localhost",
        32001,
        URI.create("http://localhost:32001/")
      );

    this.adminApiConfiguration =
      new IdServerHTTPServiceConfiguration(
        "localhost",
        32002,
        URI.create("http://localhost:32002/")
      );

    this.sessionConfiguration =
      new IdServerSessionConfiguration(
        Duration.ofHours(1L),
        Duration.ofHours(1L)
      );

    this.brandingConfiguration =
      new IdServerBrandingConfiguration(
        "idstore",
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
      );

    this.historyConfiguration =
      new IdServerHistoryConfiguration(
        10,
        100
      );

    this.rateLimitConfiguration =
      new IdServerRateLimitConfiguration(
        Duration.ofMinutes(5L),
        Duration.ofMinutes(5L),
        Duration.ofSeconds(1L),
        Duration.ofSeconds(1L),
        Duration.ofSeconds(1L),
        Duration.ofSeconds(1L)
      );

    this.serverConfiguration =
      new IdServerConfiguration(
        Locale.ROOT,
        Clock.systemUTC(),
        DATABASES,
        this.databaseConfiguration,
        this.mailConfiguration,
        this.userApiConfiguration,
        this.userViewConfiguration,
        this.adminApiConfiguration,
        this.sessionConfiguration,
        this.brandingConfiguration,
        this.historyConfiguration,
        this.rateLimitConfiguration,
        new IdServerPasswordExpirationConfiguration(
          Optional.empty(),
          Optional.empty()
        ),
        Optional.empty()
      );

    this.server = SERVERS.createServer(this.serverConfiguration);
    this.perTestResources.add(this.server);
    this.server.start();
  }

  /**
   * @return The server configuration
   */

  public IdServerConfiguration serverConfiguration()
  {
    return this.serverConfiguration;
  }

  /**
   * @return The server
   */

  public IdServerType server()
  {
    return this.server;
  }

  @Override
  public void afterEach(
    final ExtensionContext context)
    throws Exception
  {
    LOG.debug("tearing down server");
    this.perTestResources.close();
  }

  @Override
  public boolean supportsParameter(
    final ParameterContext parameterContext,
    final ExtensionContext extensionContext)
    throws ParameterResolutionException
  {
    final var requiredType =
      parameterContext.getParameter().getType();

    return Objects.equals(requiredType, IdServerType.class)
           || Objects.equals(requiredType, IdTestMailQueueType.class);
  }

  @Override
  public Object resolveParameter(
    final ParameterContext parameterContext,
    final ExtensionContext extensionContext)
    throws ParameterResolutionException
  {
    final var requiredType =
      parameterContext.getParameter().getType();

    if (Objects.equals(requiredType, IdServerType.class)) {
      return this.server;
    }
    if (Objects.equals(requiredType, IdTestMailQueueType.class)) {
      return this.emailsReceivedQueue;
    }

    throw new IllegalStateException(
      "Unrecognized requested parameter type: %s".formatted(requiredType)
    );
  }
}

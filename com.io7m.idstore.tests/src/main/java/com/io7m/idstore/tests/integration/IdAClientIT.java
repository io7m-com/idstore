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

package com.io7m.idstore.tests.integration;

import com.io7m.ervilla.api.EContainerSupervisorType;
import com.io7m.ervilla.test_extension.ErvillaCloseAfterSuite;
import com.io7m.ervilla.test_extension.ErvillaConfiguration;
import com.io7m.ervilla.test_extension.ErvillaExtension;
import com.io7m.idstore.admin_client.IdAClients;
import com.io7m.idstore.admin_client.api.IdAClientConfiguration;
import com.io7m.idstore.admin_client.api.IdAClientConnectionParameters;
import com.io7m.idstore.admin_client.api.IdAClientException;
import com.io7m.idstore.admin_client.api.IdAClientType;
import com.io7m.idstore.error_codes.IdErrorCode;
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdAdminColumn;
import com.io7m.idstore.model.IdAdminColumnOrdering;
import com.io7m.idstore.model.IdAdminPermissionSet;
import com.io7m.idstore.model.IdAdminSearchByEmailParameters;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdNonEmptyList;
import com.io7m.idstore.model.IdPasswordAlgorithmRedacted;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdTimeRange;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchByEmailBegin;
import com.io7m.idstore.protocol.admin.IdACommandAdminSelf;
import com.io7m.idstore.protocol.admin.IdACommandType;
import com.io7m.idstore.protocol.admin.IdAMessageType;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSelf;
import com.io7m.idstore.protocol.admin.IdAResponseBlame;
import com.io7m.idstore.protocol.admin.IdAResponseError;
import com.io7m.idstore.protocol.admin.IdAResponseLogin;
import com.io7m.idstore.protocol.admin.IdAResponseUserBanDelete;
import com.io7m.idstore.protocol.admin.cb.IdACB1Messages;
import com.io7m.idstore.tests.containers.IdTestContainerInstances;
import com.io7m.idstore.tests.extensions.IdTestDatabases;
import com.io7m.idstore.tests.extensions.IdTestServers;
import com.io7m.quixote.core.QWebServerType;
import com.io7m.quixote.core.QWebServers;
import com.io7m.verdant.core.VProtocolException;
import com.io7m.verdant.core.VProtocolSupported;
import com.io7m.verdant.core.VProtocols;
import com.io7m.verdant.core.cb.VProtocolMessages;
import com.io7m.zelador.test_extension.CloseableResourcesType;
import com.io7m.zelador.test_extension.ZeladorExtension;
import net.jqwik.api.Arbitraries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_DUPLICATE_ID_NAME;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_NONEXISTENT;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.API_MISUSE_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.AUTHENTICATION_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.EMAIL_DUPLICATE;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.HTTP_SIZE_LIMIT;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.USER_DUPLICATE_ID_NAME;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.USER_NONEXISTENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
@Tag("admin-client")
@ExtendWith({ErvillaExtension.class, ZeladorExtension.class})
@ErvillaConfiguration(disabledIfUnsupported = true, projectName = "com.io7m.idstore")
public final class IdAClientIT
{
  private static final Set<IdErrorCode> ALLOWED_SMOKE_CODES =
    Set.of(
      ADMIN_DUPLICATE_ID_NAME,
      ADMIN_NONEXISTENT,
      API_MISUSE_ERROR,
      AUTHENTICATION_ERROR,
      EMAIL_DUPLICATE,
      HTTP_SIZE_LIMIT,
      USER_DUPLICATE_ID_NAME,
      USER_NONEXISTENT
    );

  private static final IdACB1Messages MESSAGES = new IdACB1Messages();
  private static final IdAdmin ADMIN;
  private static final IdAClients CLIENTS = new IdAClients();

  private static final VProtocolSupported V1 =
    new VProtocolSupported(
      IdACB1Messages.protocolId(),
      1L,
      0L,
      "/v1/"
    );

  private static final VProtocols V_PROTOCOLS =
    new VProtocols(List.of(V1));
  private static final VProtocolMessages V_PROTOCOL_MESSAGES =
    VProtocolMessages.create();
  private static final byte[] VERSION_HEADER;

  static {
    try {
      VERSION_HEADER = V_PROTOCOL_MESSAGES.serialize(V_PROTOCOLS, 1);
    } catch (final VProtocolException e) {
      throw new IllegalStateException(e);
    }

    try {
      ADMIN = new IdAdmin(
        UUID.randomUUID(),
        new IdName("someone"),
        new IdRealName("Someone"),
        IdNonEmptyList.single(new IdEmail("someone@example.com")),
        OffsetDateTime.now(),
        OffsetDateTime.now(),
        IdPasswordAlgorithmRedacted.create().createHashed("x"),
        IdAdminPermissionSet.empty()
      );
    } catch (final IdPasswordException e) {
      throw new IllegalStateException(e);
    }
  }

  private static IdTestDatabases.IdDatabaseFixture DATABASE_FIXTURE;
  private IdAClientType client;
  private QWebServerType webServer;
  private IdTestServers.IdTestServerFixture serverFixture;

  @BeforeAll
  public static void setupOnce(
    final @ErvillaCloseAfterSuite EContainerSupervisorType containers)
    throws Exception
  {
    DATABASE_FIXTURE = IdTestContainerInstances.database(containers);
  }

  @BeforeEach
  public void setupDatabase()
    throws Exception
  {
    DATABASE_FIXTURE.reset();
  }

  @BeforeEach
  public void setup(
    final CloseableResourcesType closeables)
    throws Exception
  {
    this.serverFixture =
      closeables.addPerTestResource(
        IdTestServers.create(
          DATABASE_FIXTURE,
          10025,
          50000,
          50001,
          51000
        ));

    this.client =
      CLIENTS.create(
        new IdAClientConfiguration(
          Clock.systemUTC(),
          Locale.ROOT
        )
      );

    this.webServer =
      closeables.addPerTestResource(QWebServers.createServer(60002));
  }

  /**
   * Command retries work when the server indicates a session has expired.
   *
   * @throws Exception On errors
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.HOURS)
  public void testCommandRetry()
    throws Exception
  {
    this.webServer.addResponse()
      .forPath("/")
      .withStatus(200)
      .withContentType("application/verdant+cedarbridge")
      .withFixedData(VERSION_HEADER);

    this.webServer.addResponse()
      .forPath("/v1/login")
      .withStatus(200)
      .withContentType(IdACB1Messages.contentType())
      .withFixedData(MESSAGES.serialize(
        new IdAResponseLogin(UUID.randomUUID(), ADMIN)));

    this.webServer.addResponse()
      .forPath("/v1/command")
      .withStatus(401)
      .withContentType(IdACB1Messages.contentType())
      .withFixedData(
        MESSAGES.serialize(
          new IdAResponseError(
            UUID.randomUUID(),
            "error",
            AUTHENTICATION_ERROR,
            Map.of(),
            Optional.empty(),
            IdAResponseBlame.BLAME_CLIENT
          ))
      );

    this.webServer.addResponse()
      .forPath("/v1/login")
      .withStatus(200)
      .withContentType(IdACB1Messages.contentType())
      .withFixedData(MESSAGES.serialize(
        new IdAResponseLogin(UUID.randomUUID(), ADMIN)));

    this.webServer.addResponse()
      .forPath("/v1/command")
      .withStatus(200)
      .withContentType(IdACB1Messages.contentType())
      .withFixedData(
        MESSAGES.serialize(
          new IdAResponseAdminSelf(UUID.randomUUID(), ADMIN))
      );

    this.client.connectOrThrow(
      new IdAClientConnectionParameters(
        "someone",
        "whatever",
        this.webServer.uri(),
        Map.of(),
        Duration.ofSeconds(30L),
        Duration.ofSeconds(30L)
      )
    );

    final var result =
      (IdAResponseAdminSelf) this.client.sendAndWaitOrThrow(
        new IdACommandAdminSelf(),
        Duration.ofSeconds(30L)
      );
    assertEquals(ADMIN.id(), result.admin().id());
  }

  /**
   * The client fails if the server returns a non-response.
   *
   * @throws Exception On errors
   */

  @Test
  public void testServerReturnsNonResponse()
    throws Exception
  {
    this.webServer.addResponse()
      .forPath("/")
      .withStatus(200)
      .withContentType("application/verdant+cedarbridge")
      .withFixedData(VERSION_HEADER);

    this.webServer.addResponse()
      .forPath("/v1/login")
      .withStatus(200)
      .withContentType(IdACB1Messages.contentType())
      .withFixedData(MESSAGES.serialize(
        new IdAResponseLogin(UUID.randomUUID(), ADMIN)));

    this.webServer.addResponse()
      .forPath("/v1/command")
      .withStatus(200)
      .withContentType(IdACB1Messages.contentType())
      .withFixedData(MESSAGES.serialize(new IdACommandAdminSelf()));

    this.client.connectOrThrow(
      new IdAClientConnectionParameters(
        "someone",
        "whatever",
        this.webServer.uri(),
        Map.of(),
        Duration.ofSeconds(30L),
        Duration.ofSeconds(30L)
      )
    );

    final var ex =
      assertThrows(
        IdAClientException.class,
        () -> {
          this.client.sendAndWaitOrThrow(
            new IdACommandAdminSelf(),
            Duration.ofSeconds(30L)
          );
        }
      );

    assertEquals(PROTOCOL_ERROR, ex.errorCode());
  }

  /**
   * The client fails if the server returns the wrong response.
   *
   * @throws Exception On errors
   */

  @Test
  public void testServerReturnsWrongResponse()
    throws Exception
  {
    this.webServer.addResponse()
      .forPath("/")
      .withStatus(200)
      .withContentType("application/verdant+cedarbridge")
      .withFixedData(VERSION_HEADER);

    this.webServer.addResponse()
      .forPath("/v1/login")
      .withStatus(200)
      .withContentType(IdACB1Messages.contentType())
      .withFixedData(MESSAGES.serialize(
        new IdAResponseLogin(UUID.randomUUID(), ADMIN)));

    this.webServer.addResponse()
      .forPath("/v1/command")
      .withStatus(200)
      .withContentType(IdACB1Messages.contentType())
      .withFixedData(MESSAGES.serialize(
        new IdAResponseUserBanDelete(UUID.randomUUID())));

    this.client.connectOrThrow(
      new IdAClientConnectionParameters(
        "someone",
        "whatever",
        this.webServer.uri(),
        Map.of(),
        Duration.ofSeconds(30L),
        Duration.ofSeconds(30L)
      )
    );

    final var ex =
      assertThrows(
        IdAClientException.class,
        () -> {
          this.client.sendAndWaitOrThrow(
            new IdACommandAdminSelf(),
            Duration.ofSeconds(30L)
          );
        }
      );

    assertEquals(PROTOCOL_ERROR, ex.errorCode());
  }

  /**
   * Logging in and seeing oneself works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginSelf()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");

    this.client.connectOrThrow(
      new IdAClientConnectionParameters(
        "admin",
        "12345678",
        this.serverFixture.server().adminAPI(),
        Map.of(),
        Duration.ofSeconds(30L),
        Duration.ofSeconds(30L)
      )
    );

    final var self =
      (IdAResponseAdminSelf)
        this.client.sendAndWaitOrThrow(
          new IdACommandAdminSelf(),
          Duration.ofSeconds(30L)
        );
    assertEquals(admin, self.admin().id());

    this.client.close();

    assertThrows(IllegalStateException.class, () -> {
      this.client.connectOrThrow(
        new IdAClientConnectionParameters(
          "admin",
          "12345678",
          this.serverFixture.server().adminAPI(),
          Map.of(),
          Duration.ofSeconds(30L),
          Duration.ofSeconds(30L)
        )
      );
    });
  }

  /**
   * Logging in with the wrong password fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginWrongPassword()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");

    final var ex =
      assertThrows(IdAClientException.class, () -> {
        this.client.connectOrThrow(
          new IdAClientConnectionParameters(
            "admin",
            "1234",
            this.serverFixture.server().adminAPI(),
            Map.of(),
            Duration.ofSeconds(30L),
            Duration.ofSeconds(30L)
          )
        );
      });

    assertEquals(AUTHENTICATION_ERROR, ex.errorCode());
  }

  /**
   * Executing a command without being connected results in an error.
   *
   * @return The tests
   */

  @TestFactory
  public Stream<DynamicTest> testDisconnected()
  {
    return Arbitraries.defaultFor(IdAMessageType.class)
      .sampleStream()
      .filter(m -> m instanceof IdACommandType<?>)
      .map(IdACommandType.class::cast)
      .limit(1000L)
      .map(c -> {
        return DynamicTest.dynamicTest(
          "testDisconnected_%s".formatted(c),
          () -> {
            assertThrows(IdAClientException.class, () -> {
              this.client.sendAndWaitOrThrow(c, Duration.ofSeconds(30L));
            });
          }
        );
      });
  }

  /**
   * A smoke test that simply executes random commands.
   */

  @Test
  public void testSmoke()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");

    final var messages =
      Arbitraries.defaultFor(IdAMessageType.class)
        .sampleStream()
        .filter(m -> m instanceof IdACommandType<?>)
        .map(IdACommandType.class::cast)
        .limit(2000L)
        .toList();

    this.client.connectOrThrow(
      new IdAClientConnectionParameters(
        "admin",
        "12345678",
        this.serverFixture.server().adminAPI(),
        Map.of(),
        Duration.ofSeconds(30L),
        Duration.ofSeconds(30L)
      )
    );

    for (final var c : messages) {
      try {
        this.client.sendAndWaitOrThrow(c, Duration.ofSeconds(30L));
      } catch (final IdAClientException ex) {
        if (ALLOWED_SMOKE_CODES.contains(ex.errorCode())) {
          continue;
        }
        throw ex;
      }
    }
  }

  /**
   * Emails with bad encodings do not cause problems.
   *
   * @throws Exception On errors
   */

  @Test
  public void testEmailEncodingBug()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");

    this.client.connectOrThrow(
      new IdAClientConnectionParameters(
        "admin",
        "12345678",
        this.serverFixture.server().adminAPI(),
        Map.of(),
        Duration.ofSeconds(30L),
        Duration.ofSeconds(30L)
      )
    );

    final var ex =
      assertThrows(IdAClientException.class, () -> {
        this.client.sendAndWaitOrThrow(
          new IdACommandAdminSearchByEmailBegin(
            new IdAdminSearchByEmailParameters(
              IdTimeRange.largest(),
              IdTimeRange.largest(),
              "\0",
              new IdAdminColumnOrdering(IdAdminColumn.BY_IDNAME, true),
              100
            )
          ),
          Duration.ofSeconds(30L)
        );
      });

    assertTrue(ex.getMessage().contains("invalid byte sequence for encoding"));
    assertEquals(PROTOCOL_ERROR, ex.errorCode());
  }
}

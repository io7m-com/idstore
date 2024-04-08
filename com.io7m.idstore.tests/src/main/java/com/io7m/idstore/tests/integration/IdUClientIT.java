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
import com.io7m.idstore.error_codes.IdErrorCode;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdNonEmptyList;
import com.io7m.idstore.model.IdPasswordAlgorithmRedacted;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.protocol.user.IdUCommandPasswordUpdate;
import com.io7m.idstore.protocol.user.IdUCommandType;
import com.io7m.idstore.protocol.user.IdUCommandUserSelf;
import com.io7m.idstore.protocol.user.IdUMessageType;
import com.io7m.idstore.protocol.user.IdUResponseBlame;
import com.io7m.idstore.protocol.user.IdUResponseEmailRemoveDeny;
import com.io7m.idstore.protocol.user.IdUResponseError;
import com.io7m.idstore.protocol.user.IdUResponseLogin;
import com.io7m.idstore.protocol.user.IdUResponseUserSelf;
import com.io7m.idstore.protocol.user.cb.IdUCB2Messages;
import com.io7m.idstore.tests.containers.IdTestContainerInstances;
import com.io7m.idstore.tests.extensions.IdTestDatabases;
import com.io7m.idstore.tests.extensions.IdTestServers;
import com.io7m.idstore.user_client.IdUClients;
import com.io7m.idstore.user_client.api.IdUClientConfiguration;
import com.io7m.idstore.user_client.api.IdUClientConnectionParameters;
import com.io7m.idstore.user_client.api.IdUClientException;
import com.io7m.idstore.user_client.api.IdUClientType;
import com.io7m.quixote.core.QWebServerType;
import com.io7m.quixote.core.QWebServers;
import com.io7m.verdant.core.VProtocolException;
import com.io7m.verdant.core.VProtocolSupported;
import com.io7m.verdant.core.VProtocols;
import com.io7m.verdant.core.cb.VProtocolMessages;
import com.io7m.zelador.test_extension.CloseableResourcesType;
import com.io7m.zelador.test_extension.ZeladorExtension;
import io.opentelemetry.api.OpenTelemetry;
import net.jqwik.api.Arbitraries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.API_MISUSE_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.AUTHENTICATION_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.EMAIL_NONEXISTENT;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.EMAIL_VERIFICATION_NONEXISTENT;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PASSWORD_RESET_MISMATCH;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.RATE_LIMIT_EXCEEDED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SECURITY_POLICY_DENIED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.USER_NONEXISTENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
@Tag("user-client")
@ExtendWith({ErvillaExtension.class, ZeladorExtension.class})
@ErvillaConfiguration(disabledIfUnsupported = true, projectName = "com.io7m.idstore")
public final class IdUClientIT
{
  private static final Set<IdErrorCode> ALLOWED_SMOKE_CODES =
    Set.of(
      API_MISUSE_ERROR,
      EMAIL_NONEXISTENT,
      EMAIL_VERIFICATION_NONEXISTENT,
      PASSWORD_RESET_MISMATCH,
      RATE_LIMIT_EXCEEDED,
      SECURITY_POLICY_DENIED,
      USER_NONEXISTENT
    );

  private static final IdUCB2Messages MESSAGES = new IdUCB2Messages();
  private static final IdUser USER;
  private static final IdUClients CLIENTS = new IdUClients();

  private static final VProtocolSupported V1 =
    new VProtocolSupported(
      IdUCB2Messages.protocolId(),
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
      USER = new IdUser(
        UUID.randomUUID(),
        new IdName("someone"),
        new IdRealName("Someone"),
        IdNonEmptyList.single(new IdEmail("someone@example.com")),
        OffsetDateTime.now(),
        OffsetDateTime.now(),
        IdPasswordAlgorithmRedacted.create().createHashed("x")
      );
    } catch (final IdPasswordException e) {
      throw new IllegalStateException(e);
    }
  }

  private static IdTestDatabases.IdDatabaseFixture DATABASE_FIXTURE;
  private IdUClientType client;
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
      closeables.addPerTestResource(
        CLIENTS.openSynchronousClient(
          new IdUClientConfiguration(
            OpenTelemetry.noop(),
            Locale.ROOT)
        ));

    this.webServer =
      closeables.addPerTestResource(QWebServers.createServer(60004));
  }


  /**
   * Command retries work when the server indicates a session has expired.
   *
   * @throws Exception On errors
   */

  @Test
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
      .withContentType(IdUCB2Messages.contentType())
      .withFixedData(
        MESSAGES.serialize(
          new IdUResponseLogin(UUID.randomUUID(), USER))
      );

    this.webServer.addResponse()
      .forPath("/v1/command")
      .withStatus(401)
      .withContentType(IdUCB2Messages.contentType())
      .withFixedData(
        MESSAGES.serialize(
          new IdUResponseError(
            UUID.randomUUID(),
            "error",
            AUTHENTICATION_ERROR,
            Map.of(),
            Optional.empty(),
            IdUResponseBlame.BLAME_CLIENT))
      );

    this.webServer.addResponse()
      .forPath("/v1/login")
      .withStatus(200)
      .withContentType(IdUCB2Messages.contentType())
      .withFixedData(
        MESSAGES.serialize(
          new IdUResponseLogin(UUID.randomUUID(), USER))
      );

    this.webServer.addResponse()
      .forPath("/v1/command")
      .withStatus(200)
      .withContentType(IdUCB2Messages.contentType())
      .withFixedData(
        MESSAGES.serialize(
          new IdUResponseUserSelf(UUID.randomUUID(), USER))
      );

    this.client.login(
      new IdUClientConnectionParameters(
        "someone",
        "whatever",
        this.webServer.uri(),
        Map.of()
      )
    );

    final var result =
      this.client.execute(new IdUCommandUserSelf())
        .map(IdUResponseUserSelf.class::cast)
        .orElseThrow(e -> new IllegalStateException());

    assertEquals(USER.id(), result.user().id());
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
      .withContentType(IdUCB2Messages.contentType())
      .withFixedData(
        MESSAGES.serialize(
          new IdUResponseLogin(UUID.randomUUID(), USER))
      );

    this.webServer.addResponse()
      .forPath("/v1/command")
      .withStatus(200)
      .withContentType(IdUCB2Messages.contentType())
      .withFixedData(MESSAGES.serialize(new IdUCommandUserSelf()));

    this.client.login(
      new IdUClientConnectionParameters(
        "someone",
        "whatever",
        this.webServer.uri(),
        Map.of()
      )
    );

    final var ex =
      assertThrows(IdUClientException.class, () -> {
        this.client.execute(new IdUCommandUserSelf())
          .orElseThrow(e -> new IdUClientException(
            e.message(),
            e.errorCode(),
            e.attributes(),
            e.remediatingAction(),
            Optional.of(e.requestId())
          ));
      });

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
      .withContentType(IdUCB2Messages.contentType())
      .withFixedData(
        MESSAGES.serialize(
          new IdUResponseLogin(UUID.randomUUID(), USER))
      );

    this.webServer.addResponse()
      .forPath("/v1/command")
      .withStatus(200)
      .withContentType(IdUCB2Messages.contentType())
      .withFixedData(MESSAGES.serialize(
        new IdUResponseEmailRemoveDeny(UUID.randomUUID()))
      );

    this.client.login(
      new IdUClientConnectionParameters(
        "someone",
        "whatever",
        this.webServer.uri(),
        Map.of()
      )
    );

    final var ex =
      assertThrows(IdUClientException.class, () -> {
        this.client.execute(new IdUCommandUserSelf())
          .orElseThrow(e -> new IdUClientException(
            e.message(),
            e.errorCode(),
            e.attributes(),
            e.remediatingAction(),
            Optional.of(e.requestId())
          ));
      });

    assertEquals(PROTOCOL_ERROR, ex.errorCode());
  }

  /**
   * Executing a command without being connected results in an error.
   *
   * @return The tests
   */

  @TestFactory
  public Stream<DynamicTest> testDisconnected()
  {
    return Arbitraries.defaultFor(IdUMessageType.class)
      .sampleStream()
      .filter(m -> m instanceof IdUCommandType<?>)
      .map(IdUCommandType.class::cast)
      .limit(1000L)
      .map(c -> {
        return DynamicTest.dynamicTest(
          "testDisconnected_%s".formatted(c),
          () -> {
            assertThrows(IdUClientException.class, () -> {
              this.client.executeOrElseThrow(c, IdUClientException::ofError);
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
    final var user =
      this.serverFixture.createUser(admin, "someone");

    final var messages =
      Arbitraries.defaultFor(IdUMessageType.class)
        .sampleStream()
        .filter(m -> m instanceof IdUCommandType<?>)
        .map(IdUCommandType.class::cast)
        .filter(m -> !(m instanceof IdUCommandPasswordUpdate))
        .limit(2000L)
        .toList();

    this.client.loginOrElseThrow(
      new IdUClientConnectionParameters(
        "someone",
        "12345678",
        this.serverFixture.server().userAPI(),
        Map.of()
      ),
      IdUClientException::ofError
    );

    for (final var c : messages) {
      try {
        this.client.executeOrElseThrow(c, IdUClientException::ofError);
      } catch (final IdUClientException ex) {
        if (ALLOWED_SMOKE_CODES.contains(ex.errorCode())) {
          continue;
        }
        throw ex;
      }
    }
  }

  /**
   * The version endpoint returns something sensible.
   *
   * @throws Exception On errors
   */

  @Test
  public void testServerVersionEndpoint()
    throws Exception
  {
    final var httpClient =
      HttpClient.newHttpClient();

    final var request =
      HttpRequest.newBuilder(
          this.serverFixture
            .server()
            .userAPI()
            .resolve("/version")
            .normalize()
        ).GET()
        .build();

    final var response =
      httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(
      "text/plain",
      response.headers()
        .firstValue("content-type")
        .orElseThrow()
    );
    assertTrue(response.body().startsWith("com.io7m.idstore "));
  }
}

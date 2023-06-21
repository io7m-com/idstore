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

import com.io7m.idstore.error_codes.IdErrorCode;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdNonEmptyList;
import com.io7m.idstore.model.IdPasswordAlgorithmRedacted;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.protocol.user.IdUCommandPasswordUpdate;
import com.io7m.idstore.protocol.user.IdUCommandType;
import com.io7m.idstore.protocol.user.IdUCommandUserSelf;
import com.io7m.idstore.protocol.user.IdUMessageType;
import com.io7m.idstore.protocol.user.IdUResponseEmailRemoveDeny;
import com.io7m.idstore.protocol.user.IdUResponseError;
import com.io7m.idstore.protocol.user.IdUResponseLogin;
import com.io7m.idstore.protocol.user.IdUResponseUserSelf;
import com.io7m.idstore.protocol.user.cb.IdUCB1Messages;
import com.io7m.idstore.tests.server.IdWithServerContract;
import com.io7m.idstore.user_client.IdUClients;
import com.io7m.idstore.user_client.api.IdUClientAsynchronousType;
import com.io7m.idstore.user_client.api.IdUClientConfiguration;
import com.io7m.idstore.user_client.api.IdUClientCredentials;
import com.io7m.idstore.user_client.api.IdUClientException;
import com.io7m.quixote.core.QWebServerType;
import com.io7m.quixote.core.QWebServers;
import com.io7m.verdant.core.VProtocolSupported;
import com.io7m.verdant.core.VProtocols;
import com.io7m.verdant.core.cb.VProtocolMessages;
import io.opentelemetry.api.OpenTelemetry;
import net.jqwik.api.Arbitraries;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.net.URI;
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
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_NONEXISTENT;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.API_MISUSE_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.AUTHENTICATION_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.EMAIL_NONEXISTENT;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.EMAIL_VERIFICATION_NONEXISTENT;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PASSWORD_RESET_MISMATCH;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.RATE_LIMIT_EXCEEDED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SECURITY_POLICY_DENIED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.USER_NONEXISTENT;
import static com.io7m.idstore.protocol.user.IdUResponseBlame.BLAME_CLIENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
@Tag("user-client")
public final class IdUClientAsynchronousIT extends IdWithServerContract
{
  private IdUCB1Messages messages;
  private IdUser user;
  private IdUClients clients;
  private IdUClientAsynchronousType client;
  private VProtocols versions;
  private VProtocolMessages versionMessages;
  private byte[] versionHeader;
  private QWebServerType webServer;

  @BeforeEach
  public void setup()
    throws Exception
  {
    this.user =
      new IdUser(
        UUID.randomUUID(),
        new IdName("someone"),
        new IdRealName("Someone"),
        IdNonEmptyList.single(new IdEmail("someone@example.com")),
        OffsetDateTime.now(),
        OffsetDateTime.now(),
        IdPasswordAlgorithmRedacted.create().createHashed("x")
      );

    this.clients =
      new IdUClients();
    this.client =
      this.clients.openAsynchronousClient(
        new IdUClientConfiguration(
          OpenTelemetry.noop(),
          Locale.ROOT)
      );

    this.messages =
      new IdUCB1Messages();

    final var v1 =
      new VProtocolSupported(
        IdUCB1Messages.protocolId(),
        1L,
        0L,
        "/v1/"
      );

    this.versions =
      new VProtocols(List.of(v1));
    this.versionMessages =
      VProtocolMessages.create();
    this.versionHeader =
      this.versionMessages.serialize(this.versions, 1);
    this.webServer =
      QWebServers.createServer(60001);
  }

  @AfterEach
  public void tearDown()
    throws Exception
  {
    this.webServer.close();
    this.client.close();
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
      .withFixedData(this.versionHeader);

    this.webServer.addResponse()
      .forPath("/v1/login")
      .withStatus(200)
      .withContentType(IdUCB1Messages.contentType())
      .withFixedData(
        this.messages.serialize(
          new IdUResponseLogin(UUID.randomUUID(), this.user))
      );

    this.webServer.addResponse()
      .forPath("/v1/command")
      .withStatus(401)
      .withContentType(IdUCB1Messages.contentType())
      .withFixedData(
        this.messages.serialize(
          new IdUResponseError(
            UUID.randomUUID(),
            "error",
            AUTHENTICATION_ERROR,
            Map.of(),
            Optional.empty(),
            BLAME_CLIENT
          ))
      );

    this.webServer.addResponse()
      .forPath("/v1/login")
      .withStatus(200)
      .withContentType(IdUCB1Messages.contentType())
      .withFixedData(
        this.messages.serialize(
          new IdUResponseLogin(UUID.randomUUID(), this.user))
      );

    this.webServer.addResponse()
      .forPath("/v1/command")
      .withStatus(200)
      .withContentType(IdUCB1Messages.contentType())
      .withFixedData(
        this.messages.serialize(
          new IdUResponseUserSelf(UUID.randomUUID(), this.user))
      );

    this.client.loginAsync(
      new IdUClientCredentials(
        "someone",
        "whatever",
        URI.create("http://localhost:60001/"),
        Map.of()
      )
    ).get();

    final var result =
      this.client.executeAsync(new IdUCommandUserSelf())
        .get()
        .map(IdUResponseUserSelf.class::cast)
        .orElseThrow(e -> new IllegalStateException());

    assertEquals(this.user.id(), result.user().id());
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
      .withFixedData(this.versionHeader);

    this.webServer.addResponse()
      .forPath("/v1/login")
      .withStatus(200)
      .withContentType(IdUCB1Messages.contentType())
      .withFixedData(
        this.messages.serialize(
          new IdUResponseLogin(UUID.randomUUID(), this.user))
      );

    this.webServer.addResponse()
      .forPath("/v1/command")
      .withStatus(200)
      .withContentType(IdUCB1Messages.contentType())
      .withFixedData(this.messages.serialize(new IdUCommandUserSelf()));

    this.client.loginAsync(
      new IdUClientCredentials(
        "someone",
        "whatever",
        URI.create("http://localhost:60001/"),
        Map.of()
      )
    ).get();

    final var ex =
      assertThrows(IdUClientException.class, () -> {
        try {
          this.client.executeAsyncOrElseThrow(
            new IdUCommandUserSelf(),
            e -> new IdUClientException(
              e.message(),
              e.errorCode(),
              e.attributes(),
              e.remediatingAction(),
              Optional.of(e.requestId())
            )).get();
        } catch (final ExecutionException e) {
          throw e.getCause();
        }
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
      .withFixedData(this.versionHeader);

    this.webServer.addResponse()
      .forPath("/v1/login")
      .withStatus(200)
      .withContentType(IdUCB1Messages.contentType())
      .withFixedData(
        this.messages.serialize(
          new IdUResponseLogin(UUID.randomUUID(), this.user))
      );

    this.webServer.addResponse()
      .forPath("/v1/command")
      .withStatus(200)
      .withContentType(IdUCB1Messages.contentType())
      .withFixedData(this.messages.serialize(
        new IdUResponseEmailRemoveDeny(UUID.randomUUID()))
      );

    this.client.loginAsync(
      new IdUClientCredentials(
        "someone",
        "whatever",
        URI.create("http://localhost:60001/"),
        Map.of()
      )
    ).get();

    final var ex =
      assertThrows(IdUClientException.class, () -> {
        try {
          this.client.executeAsyncOrElseThrow(
            new IdUCommandUserSelf(),
            e -> new IdUClientException(
              e.message(),
              e.errorCode(),
              e.attributes(),
              e.remediatingAction(),
              Optional.of(e.requestId())
            )).get();
        } catch (final ExecutionException e) {
          throw e.getCause();
        }
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
              try {
                this.client.executeAsyncOrElseThrow(c, IdUClientException::ofError)
                  .get();
              } catch (final ExecutionException e) {
                throw e.getCause();
              }
            });
          }
        );
      });
  }

  /**
   * A smoke test that simply executes random commands.
   *
   * @return The tests
   */

  @TestFactory
  public Stream<DynamicTest> testSmoke()
    throws Exception
  {
    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    final var user =
      this.serverCreateUser(admin, "someone");

    return Arbitraries.defaultFor(IdUMessageType.class)
      .sampleStream()
      .filter(m -> m instanceof IdUCommandType<?>)
      .map(IdUCommandType.class::cast)
      .filter(m -> !(m instanceof IdUCommandPasswordUpdate))
      .limit(2000L)
      .map(c -> {
        return DynamicTest.dynamicTest("testSmoke_" + c, () -> {
          this.client.loginAsyncOrElseThrow(
            new IdUClientCredentials(
              "someone",
              "12345678",
              this.serverUserAPIURL(),
              Map.of()
            ),
            IdUClientException::ofError
          ).get();

          try {
            try {
              this.client.executeAsyncOrElseThrow(c, IdUClientException::ofError).get();
            } catch (final ExecutionException e) {
              throw e.getCause();
            }
          } catch (final IdUClientException ex) {
            if (ALLOWED_SMOKE_CODES.contains(ex.errorCode())) {
              return;
            }
            throw ex;
          }
        });
      });
  }

  private static final Set<IdErrorCode> ALLOWED_SMOKE_CODES =
    Set.of(
      ADMIN_NONEXISTENT,
      API_MISUSE_ERROR,
      EMAIL_NONEXISTENT,
      EMAIL_VERIFICATION_NONEXISTENT,
      PASSWORD_RESET_MISMATCH,
      RATE_LIMIT_EXCEEDED,
      SECURITY_POLICY_DENIED,
      USER_NONEXISTENT
    );

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

    this.serverStartIfNecessary();

    final var request =
      HttpRequest.newBuilder(
          this.serverUserAPIURL()
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

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

import com.io7m.idstore.admin_client.IdAClients;
import com.io7m.idstore.admin_client.api.IdAClientConfiguration;
import com.io7m.idstore.admin_client.api.IdAClientCredentials;
import com.io7m.idstore.admin_client.api.IdAClientException;
import com.io7m.idstore.admin_client.api.IdAClientSynchronousType;
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
import com.io7m.idstore.tests.server.IdWithServerContract;
import com.io7m.quixote.core.QWebServerType;
import com.io7m.quixote.core.QWebServers;
import com.io7m.verdant.core.VProtocolSupported;
import com.io7m.verdant.core.VProtocols;
import com.io7m.verdant.core.cb.VProtocolMessages;
import net.jqwik.api.Arbitraries;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_DUPLICATE_ID_NAME;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_NONEXISTENT;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.AUTHENTICATION_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.EMAIL_DUPLICATE;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.USER_DUPLICATE_ID_NAME;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.USER_NONEXISTENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
@Tag("admin-client")
public final class IdAClientIT extends IdWithServerContract
{
  private IdACB1Messages messages;
  private IdAdmin admin;
  private IdAClients clients;
  private IdAClientSynchronousType client;
  private VProtocols versions;
  private VProtocolMessages versionMessages;
  private byte[] versionHeader;
  private QWebServerType webServer;

  @BeforeEach
  public void setup()
    throws Exception
  {
    this.admin =
      new IdAdmin(
        UUID.randomUUID(),
        new IdName("someone"),
        new IdRealName("Someone"),
        IdNonEmptyList.single(new IdEmail("someone@example.com")),
        OffsetDateTime.now(),
        OffsetDateTime.now(),
        IdPasswordAlgorithmRedacted.create().createHashed("x"),
        IdAdminPermissionSet.empty()
      );

    this.clients =
      new IdAClients();
    this.client =
      this.clients.openSynchronousClient(new IdAClientConfiguration(Locale.ROOT));

    this.messages =
      new IdACB1Messages();

    final var v1 =
      new VProtocolSupported(
        IdACB1Messages.protocolId(),
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
      QWebServers.createServer(60000);
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
      .withContentType(IdACB1Messages.contentType())
      .withFixedData(this.messages.serialize(
        new IdAResponseLogin(UUID.randomUUID(), this.admin)));

    this.webServer.addResponse()
      .forPath("/v1/command")
      .withStatus(401)
      .withContentType(IdACB1Messages.contentType())
      .withFixedData(
        this.messages.serialize(
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
      .withFixedData(this.messages.serialize(
        new IdAResponseLogin(UUID.randomUUID(), this.admin)));

    this.webServer.addResponse()
      .forPath("/v1/command")
      .withStatus(200)
      .withContentType(IdACB1Messages.contentType())
      .withFixedData(
        this.messages.serialize(
          new IdAResponseAdminSelf(UUID.randomUUID(), this.admin))
      );

    this.client.loginOrElseThrow(
      new IdAClientCredentials(
        "someone",
        "whatever",
        URI.create("http://localhost:60000/"),
        Map.of()
      ),
      IdAClientException::ofError
    );

    final var result = (IdAResponseAdminSelf) this.client.executeOrElseThrow(
      new IdACommandAdminSelf(),
      IdAClientException::ofError);
    assertEquals(this.admin.id(), result.admin().id());
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
      .withContentType(IdACB1Messages.contentType())
      .withFixedData(this.messages.serialize(
        new IdAResponseLogin(UUID.randomUUID(), this.admin)));

    this.webServer.addResponse()
      .forPath("/v1/command")
      .withStatus(200)
      .withContentType(IdACB1Messages.contentType())
      .withFixedData(this.messages.serialize(new IdACommandAdminSelf()));

    this.client.loginOrElseThrow(
      new IdAClientCredentials(
        "someone",
        "whatever",
        URI.create("http://localhost:60000/"),
        Map.of()
      ),
      IdAClientException::ofError
    );

    final var ex =
      assertThrows(
        IdAClientException.class,
        () -> {
          this.client.executeOrElseThrow(
            new IdACommandAdminSelf(),
            IdAClientException::ofError
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
      .withFixedData(this.versionHeader);

    this.webServer.addResponse()
      .forPath("/v1/login")
      .withStatus(200)
      .withContentType(IdACB1Messages.contentType())
      .withFixedData(this.messages.serialize(
        new IdAResponseLogin(UUID.randomUUID(), this.admin)));

    this.webServer.addResponse()
      .forPath("/v1/command")
      .withStatus(200)
      .withContentType(IdACB1Messages.contentType())
      .withFixedData(this.messages.serialize(
        new IdAResponseUserBanDelete(UUID.randomUUID())));

    this.client.loginOrElseThrow(
      new IdAClientCredentials(
        "someone",
        "whatever",
        URI.create("http://localhost:60000/"),
        Map.of()
      ),
      IdAClientException::ofError
    );

    final var ex =
      assertThrows(
        IdAClientException.class,
        () -> this.client.executeOrElseThrow(
          new IdACommandAdminSelf(),
          IdAClientException::ofError)
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
      this.serverCreateAdminInitial("admin", "12345678");

    this.client.loginOrElseThrow(
      new IdAClientCredentials(
        "admin",
        "12345678",
        this.serverAdminAPIURL(),
        Map.of()
      ),
      IdAClientException::ofError
    );

    final var self =
      (IdAResponseAdminSelf)
        this.client.executeOrElseThrow(
          new IdACommandAdminSelf(),
          IdAClientException::ofError
        );
    assertEquals(admin, self.admin().id());

    this.client.close();

    assertThrows(IllegalStateException.class, () -> {
      this.client.loginOrElseThrow(
        new IdAClientCredentials(
          "admin",
          "12345678",
          this.serverAdminAPIURL(),
          Map.of()
        ),
        IdAClientException::ofError
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
      this.serverCreateAdminInitial("admin", "12345678");

    final var ex =
      assertThrows(IdAClientException.class, () -> {
        this.client.loginOrElseThrow(
          new IdAClientCredentials(
            "admin",
            "1234",
            this.serverAdminAPIURL(),
            Map.of()
          ),
          IdAClientException::ofError
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
              this.client.executeOrElseThrow(c, IdAClientException::ofError);
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

    return Arbitraries.defaultFor(IdAMessageType.class)
      .sampleStream()
      .filter(m -> m instanceof IdACommandType<?>)
      .map(IdACommandType.class::cast)
      .limit(2000L)
      .map(c -> {
        return DynamicTest.dynamicTest("testSmoke_" + c, () -> {
          this.client.loginOrElseThrow(
            new IdAClientCredentials(
              "admin",
              "12345678",
              this.serverAdminAPIURL(),
              Map.of()
            ),
            IdAClientException::ofError
          );

          try {
            this.client.executeOrElseThrow(c, IdAClientException::ofError);
          } catch (final IdAClientException ex) {
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
      ADMIN_DUPLICATE_ID_NAME,
      ADMIN_NONEXISTENT,
      USER_NONEXISTENT,
      USER_DUPLICATE_ID_NAME,
      PROTOCOL_ERROR,
      EMAIL_DUPLICATE
    );

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
      this.serverCreateAdminInitial("admin", "12345678");

    this.client.loginOrElseThrow(
      new IdAClientCredentials(
        "admin",
        "12345678",
        this.serverAdminAPIURL(),
        Map.of()
      ),
      IdAClientException::ofError
    );

    final var ex =
      assertThrows(IdAClientException.class, () -> {
        this.client.executeOrElseThrow(
          new IdACommandAdminSearchByEmailBegin(
            new IdAdminSearchByEmailParameters(
              IdTimeRange.largest(),
              IdTimeRange.largest(),
              "\0",
              new IdAdminColumnOrdering(IdAdminColumn.BY_IDNAME, true),
              100
            )
          ),
          IdAClientException::ofError
        );
      });

    assertTrue(ex.getMessage().contains("invalid byte sequence for encoding"));
    assertEquals(PROTOCOL_ERROR, ex.errorCode());
  }
}

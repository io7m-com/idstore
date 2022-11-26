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

import com.io7m.idstore.admin_client.IdAClients;
import com.io7m.idstore.admin_client.api.IdAClientException;
import com.io7m.idstore.admin_client.api.IdAClientType;
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdAdminPermissionSet;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdNonEmptyList;
import com.io7m.idstore.model.IdPasswordAlgorithmRedacted;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.protocol.admin.IdACommandAdminSelf;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSelf;
import com.io7m.idstore.protocol.admin.IdAResponseError;
import com.io7m.idstore.protocol.admin.IdAResponseLogin;
import com.io7m.idstore.protocol.admin.IdAResponseUserBanDelete;
import com.io7m.idstore.protocol.admin.cb.IdACB1Messages;
import com.io7m.verdant.core.VProtocolSupported;
import com.io7m.verdant.core.VProtocols;
import com.io7m.verdant.core.cb.VProtocolMessages;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.AUTHENTICATION_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class IdAClientTest
{
  private ClientAndServer mockServer;
  private IdACB1Messages messages;
  private IdAdmin admin;
  private IdAClients clients;
  private IdAClientType client;
  private VProtocols versions;
  private VProtocolMessages versionMessages;
  private byte[] versionHeader;

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
      this.clients.create(Locale.ROOT);

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

    this.mockServer =
      ClientAndServer.startClientAndServer(Integer.valueOf(60000));
  }

  @AfterEach
  public void tearDown()
    throws Exception
  {
    this.mockServer.close();
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
    this.mockServer.when(
      HttpRequest.request()
        .withPath("/")
    ).respond(
      HttpResponse.response()
        .withBody(this.versionHeader)
        .withHeader("Content-Type", "application/verdant+cedarbridge")
    );

    this.mockServer.when(
      HttpRequest.request()
        .withPath("/v1/login")
    ).respond(
      HttpResponse.response()
        .withBody(this.messages.serialize(new IdAResponseLogin(
          UUID.randomUUID(),
          this.admin)))
        .withHeader("Content-Type", IdACB1Messages.contentType())
    );

    this.mockServer.when(
      HttpRequest.request()
        .withPath("/v1/command"),
      Times.exactly(1)
    ).respond(
      HttpResponse.response()
        .withStatusCode(401)
        .withBody(this.messages.serialize(
          new IdAResponseError(
            UUID.randomUUID(),
            AUTHENTICATION_ERROR.id(),
            "error")))
        .withHeader("Content-Type", IdACB1Messages.contentType())
    );

    this.mockServer.when(
      HttpRequest.request()
        .withPath("/v1/command"),
      Times.exactly(1)
    ).respond(
      HttpResponse.response()
        .withStatusCode(200)
        .withBody(this.messages.serialize(
          new IdAResponseAdminSelf(UUID.randomUUID(), this.admin)))
        .withHeader("Content-Type", IdACB1Messages.contentType())
    );

    this.client.login(
      "someone",
      "whatever",
      URI.create("http://localhost:60000/")
    );

    final var result = this.client.adminSelf();
    assertEquals(this.admin.id(), result.id());
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
    this.mockServer.when(
      HttpRequest.request()
        .withPath("/")
    ).respond(
      HttpResponse.response()
        .withBody(this.versionHeader)
        .withHeader("Content-Type", "application/verdant+cedarbridge")
    );

    this.mockServer.when(
      HttpRequest.request()
        .withPath("/v1/login")
    ).respond(
      HttpResponse.response()
        .withBody(this.messages.serialize(new IdAResponseLogin(
          UUID.randomUUID(),
          this.admin)))
        .withHeader("Content-Type", IdACB1Messages.contentType())
    );

    this.mockServer.when(
      HttpRequest.request()
        .withPath("/v1/command"),
      Times.exactly(1)
    ).respond(
      HttpResponse.response()
        .withStatusCode(200)
        .withBody(this.messages.serialize(new IdACommandAdminSelf()))
        .withHeader("Content-Type", IdACB1Messages.contentType())
    );

    this.client.login(
      "someone",
      "whatever",
      URI.create("http://localhost:60000/")
    );

    final var ex =
      assertThrows(IdAClientException.class, () -> this.client.adminSelf());

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
    this.mockServer.when(
      HttpRequest.request()
        .withPath("/")
    ).respond(
      HttpResponse.response()
        .withBody(this.versionHeader)
        .withHeader("Content-Type", "application/verdant+cedarbridge")
    );

    this.mockServer.when(
      HttpRequest.request()
        .withPath("/v1/login")
    ).respond(
      HttpResponse.response()
        .withBody(this.messages.serialize(new IdAResponseLogin(
          UUID.randomUUID(),
          this.admin)))
        .withHeader("Content-Type", IdACB1Messages.contentType())
    );

    this.mockServer.when(
      HttpRequest.request()
        .withPath("/v1/command"),
      Times.exactly(1)
    ).respond(
      HttpResponse.response()
        .withStatusCode(200)
        .withBody(this.messages.serialize(
          new IdAResponseUserBanDelete(UUID.randomUUID())))
        .withHeader("Content-Type", IdACB1Messages.contentType())
    );

    this.client.login(
      "someone",
      "whatever",
      URI.create("http://localhost:60000/")
    );

    final var ex =
      assertThrows(IdAClientException.class, () -> this.client.adminSelf());

    assertEquals(PROTOCOL_ERROR, ex.errorCode());
  }
}

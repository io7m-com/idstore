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


package com.io7m.idstore.admin_client.internal;

import com.io7m.genevan.core.GenProtocolException;
import com.io7m.genevan.core.GenProtocolIdentifier;
import com.io7m.genevan.core.GenProtocolServerEndpointType;
import com.io7m.genevan.core.GenProtocolSolved;
import com.io7m.genevan.core.GenProtocolSolver;
import com.io7m.genevan.core.GenProtocolVersion;
import com.io7m.idstore.admin_client.api.IdAClientException;
import com.io7m.idstore.protocol.admin_v1.IdA1Messages;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.versions.IdVMessageType;
import com.io7m.idstore.protocol.versions.IdVMessages;
import com.io7m.idstore.protocol.versions.IdVProtocols;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.HTTP_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.IO_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.NO_SUPPORTED_PROTOCOLS;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static java.net.http.HttpResponse.BodyHandlers.ofByteArray;

/**
 * Functions to negotiate protocols.
 */

public final class IdAProtocolNegotiation
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdAProtocolNegotiation.class);

  private IdAProtocolNegotiation()
  {

  }

  private static List<IdAServerEndpoint> fetchSupportedVersions(
    final URI base,
    final HttpClient httpClient,
    final IdAStrings strings)
    throws InterruptedException, IdAClientException
  {
    LOG.debug("retrieving supported server protocols");

    final var vMessages =
      new IdVMessages();

    final var request =
      HttpRequest.newBuilder(base)
        .GET()
        .build();

    final HttpResponse<byte[]> response;
    try {
      response = httpClient.send(request, ofByteArray());
    } catch (final IOException e) {
      throw new IdAClientException(IO_ERROR, e);
    }

    LOG.debug("server: status {}", response.statusCode());

    if (response.statusCode() >= 400) {
      throw new IdAClientException(
        HTTP_ERROR,
        strings.format("httpError", Integer.valueOf(response.statusCode()))
      );
    }

    final IdVMessageType message;
    try {
      message = vMessages.parse(response.body());
    } catch (final IdProtocolException e) {
      throw new IdAClientException(e.errorCode(), e);
    }

    if (message instanceof IdVProtocols protocols) {
      return protocols.protocols()
        .stream()
        .map(v -> {
          return new IdAServerEndpoint(
            new GenProtocolIdentifier(
              v.id(),
              new GenProtocolVersion(
                v.versionMajor(),
                v.versionMinor()
              )
            ),
            v.endpointPath()
          );
        }).toList();
    }

    throw new IdAClientException(
      PROTOCOL_ERROR,
      strings.format(
        "unexpectedMessage", "IdVProtocols", message.getClass())
    );
  }

  private record IdAServerEndpoint(
    GenProtocolIdentifier supported,
    String endpoint)
    implements GenProtocolServerEndpointType
  {
    IdAServerEndpoint
    {
      Objects.requireNonNull(supported, "supported");
      Objects.requireNonNull(endpoint, "endpoint");
    }
  }

  /**
   * Negotiate a protocol handler.
   *
   * @param locale     The locale
   * @param httpClient The HTTP client
   * @param strings    The string resources
   * @param admin      The admin
   * @param password   The password
   * @param base       The base URI
   *
   * @return The protocol handler
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  public static IdAClientProtocolHandlerType negotiateProtocolHandler(
    final Locale locale,
    final HttpClient httpClient,
    final IdAStrings strings,
    final String admin,
    final String password,
    final URI base)
    throws IdAClientException, InterruptedException
  {
    Objects.requireNonNull(locale, "locale");
    Objects.requireNonNull(httpClient, "httpClient");
    Objects.requireNonNull(strings, "strings");
    Objects.requireNonNull(admin, "admin");
    Objects.requireNonNull(password, "password");
    Objects.requireNonNull(base, "base");

    final var clientSupports =
      List.of(
        new IdAClientProtocolHandlers1()
      );

    final var serverProtocols =
      fetchSupportedVersions(base, httpClient, strings);

    LOG.debug("server supports {} protocols", serverProtocols.size());

    final var solver =
      GenProtocolSolver.<IdAClientProtocolHandlerFactoryType, IdAServerEndpoint>create(locale);

    final GenProtocolSolved<IdAClientProtocolHandlerFactoryType, IdAServerEndpoint> solved;
    try {
      solved = solver.solve(
        serverProtocols,
        clientSupports,
        List.of(IdA1Messages.schemaId())
      );
    } catch (final GenProtocolException e) {
      throw new IdAClientException(NO_SUPPORTED_PROTOCOLS, e.getMessage(), e);
    }

    final var serverEndpoint =
      solved.serverEndpoint();
    final var target =
      base.resolve(serverEndpoint.endpoint())
        .normalize();

    final var protocol = serverEndpoint.supported();
    LOG.debug(
      "using protocol {} {}.{} at endpoint {}",
      protocol.identifier(),
      protocol.version().versionMajor(),
      protocol.version().versionMinor(),
      target
    );

    return solved.clientHandler().createHandler(
      httpClient,
      strings,
      target
    );
  }
}

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

package com.io7m.idstore.admin_client.internal;

import com.io7m.genevan.core.GenProtocolException;
import com.io7m.genevan.core.GenProtocolIdentifier;
import com.io7m.genevan.core.GenProtocolServerEndpointType;
import com.io7m.genevan.core.GenProtocolSolved;
import com.io7m.genevan.core.GenProtocolSolver;
import com.io7m.genevan.core.GenProtocolVersion;
import com.io7m.idstore.admin_client.api.IdAClientConfiguration;
import com.io7m.idstore.admin_client.api.IdAClientException;
import com.io7m.idstore.protocol.admin.cb.IdACB2Messages;
import com.io7m.idstore.strings.IdStringConstants;
import com.io7m.idstore.strings.IdStrings;
import com.io7m.verdant.core.VProtocolException;
import com.io7m.verdant.core.VProtocols;
import com.io7m.verdant.core.cb.VProtocolMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.io7m.idstore.admin_client.internal.IdACompression.decompressResponse;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.HTTP_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.IO_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.NO_SUPPORTED_PROTOCOLS;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.idstore.strings.IdStringConstants.CONNECT_FAILURE;
import static java.net.http.HttpResponse.BodyHandlers.ofByteArray;
import static java.util.Objects.requireNonNullElse;
import static java.util.Optional.empty;

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
    final IdStrings strings)
    throws InterruptedException, IdAClientException
  {
    LOG.debug("Retrieving supported server protocols");

    final var request =
      HttpRequest.newBuilder(base)
        .GET()
        .build();

    final HttpResponse<byte[]> response;
    try {
      response = httpClient.send(request, ofByteArray());
    } catch (final IOException e) {
      throw new IdAClientException(
        requireNonNullElse(e.getMessage(), strings.format(CONNECT_FAILURE)),
        e,
        IO_ERROR,
        Map.of(),
        empty(),
        empty()
      );
    }

    LOG.debug("Server: Status {}", response.statusCode());

    if (response.statusCode() >= 400) {
      final var msg =
        strings.format(IdStringConstants.HTTP_ERROR, Integer.valueOf(response.statusCode()));

      throw new IdAClientException(
        msg,
        HTTP_ERROR,
        Map.ofEntries(
          Map.entry("Status", Integer.toString(response.statusCode()))
        ),
        empty(),
        empty()
      );
    }

    final var protocols =
      VProtocolMessages.create();

    final VProtocols message;
    try {
      final var body = decompressResponse(response, response.headers());
      message = protocols.parse(base, body);
    } catch (final VProtocolException e) {
      throw new IdAClientException(
        requireNonNullElse(e.getMessage(), e.getClass().getSimpleName()),
        e,
        PROTOCOL_ERROR,
        Map.of(),
        empty(),
        empty()
      );
    } catch (final IOException e) {
      throw new IdAClientException(
        requireNonNullElse(
          e.getMessage(),
          strings.format(CONNECT_FAILURE)
        ),
        e,
        IO_ERROR,
        Map.of(),
        empty(),
        empty()
      );
    }

    return message.protocols()
      .stream()
      .map(v -> {
        return new IdAServerEndpoint(
          new GenProtocolIdentifier(
            v.id().toString(),
            new GenProtocolVersion(
              new BigInteger(Long.toUnsignedString(v.versionMajor())),
              new BigInteger(Long.toUnsignedString(v.versionMinor()))
            )
          ),
          v.endpointPath()
        );
      }).toList();
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
   * Negotiate a protocol transport.
   *
   * @param configuration The configuration
   * @param httpClient    The HTTP client
   * @param strings       The string resources
   * @param base          The base URI
   *
   * @return The protocol transport
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  public static IdATransportType negotiateTransport(
    final IdAClientConfiguration configuration,
    final HttpClient httpClient,
    final IdStrings strings,
    final URI base)
    throws IdAClientException, InterruptedException
  {
    Objects.requireNonNull(configuration, "configuration");
    Objects.requireNonNull(httpClient, "httpClient");
    Objects.requireNonNull(strings, "strings");
    Objects.requireNonNull(base, "base");

    final var clientSupports =
      List.of(
        new IdATransports2()
      );

    final var serverProtocols =
      fetchSupportedVersions(base, httpClient, strings);

    LOG.debug("Server supports {} protocols", serverProtocols.size());

    final var solver =
      GenProtocolSolver.<IdATransportFactoryType, IdAServerEndpoint>
        create(configuration.locale());

    final GenProtocolSolved<IdATransportFactoryType, IdAServerEndpoint> solved;
    try {
      solved = solver.solve(
        serverProtocols,
        clientSupports,
        List.of(IdACB2Messages.protocolId().toString())
      );
    } catch (final GenProtocolException e) {
      throw new IdAClientException(
        requireNonNullElse(e.getMessage(), e.getClass().getSimpleName()),
        e,
        NO_SUPPORTED_PROTOCOLS,
        Map.of(),
        empty(),
        empty()
      );
    }

    final var serverEndpoint =
      solved.serverEndpoint();
    final var target =
      base.resolve(serverEndpoint.endpoint())
        .normalize();

    final var protocol = serverEndpoint.supported();
    LOG.debug(
      "Using protocol {} {}.{} at endpoint {}",
      protocol.identifier(),
      protocol.version().versionMajor(),
      protocol.version().versionMinor(),
      target
    );

    return solved.clientHandler().createTransport(
      configuration,
      httpClient,
      strings,
      target
    );
  }
}

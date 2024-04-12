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

import com.io7m.hibiscus.api.HBClientHandlerType;
import com.io7m.hibiscus.api.HBConnectionError;
import com.io7m.hibiscus.api.HBConnectionResultType;
import com.io7m.hibiscus.api.HBTransportClosed;
import com.io7m.hibiscus.api.HBTransportType;
import com.io7m.idstore.admin_client.api.IdAClientConfiguration;
import com.io7m.idstore.admin_client.api.IdAClientConnectionParameters;
import com.io7m.idstore.admin_client.api.IdAClientException;
import com.io7m.idstore.protocol.admin.IdAMessageType;
import com.io7m.idstore.strings.IdStrings;

import java.net.http.HttpClient;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * The initial "disconnected" protocol handler.
 */

final class IdAHandlerDisconnected extends IdAHandlerAbstract
{
  private final HBTransportType<IdAMessageType, IdAClientException> transport;
  private final Supplier<HttpClient> httpClients;

  /**
   * Construct a handler.
   *
   * @param inConfiguration The configuration
   * @param inStrings       The string resources
   * @param inHttpClient    The client
   */

  IdAHandlerDisconnected(
    final IdAClientConfiguration inConfiguration,
    final IdStrings inStrings,
    final Supplier<HttpClient> inHttpClient)
  {
    super(inConfiguration, inStrings);

    this.transport =
      new HBTransportClosed<>(IdAClientException::ofException);
    this.httpClients =
      Objects.requireNonNull(inHttpClient, "inHttpClient");
  }

  @Override
  public HBConnectionResultType<
    IdAMessageType,
    IdAClientConnectionParameters,
    HBClientHandlerType<
      IdAMessageType,
      IdAClientConnectionParameters,
      IdAClientException>,
    IdAClientException>
  doConnect(
    final IdAClientConnectionParameters parameters)
    throws InterruptedException
  {
    try {
      final var httpClient =
        this.httpClients.get();

      final var newTransport =
        IdAProtocolNegotiation.negotiateTransport(
          this.configuration(),
          httpClient,
          this.strings(),
          parameters.baseURI()
        );

      final var newHandler =
        new IdAHandlerConnected(
          this.configuration(),
          this.strings(),
          newTransport
        );

      return newHandler.doConnect(parameters);
    } catch (final IdAClientException e) {
      return new HBConnectionError<>(e);
    }
  }

  @Override
  public HBTransportType<IdAMessageType, IdAClientException> transport()
  {
    return this.transport;
  }

  @Override
  public boolean isClosed()
  {
    return this.transport.isClosed();
  }

  @Override
  public void close()
    throws IdAClientException
  {
    this.transport.close();
  }

  @Override
  public String toString()
  {
    return "[%s 0x%s]".formatted(
      this.getClass().getSimpleName(),
      Integer.toUnsignedString(this.hashCode(), 16)
    );
  }
}

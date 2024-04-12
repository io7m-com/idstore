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

package com.io7m.idstore.user_client.internal;

import com.io7m.hibiscus.api.HBClientHandlerType;
import com.io7m.hibiscus.api.HBConnectionError;
import com.io7m.hibiscus.api.HBConnectionResultType;
import com.io7m.hibiscus.api.HBTransportClosed;
import com.io7m.hibiscus.api.HBTransportType;
import com.io7m.idstore.protocol.user.IdUMessageType;
import com.io7m.idstore.strings.IdStrings;
import com.io7m.idstore.user_client.api.IdUClientConfiguration;
import com.io7m.idstore.user_client.api.IdUClientConnectionParameters;
import com.io7m.idstore.user_client.api.IdUClientException;

import java.net.http.HttpClient;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * The initial "disconnected" protocol handler.
 */

final class IdUHandlerDisconnected extends IdUHandlerAbstract
{
  private final HBTransportType<IdUMessageType, IdUClientException> transport;
  private final Supplier<HttpClient> httpClients;

  /**
   * Construct a handler.
   *
   * @param inConfiguration The configuration
   * @param inStrings       The string resources
   * @param inHttpClients    The HTTP client supplier
   */

  IdUHandlerDisconnected(
    final IdUClientConfiguration inConfiguration,
    final IdStrings inStrings,
    final Supplier<HttpClient> inHttpClients)
  {
    super(inConfiguration, inStrings);

    this.transport =
      new HBTransportClosed<>(IdUClientException::ofException);
    this.httpClients =
      Objects.requireNonNull(inHttpClients, "inHttpClients");
  }

  @Override
  public HBConnectionResultType<
    IdUMessageType,
    IdUClientConnectionParameters,
    HBClientHandlerType<
      IdUMessageType,
      IdUClientConnectionParameters,
      IdUClientException>,
    IdUClientException>
  doConnect(
    final IdUClientConnectionParameters parameters)
    throws InterruptedException
  {
    try {
      final var httpClient =
        this.httpClients.get();

      final var newTransport =
        IdUProtocolNegotiation.negotiateTransport(
          this.configuration(),
          httpClient,
          this.strings(),
          parameters.baseURI()
        );

      final var newHandler =
        new IdUHandlerConnected(
          this.configuration(),
          this.strings(),
          newTransport
        );

      return newHandler.doConnect(parameters);
    } catch (final IdUClientException e) {
      return new HBConnectionError<>(e);
    }
  }

  @Override
  public HBTransportType<IdUMessageType, IdUClientException> transport()
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
    throws IdUClientException
  {
    this.transport.close();
  }
}

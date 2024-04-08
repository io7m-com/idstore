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

import com.io7m.hibiscus.api.HBConnection;
import com.io7m.hibiscus.api.HBConnectionClosed;
import com.io7m.hibiscus.api.HBConnectionType;
import com.io7m.hibiscus.basic.HBConnectionError;
import com.io7m.hibiscus.basic.HBConnectionResultType;
import com.io7m.idstore.admin_client.api.IdAClientConfiguration;
import com.io7m.idstore.admin_client.api.IdAClientConnectionParameters;
import com.io7m.idstore.admin_client.api.IdAClientException;
import com.io7m.idstore.protocol.admin.IdAMessageType;
import com.io7m.idstore.strings.IdStrings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;

/**
 * The initial "disconnected" protocol handler.
 */

final class IdAHandlerDisconnected extends IdAHandlerAbstract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdAHandlerDisconnected.class);

  private final HBConnectionClosed<IdAMessageType, IdAClientException> connection;

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
    final HttpClient inHttpClient)
  {
    super(inConfiguration, inStrings, inHttpClient);

    this.connection =
      new HBConnectionClosed<>(IdAClientException::ofException);
  }

  @Override
  public HBConnectionResultType<
    IdAMessageType,
    IdAClientConnectionParameters,
    IdAClientException>
  doConnect(
    final IdAClientConnectionParameters parameters)
    throws InterruptedException
  {
    try {
      final var transport =
        IdAProtocolNegotiation.negotiateTransport(
          this.configuration(),
          this.httpClient(),
          this.strings(),
          parameters.baseURI()
        );

      final var newConnection =
        new HBConnection<>(
          this.configuration().clock(),
          transport,
          this.configuration().receiveQueueBounds()
        );

      final var newHandler =
        new IdAHandlerConnected(
          this.configuration(),
          this.strings(),
          this.httpClient(),
          newConnection
        );

      return newHandler.doConnect(parameters);
    } catch (final IdAClientException e) {
      return new HBConnectionError<>(e);
    }
  }

  @Override
  public HBConnectionType<IdAMessageType, IdAClientException> connection()
  {
    return this.connection;
  }

  @Override
  public void close()
    throws IdAClientException
  {
    this.connection.close();
  }
}

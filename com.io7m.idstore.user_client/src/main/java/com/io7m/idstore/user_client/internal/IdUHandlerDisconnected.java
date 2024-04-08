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

import com.io7m.hibiscus.api.HBConnection;
import com.io7m.hibiscus.api.HBConnectionClosed;
import com.io7m.hibiscus.api.HBConnectionType;
import com.io7m.hibiscus.basic.HBConnectionError;
import com.io7m.hibiscus.basic.HBConnectionResultType;
import com.io7m.idstore.protocol.user.IdUMessageType;
import com.io7m.idstore.strings.IdStrings;
import com.io7m.idstore.user_client.api.IdUClientConfiguration;
import com.io7m.idstore.user_client.api.IdUClientConnectionParameters;
import com.io7m.idstore.user_client.api.IdUClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;

/**
 * The initial "disconnected" protocol handler.
 */

final class IdUHandlerDisconnected extends IdUHandlerAbstract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdUHandlerDisconnected.class);

  private final HBConnectionClosed<IdUMessageType, IdUClientException> connection;

  /**
   * Construct a handler.
   *
   * @param inConfiguration The configuration
   * @param inStrings       The string resources
   * @param inHttpClient    The client
   */

  IdUHandlerDisconnected(
    final IdUClientConfiguration inConfiguration,
    final IdStrings inStrings,
    final HttpClient inHttpClient)
  {
    super(inConfiguration, inStrings, inHttpClient);

    this.connection =
      new HBConnectionClosed<>(IdUClientException::ofException);
  }

  @Override
  public HBConnectionResultType<
    IdUMessageType,
    IdUClientConnectionParameters,
    IdUClientException>
  doConnect(
    final IdUClientConnectionParameters parameters)
    throws InterruptedException
  {
    try {
      final var transport =
        IdUProtocolNegotiation.negotiateTransport(
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
        new IdUHandlerConnected(
          this.configuration(),
          this.strings(),
          this.httpClient(),
          newConnection
        );

      return newHandler.doConnect(parameters);
    } catch (final IdUClientException e) {
      return new HBConnectionError<>(e);
    }
  }

  @Override
  public HBConnectionType<IdUMessageType, IdUClientException> connection()
  {
    return this.connection;
  }

  @Override
  public void close()
    throws IdUClientException
  {
    this.connection.close();
  }
}

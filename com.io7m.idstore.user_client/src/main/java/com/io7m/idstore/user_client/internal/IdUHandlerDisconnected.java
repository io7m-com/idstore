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

import com.io7m.hibiscus.api.HBResultFailure;
import com.io7m.hibiscus.api.HBResultType;
import com.io7m.hibiscus.basic.HBClientNewHandler;
import com.io7m.idstore.error_codes.IdStandardErrorCodes;
import com.io7m.idstore.protocol.user.IdUCommandType;
import com.io7m.idstore.protocol.user.IdUResponseError;
import com.io7m.idstore.protocol.user.IdUResponseType;
import com.io7m.idstore.user_client.api.IdUClientConfiguration;
import com.io7m.idstore.user_client.api.IdUClientCredentials;
import com.io7m.idstore.user_client.api.IdUClientEventType;
import com.io7m.idstore.user_client.api.IdUClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.idstore.protocol.user.IdUResponseBlame.BLAME_CLIENT;

/**
 * The initial "disconnected" protocol handler.
 */

public final class IdUHandlerDisconnected extends IdUHandlerAbstract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdUHandlerDisconnected.class);

  /**
   * Construct a handler.
   *
   * @param inConfiguration The configuration
   * @param inStrings       The string resources
   * @param inHttpClient    The client
   */

  public IdUHandlerDisconnected(
    final IdUClientConfiguration inConfiguration,
    final IdUStrings inStrings,
    final HttpClient inHttpClient)
  {
    super(inConfiguration, inStrings, inHttpClient);
  }

  private <A> HBResultFailure<A, IdUResponseError> notLoggedIn()
  {
    return new HBResultFailure<>(
      new IdUResponseError(
        UUID.randomUUID(),
        this.strings().format("notLoggedIn"),
        IdStandardErrorCodes.NOT_LOGGED_IN,
        Map.of(),
        Optional.empty(),
        BLAME_CLIENT
      )
    );
  }

  @Override
  public boolean onIsConnected()
  {
    return false;
  }

  @Override
  public List<IdUClientEventType> onPollEvents()
  {
    return List.of();
  }

  @Override
  public HBResultType<HBClientNewHandler<
    IdUClientException,
    IdUCommandType<?>,
    IdUResponseType,
    IdUResponseType,
    IdUResponseError,
    IdUClientEventType,
    IdUClientCredentials>,
    IdUResponseError>
  onExecuteLogin(
    final IdUClientCredentials credentials)
    throws InterruptedException
  {
    try {
      final var handler =
        IdUProtocolNegotiation.negotiateProtocolHandler(
          this.configuration(),
          this.httpClient(),
          this.strings(),
          credentials.baseURI()
        );

      LOG.debug("login: negotiated {}", handler);
      return handler.onExecuteLogin(credentials);
    } catch (final IdUClientException e) {
      LOG.debug("login: ", e);
      return new HBResultFailure<>(
        new IdUResponseError(
          UUID.randomUUID(),
          e.message(),
          e.errorCode(),
          e.attributes(),
          Optional.empty(),
          BLAME_CLIENT
        )
      );
    }
  }

  @Override
  public HBResultType<IdUResponseType, IdUResponseError>
  onExecuteCommand(
    final IdUCommandType<?> command)
  {
    return this.notLoggedIn();
  }

  @Override
  public void onDisconnect()
  {

  }
}

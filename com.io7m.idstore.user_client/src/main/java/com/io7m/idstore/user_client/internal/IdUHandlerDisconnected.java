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
import com.io7m.idstore.error_codes.IdStandardErrorCodes;
import com.io7m.idstore.protocol.user.IdUCommandType;
import com.io7m.idstore.protocol.user.IdUResponseError;
import com.io7m.idstore.protocol.user.IdUResponseType;
import com.io7m.idstore.user_client.api.IdUClientConfiguration;
import com.io7m.idstore.user_client.api.IdUClientCredentials;
import com.io7m.idstore.user_client.api.IdUClientException;

import java.net.http.HttpClient;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The initial "disconnected" protocol handler.
 */

public final class IdUHandlerDisconnected extends IdUHandlerAbstract
{
  IdUHandlerDisconnected(
    final IdUClientConfiguration inConfiguration,
    final IdUStrings inStrings,
    final HttpClient inHttpClient)
  {
    super(inConfiguration, inStrings, inHttpClient);
  }

  @Override
  public void pollEvents()
  {

  }

  @Override
  public <R extends IdUResponseType> HBResultType<R, IdUResponseError> executeCommand(
    final IdUCommandType<R> command)
  {
    return this.notLoggedIn();
  }

  private <A> HBResultFailure<A, IdUResponseError> notLoggedIn()
  {
    return new HBResultFailure<>(
      new IdUResponseError(
        UUID.randomUUID(),
        this.strings().format("notLoggedIn"),
        IdStandardErrorCodes.NOT_LOGGED_IN,
        Map.of(),
        Optional.empty()
      )
    );
  }

  @Override
  public boolean isConnected()
  {
    return false;
  }

  /**
   * Execute the login process.
   *
   * @param credentials The credentials
   *
   * @return The result
   *
   * @throws InterruptedException On interruption
   */

  @Override
  public HBResultType<IdUNewHandler, IdUResponseError> login(
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

      return handler.login(credentials);
    } catch (final IdUClientException e) {
      return new HBResultFailure<>(
        new IdUResponseError(
          UUID.randomUUID(),
          e.message(),
          e.errorCode(),
          e.attributes(),
          Optional.empty()
        )
      );
    }
  }

}

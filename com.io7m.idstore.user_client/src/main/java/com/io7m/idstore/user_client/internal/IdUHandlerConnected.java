/*
 * Copyright © 2024 Mark Raynsford <code@io7m.com> https://www.io7m.com
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
import com.io7m.hibiscus.api.HBConnectionFailed;
import com.io7m.hibiscus.api.HBConnectionResultType;
import com.io7m.hibiscus.api.HBConnectionSucceeded;
import com.io7m.hibiscus.api.HBTransportType;
import com.io7m.idstore.error_codes.IdStandardErrorCodes;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.protocol.user.IdUCommandLogin;
import com.io7m.idstore.protocol.user.IdUMessageType;
import com.io7m.idstore.protocol.user.IdUResponseError;
import com.io7m.idstore.protocol.user.IdUResponseLogin;
import com.io7m.idstore.strings.IdStrings;
import com.io7m.idstore.user_client.api.IdUClientConfiguration;
import com.io7m.idstore.user_client.api.IdUClientConnectionParameters;
import com.io7m.idstore.user_client.api.IdUClientException;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

final class IdUHandlerConnected
  extends IdUHandlerAbstract
{
  private final HBTransportType<IdUMessageType, IdUClientException> transport;
  private IdUCommandLogin mostRecentLogin;
  private Duration loginTimeout;

  IdUHandlerConnected(
    final IdUClientConfiguration inConfiguration,
    final IdStrings inStrings,
    final HBTransportType<IdUMessageType, IdUClientException> inTransport)
  {
    super(inConfiguration, inStrings);

    this.transport =
      Objects.requireNonNull(inTransport, "transport");
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
    final var commandLogin =
      new IdUCommandLogin(
        new IdName(parameters.userName()),
        parameters.password(),
        parameters.attributes()
      );

    this.loginTimeout = parameters.loginTimeout();
    try {
      return this.doLogin(commandLogin);
    } catch (final IdUClientException | TimeoutException e) {
      return new HBConnectionError<>(e);
    }
  }

  @Override
  public IdUMessageType sendAndWait(
    final IdUMessageType message,
    final Duration timeout)
    throws IdUClientException, InterruptedException, TimeoutException
  {
    var attempt = 0;

    while (true) {
      ++attempt;

      final var response =
        this.transport.sendAndWait(message, timeout);

      if (response instanceof final IdUResponseError error) {
        if (!isAuthenticationError(error)) {
          return error;
        }

        if (attempt == 3) {
          return error;
        }

        this.doLogin(this.mostRecentLogin);
        continue;
      }

      return response;
    }
  }

  @Override
  public HBTransportType<IdUMessageType, IdUClientException> transport()
  {
    return this.transport;
  }

  private HBConnectionResultType<
    IdUMessageType,
    IdUClientConnectionParameters,
    HBClientHandlerType<
      IdUMessageType,
      IdUClientConnectionParameters,
      IdUClientException>,
    IdUClientException>
  doLogin(
    final IdUCommandLogin commandLogin)
    throws IdUClientException,
    InterruptedException,
    TimeoutException
  {
    final var response =
      this.transport.sendAndWait(commandLogin, this.loginTimeout);

    return switch (response) {
      case final IdUResponseLogin login -> {
        this.mostRecentLogin = commandLogin;
        yield new HBConnectionSucceeded<>(login, this);
      }

      case final IdUResponseError error -> {
        yield new HBConnectionFailed<>(error);
      }

      default -> {
        yield new HBConnectionFailed<>(response);
      }
    };
  }

  private static boolean isAuthenticationError(
    final IdUResponseError error)
  {
    return Objects.equals(
      error.errorCode(),
      IdStandardErrorCodes.AUTHENTICATION_ERROR
    );
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

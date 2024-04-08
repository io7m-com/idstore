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

import com.io7m.hibiscus.api.HBConnection;
import com.io7m.hibiscus.api.HBConnectionReceiveQueueOverflowException;
import com.io7m.hibiscus.api.HBConnectionType;
import com.io7m.hibiscus.basic.HBClientHandlerAndMessage;
import com.io7m.hibiscus.basic.HBConnectionError;
import com.io7m.hibiscus.basic.HBConnectionFailed;
import com.io7m.hibiscus.basic.HBConnectionResultType;
import com.io7m.hibiscus.basic.HBConnectionSucceeded;
import com.io7m.idstore.user_client.api.IdUClientConfiguration;
import com.io7m.idstore.user_client.api.IdUClientConnectionParameters;
import com.io7m.idstore.user_client.api.IdUClientException;
import com.io7m.idstore.error_codes.IdStandardErrorCodes;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.protocol.user.IdUCommandLogin;
import com.io7m.idstore.protocol.user.IdUMessageType;
import com.io7m.idstore.protocol.user.IdUResponseError;
import com.io7m.idstore.protocol.user.IdUResponseLogin;
import com.io7m.idstore.strings.IdStrings;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

final class IdUHandlerConnected
  extends IdUHandlerAbstract
{
  private final HBConnection<IdUMessageType, IdUClientException> connection;
  private IdUCommandLogin mostRecentLogin;
  private Duration loginTimeout;

  IdUHandlerConnected(
    final IdUClientConfiguration inConfiguration,
    final IdStrings inStrings,
    final HttpClient inHttpClient,
    final HBConnection<IdUMessageType, IdUClientException> inConnection)
  {
    super(inConfiguration, inStrings, inHttpClient);

    this.connection =
      Objects.requireNonNull(inConnection, "connection");
  }

  @Override
  public HBConnectionResultType<IdUMessageType, IdUClientConnectionParameters, IdUClientException>
  doConnect(
    final IdUClientConnectionParameters parameters)
    throws InterruptedException
  {
    final var commandLogin =
      new IdUCommandLogin(
        UUID.randomUUID(),
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

  private HBConnectionResultType<IdUMessageType, IdUClientConnectionParameters, IdUClientException>
  doLogin(
    final IdUCommandLogin commandLogin)
    throws IdUClientException,
    InterruptedException,
    TimeoutException
  {
    final IdUMessageType response;
    try {
      response = this.connection.ask(commandLogin, this.loginTimeout);
    } catch (final HBConnectionReceiveQueueOverflowException e) {
      return new HBConnectionError<>(IdUClientException.ofException(e));
    }

    return switch (response) {
      case final IdUResponseLogin login -> {
        this.mostRecentLogin = commandLogin;
        yield new HBConnectionSucceeded<>(
          new HBClientHandlerAndMessage<>(this, login)
        );
      }

      case final IdUResponseError error -> {
        yield new HBConnectionFailed<>(error);
      }

      default -> {
        yield new HBConnectionFailed<>(response);
      }
    };
  }

  @Override
  public <R extends IdUMessageType> R ask(
    final IdUMessageType message,
    final Duration timeout)
    throws InterruptedException, TimeoutException, IdUClientException
  {
    int attempt = 0;

    while (true) {
      ++attempt;

      final IdUMessageType response;
      try {
        response = this.connection.ask(message, timeout);
      } catch (final HBConnectionReceiveQueueOverflowException e) {
        throw IdUClientException.ofException(e);
      }

      if (response instanceof final IdUResponseError error) {
        if (!isAuthenticationError(error)) {
          return (R) error;
        }

        if (attempt == 3) {
          return (R) error;
        }

        this.doLogin(this.mostRecentLogin);
        continue;
      }

      return (R) response;
    }
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

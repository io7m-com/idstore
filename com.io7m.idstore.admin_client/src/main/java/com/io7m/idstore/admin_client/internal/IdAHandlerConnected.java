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


package com.io7m.idstore.admin_client.internal;

import com.io7m.hibiscus.api.HBConnection;
import com.io7m.hibiscus.api.HBConnectionReceiveQueueOverflowException;
import com.io7m.hibiscus.api.HBConnectionType;
import com.io7m.hibiscus.basic.HBClientHandlerAndMessage;
import com.io7m.hibiscus.basic.HBConnectionError;
import com.io7m.hibiscus.basic.HBConnectionFailed;
import com.io7m.hibiscus.basic.HBConnectionResultType;
import com.io7m.hibiscus.basic.HBConnectionSucceeded;
import com.io7m.idstore.admin_client.api.IdAClientConfiguration;
import com.io7m.idstore.admin_client.api.IdAClientConnectionParameters;
import com.io7m.idstore.admin_client.api.IdAClientException;
import com.io7m.idstore.error_codes.IdStandardErrorCodes;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.protocol.admin.IdACommandLogin;
import com.io7m.idstore.protocol.admin.IdAMessageType;
import com.io7m.idstore.protocol.admin.IdAResponseError;
import com.io7m.idstore.protocol.admin.IdAResponseLogin;
import com.io7m.idstore.strings.IdStrings;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

final class IdAHandlerConnected
  extends IdAHandlerAbstract
{
  private final HBConnection<IdAMessageType, IdAClientException> connection;
  private IdACommandLogin mostRecentLogin;
  private Duration loginTimeout;

  IdAHandlerConnected(
    final IdAClientConfiguration inConfiguration,
    final IdStrings inStrings,
    final HttpClient inHttpClient,
    final HBConnection<IdAMessageType, IdAClientException> inConnection)
  {
    super(inConfiguration, inStrings, inHttpClient);

    this.connection =
      Objects.requireNonNull(inConnection, "connection");
  }

  @Override
  public HBConnectionResultType<IdAMessageType, IdAClientConnectionParameters, IdAClientException>
  doConnect(
    final IdAClientConnectionParameters parameters)
    throws InterruptedException
  {
    final var commandLogin =
      new IdACommandLogin(
        UUID.randomUUID(),
        new IdName(parameters.userName()),
        parameters.password(),
        parameters.attributes()
      );

    this.loginTimeout = parameters.loginTimeout();
    try {
      return this.doLogin(commandLogin);
    } catch (final IdAClientException | TimeoutException e) {
      return new HBConnectionError<>(e);
    }
  }

  private HBConnectionResultType<IdAMessageType, IdAClientConnectionParameters, IdAClientException>
  doLogin(
    final IdACommandLogin commandLogin)
    throws IdAClientException,
    InterruptedException,
    TimeoutException
  {
    final IdAMessageType response;
    try {
      response = this.connection.ask(commandLogin, this.loginTimeout);
    } catch (final HBConnectionReceiveQueueOverflowException e) {
      return new HBConnectionError<>(IdAClientException.ofException(e));
    }

    return switch (response) {
      case final IdAResponseLogin login -> {
        this.mostRecentLogin = commandLogin;
        yield new HBConnectionSucceeded<>(
          new HBClientHandlerAndMessage<>(this, login)
        );
      }

      case final IdAResponseError error -> {
        yield new HBConnectionFailed<>(error);
      }

      default -> {
        yield new HBConnectionFailed<>(response);
      }
    };
  }

  @Override
  public <R extends IdAMessageType> R ask(
    final IdAMessageType message,
    final Duration timeout)
    throws InterruptedException, TimeoutException, IdAClientException
  {
    int attempt = 0;

    while (true) {
      ++attempt;

      final IdAMessageType response;
      try {
        response = this.connection.ask(message, timeout);
      } catch (final HBConnectionReceiveQueueOverflowException e) {
        throw IdAClientException.ofException(e);
      }

      if (response instanceof final IdAResponseError error) {
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
    final IdAResponseError error)
  {
    return Objects.equals(
      error.errorCode(),
      IdStandardErrorCodes.AUTHENTICATION_ERROR
    );
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

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

import com.io7m.hibiscus.api.HBClientHandlerType;
import com.io7m.hibiscus.api.HBConnectionError;
import com.io7m.hibiscus.api.HBConnectionFailed;
import com.io7m.hibiscus.api.HBConnectionResultType;
import com.io7m.hibiscus.api.HBConnectionSucceeded;
import com.io7m.hibiscus.api.HBTransportType;
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

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

final class IdAHandlerConnected
  extends IdAHandlerAbstract
{
  private final HBTransportType<IdAMessageType, IdAClientException> transport;
  private IdACommandLogin mostRecentLogin;
  private Duration loginTimeout;

  IdAHandlerConnected(
    final IdAClientConfiguration inConfiguration,
    final IdStrings inStrings,
    final HBTransportType<IdAMessageType, IdAClientException> inTransport)
  {
    super(inConfiguration, inStrings);

    this.transport =
      Objects.requireNonNull(inTransport, "transport");
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
    final var commandLogin =
      new IdACommandLogin(
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

  @Override
  public IdAMessageType sendAndWait(
    final IdAMessageType message,
    final Duration timeout)
    throws IdAClientException, InterruptedException, TimeoutException
  {
    var attempt = 0;

    while (true) {
      ++attempt;

      final var response =
        this.transport.sendAndWait(message, timeout);

      if (response instanceof final IdAResponseError error) {
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
  public HBTransportType<IdAMessageType, IdAClientException> transport()
  {
    return this.transport;
  }

  private HBConnectionResultType<
    IdAMessageType,
    IdAClientConnectionParameters,
    HBClientHandlerType<
      IdAMessageType,
      IdAClientConnectionParameters,
      IdAClientException>,
    IdAClientException>
  doLogin(
    final IdACommandLogin commandLogin)
    throws IdAClientException,
    InterruptedException,
    TimeoutException
  {
    final var response =
      this.transport.sendAndWait(commandLogin, this.loginTimeout);

    return switch (response) {
      case final IdAResponseLogin login -> {
        this.mostRecentLogin = commandLogin;
        yield new HBConnectionSucceeded<>(login, this);
      }

      case final IdAResponseError error -> {
        yield new HBConnectionFailed<>(error);
      }

      default -> {
        yield new HBConnectionFailed<>(response);
      }
    };
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

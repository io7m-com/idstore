/*
 * Copyright © 2022 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

import com.io7m.idstore.admin_client.api.IdAClientException;
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSelf;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandLogin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandType;
import com.io7m.idstore.protocol.admin_v1.IdA1Messages;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminSelf;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseError;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseLogin;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseType;
import com.io7m.idstore.protocol.api.IdProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Objects;
import java.util.Optional;

import static java.net.http.HttpResponse.BodyHandlers;

/**
 * The version 1 protocol handler.
 */

public final class IdAClientProtocolHandler1
  extends IdAClientProtocolHandlerAbstract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdAClientProtocolHandler1.class);

  private final URI commandURI;
  private final URI transactionURI;
  private final IdA1Messages messages;
  private final URI loginURI;

  /**
   * The version 1 protocol handler.
   *
   * @param inHttpClient The HTTP client
   * @param inStrings    The string resources
   * @param inBase       The base URI
   */

  public IdAClientProtocolHandler1(
    final HttpClient inHttpClient,
    final IdAStrings inStrings,
    final URI inBase)
  {
    super(inHttpClient, inStrings, inBase);

    this.messages =
      new IdA1Messages();

    this.loginURI =
      inBase.resolve("login")
        .normalize();
    this.commandURI =
      inBase.resolve("command")
        .normalize();
    this.transactionURI =
      inBase.resolve("transaction")
        .normalize();
  }

  private static <A, B, E extends Exception> Optional<B> mapPartial(
    final Optional<A> o,
    final FunctionType<A, B, E> f)
    throws E
  {
    if (o.isPresent()) {
      return Optional.of(f.apply(o.get()));
    }
    return Optional.empty();
  }

  @Override
  public IdAClientProtocolHandlerType login(
    final String admin,
    final String password,
    final URI base)
    throws IdAClientException, InterruptedException
  {
    this.sendLogin(new IdA1CommandLogin(admin, password));
    return this;
  }

  private IdA1ResponseLogin sendLogin(
    final IdA1CommandLogin message)
    throws InterruptedException, IdAClientException
  {
    return this.send(this.loginURI, IdA1ResponseLogin.class, message, false)
      .orElseThrow(() -> new IllegalStateException("send() returned empty"));
  }

  private <T extends IdA1ResponseType> T sendCommand(
    final Class<T> responseClass,
    final IdA1CommandType<T> message)
    throws InterruptedException, IdAClientException
  {
    return this.send(this.commandURI, responseClass, message, false)
      .orElseThrow(() -> new IllegalStateException("send() returned empty"));
  }

  private <T extends IdA1ResponseType> Optional<T> sendCommandOptional(
    final Class<T> responseClass,
    final IdA1CommandType<T> message)
    throws InterruptedException, IdAClientException
  {
    return this.send(this.commandURI, responseClass, message, true);
  }

  private <T extends IdA1ResponseType> Optional<T> send(
    final URI uri,
    final Class<T> responseClass,
    final IdA1CommandType<T> message,
    final boolean allowNotFound)
    throws InterruptedException, IdAClientException
  {
    try {
      final var commandType = message.getClass().getSimpleName();
      LOG.debug("sending {} to {}", commandType, uri);

      final var sendBytes =
        this.messages.serialize(message);

      final var request =
        HttpRequest.newBuilder(uri)
          .header("User-Agent", userAgent())
          .POST(HttpRequest.BodyPublishers.ofByteArray(sendBytes))
          .build();

      final var response =
        this.httpClient()
          .send(request, BodyHandlers.ofByteArray());

      LOG.debug("server: status {}", response.statusCode());

      if (response.statusCode() == 404 && allowNotFound) {
        return Optional.empty();
      }

      final var responseHeaders =
        response.headers();

      final var contentType =
        responseHeaders.firstValue("content-type")
          .orElse("application/octet-stream");

      if (!contentType.equals(IdA1Messages.contentType())) {
        throw new IdAClientException(
          this.strings()
            .format(
              "errorContentType",
              commandType,
              IdA1Messages.contentType(),
              contentType)
        );
      }

      final var responseMessage =
        this.messages.parse(response.body());

      if (!(responseMessage instanceof IdA1ResponseType)) {
        throw new IdAClientException(
          this.strings()
            .format(
              "errorResponseType",
              "(unavailable)",
              commandType,
              IdA1ResponseType.class,
              responseMessage.getClass())
        );
      }

      final var responseActual = (IdA1ResponseType) responseMessage;
      if (responseActual instanceof IdA1ResponseError error) {
        throw new IdAClientException(
          this.strings()
            .format(
              "errorResponse",
              error.requestId(),
              commandType,
              Integer.valueOf(response.statusCode()),
              error.errorCode(),
              error.message())
        );
      }

      if (!Objects.equals(responseActual.getClass(), responseClass)) {
        throw new IdAClientException(
          this.strings()
            .format(
              "errorResponseType",
              responseActual.requestId(),
              commandType,
              responseClass,
              responseMessage.getClass())
        );
      }

      return Optional.of(responseClass.cast(responseMessage));
    } catch (final IdProtocolException | IOException e) {
      throw new IdAClientException(e);
    }
  }

  @Override
  public IdAdmin adminSelf()
    throws IdAClientException, InterruptedException
  {
    final var response =
      this.sendCommand(IdA1ResponseAdminSelf.class, new IdA1CommandAdminSelf());

    try {
      return response.admin().toAdmin();
    } catch (final IdPasswordException e) {
      throw new IdAClientException(e);
    }
  }

  private static String userAgent()
  {
    final String version;
    final var pack = IdAClientProtocolHandler1.class.getPackage();
    if (pack != null) {
      version = pack.getImplementationVersion();
    } else {
      version = "0.0.0";
    }
    return "com.io7m.idstore.admin_client/%s".formatted(version);
  }

  interface FunctionType<A, B, E extends Exception>
  {
    B apply(A x)
      throws E;
  }

  private static final class NotFoundException extends Exception
  {
    NotFoundException()
    {

    }
  }
}

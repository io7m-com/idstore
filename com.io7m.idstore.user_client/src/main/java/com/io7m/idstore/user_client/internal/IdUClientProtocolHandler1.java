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


package com.io7m.idstore.user_client.internal;

import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdToken;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.user_v1.IdU1CommandEmailAddBegin;
import com.io7m.idstore.protocol.user_v1.IdU1CommandEmailAddDeny;
import com.io7m.idstore.protocol.user_v1.IdU1CommandEmailAddPermit;
import com.io7m.idstore.protocol.user_v1.IdU1CommandEmailRemoveBegin;
import com.io7m.idstore.protocol.user_v1.IdU1CommandEmailRemoveDeny;
import com.io7m.idstore.protocol.user_v1.IdU1CommandEmailRemovePermit;
import com.io7m.idstore.protocol.user_v1.IdU1CommandLogin;
import com.io7m.idstore.protocol.user_v1.IdU1CommandRealnameUpdate;
import com.io7m.idstore.protocol.user_v1.IdU1CommandType;
import com.io7m.idstore.protocol.user_v1.IdU1CommandUserSelf;
import com.io7m.idstore.protocol.user_v1.IdU1Messages;
import com.io7m.idstore.protocol.user_v1.IdU1ResponseEmailAddBegin;
import com.io7m.idstore.protocol.user_v1.IdU1ResponseEmailAddDeny;
import com.io7m.idstore.protocol.user_v1.IdU1ResponseEmailAddPermit;
import com.io7m.idstore.protocol.user_v1.IdU1ResponseEmailRemoveBegin;
import com.io7m.idstore.protocol.user_v1.IdU1ResponseEmailRemoveDeny;
import com.io7m.idstore.protocol.user_v1.IdU1ResponseEmailRemovePermit;
import com.io7m.idstore.protocol.user_v1.IdU1ResponseError;
import com.io7m.idstore.protocol.user_v1.IdU1ResponseLogin;
import com.io7m.idstore.protocol.user_v1.IdU1ResponseRealnameUpdate;
import com.io7m.idstore.protocol.user_v1.IdU1ResponseType;
import com.io7m.idstore.protocol.user_v1.IdU1ResponseUserSelf;
import com.io7m.idstore.user_client.api.IdUClientException;
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

public final class IdUClientProtocolHandler1
  extends IdUClientProtocolHandlerAbstract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdUClientProtocolHandler1.class);

  private final URI commandURI;
  private final URI transactionURI;
  private final IdU1Messages messages;
  private final URI loginURI;

  /**
   * The version 1 protocol handler.
   *
   * @param inHttpClient The HTTP client
   * @param inStrings    The string resources
   * @param inBase       The base URI
   */

  public IdUClientProtocolHandler1(
    final HttpClient inHttpClient,
    final IdUStrings inStrings,
    final URI inBase)
  {
    super(inHttpClient, inStrings, inBase);

    this.messages =
      new IdU1Messages();

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
  public IdUClientProtocolHandlerType login(
    final String user,
    final String password,
    final URI base)
    throws IdUClientException, InterruptedException
  {
    this.sendLogin(new IdU1CommandLogin(user, password));
    return this;
  }

  private IdU1ResponseLogin sendLogin(
    final IdU1CommandLogin message)
    throws InterruptedException, IdUClientException
  {
    return this.send(this.loginURI, IdU1ResponseLogin.class, message, false)
      .orElseThrow(() -> new IllegalStateException("send() returned empty"));
  }

  private <T extends IdU1ResponseType> T sendCommand(
    final Class<T> responseClass,
    final IdU1CommandType<T> message)
    throws InterruptedException, IdUClientException
  {
    return this.send(this.commandURI, responseClass, message, false)
      .orElseThrow(() -> new IllegalStateException("send() returned empty"));
  }

  private <T extends IdU1ResponseType> Optional<T> sendCommandOptional(
    final Class<T> responseClass,
    final IdU1CommandType<T> message)
    throws InterruptedException, IdUClientException
  {
    return this.send(this.commandURI, responseClass, message, true);
  }

  private <T extends IdU1ResponseType> Optional<T> send(
    final URI uri,
    final Class<T> responseClass,
    final IdU1CommandType<T> message,
    final boolean allowNotFound)
    throws InterruptedException, IdUClientException
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

      if (!contentType.equals(IdU1Messages.contentType())) {
        throw new IdUClientException(
          this.strings()
            .format(
              "errorContentType",
              commandType,
              IdU1Messages.contentType(),
              contentType)
        );
      }

      final var responseMessage =
        this.messages.parse(response.body());

      if (!(responseMessage instanceof IdU1ResponseType)) {
        throw new IdUClientException(
          this.strings()
            .format(
              "errorResponseType",
              "(unavailable)",
              commandType,
              IdU1ResponseType.class,
              responseMessage.getClass())
        );
      }

      final var responseActual = (IdU1ResponseType) responseMessage;
      if (responseActual instanceof IdU1ResponseError error) {
        throw new IdUClientException(
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
        throw new IdUClientException(
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
      throw new IdUClientException(e);
    }
  }

  @Override
  public IdUser userSelf()
    throws IdUClientException, InterruptedException
  {
    try {
      final var response =
        this.sendCommand(
          IdU1ResponseUserSelf.class,
          new IdU1CommandUserSelf()
        );

      return response.user().toUser();
    } catch (final IdPasswordException e) {
      throw new IdUClientException(e);
    }
  }

  @Override
  public void userEmailAddBegin(final IdEmail email)
    throws IdUClientException, InterruptedException
  {
    this.sendCommand(
      IdU1ResponseEmailAddBegin.class,
      new IdU1CommandEmailAddBegin(email.value())
    );
  }

  @Override
  public void userEmailAddPermit(
    final IdToken token)
    throws IdUClientException, InterruptedException
  {
    this.sendCommand(
      IdU1ResponseEmailAddPermit.class,
      new IdU1CommandEmailAddPermit(token.value())
    );
  }

  @Override
  public void userEmailAddDeny(
    final IdToken token)
    throws IdUClientException, InterruptedException
  {
    this.sendCommand(
      IdU1ResponseEmailAddDeny.class,
      new IdU1CommandEmailAddDeny(token.value())
    );
  }

  @Override
  public void userEmailRemoveBegin(
    final IdEmail email)
    throws IdUClientException, InterruptedException
  {
    this.sendCommand(
      IdU1ResponseEmailRemoveBegin.class,
      new IdU1CommandEmailRemoveBegin(email.value())
    );
  }

  @Override
  public void userEmailRemovePermit(
    final IdToken token)
    throws IdUClientException, InterruptedException
  {
    this.sendCommand(
      IdU1ResponseEmailRemovePermit.class,
      new IdU1CommandEmailRemovePermit(token.value())
    );
  }

  @Override
  public void userEmailRemoveDeny(
    final IdToken token)
    throws IdUClientException, InterruptedException
  {
    this.sendCommand(
      IdU1ResponseEmailRemoveDeny.class,
      new IdU1CommandEmailRemoveDeny(token.value())
    );
  }

  @Override
  public void userRealNameUpdate(
    final IdRealName realName)
    throws IdUClientException, InterruptedException
  {
    this.sendCommand(
      IdU1ResponseRealnameUpdate.class,
      new IdU1CommandRealnameUpdate(realName.value())
    );
  }

  private static String userAgent()
  {
    final String version;
    final var pack = IdUClientProtocolHandler1.class.getPackage();
    if (pack != null) {
      version = pack.getImplementationVersion();
    } else {
      version = "0.0.0";
    }
    return "com.io7m.idstore.user_client/%s".formatted(version);
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

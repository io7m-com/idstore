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

import com.io7m.idstore.error_codes.IdErrorCode;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdToken;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.user.IdUCommandEmailAddBegin;
import com.io7m.idstore.protocol.user.IdUCommandEmailAddDeny;
import com.io7m.idstore.protocol.user.IdUCommandEmailAddPermit;
import com.io7m.idstore.protocol.user.IdUCommandEmailRemoveBegin;
import com.io7m.idstore.protocol.user.IdUCommandEmailRemoveDeny;
import com.io7m.idstore.protocol.user.IdUCommandEmailRemovePermit;
import com.io7m.idstore.protocol.user.IdUCommandLogin;
import com.io7m.idstore.protocol.user.IdUCommandPasswordUpdate;
import com.io7m.idstore.protocol.user.IdUCommandRealnameUpdate;
import com.io7m.idstore.protocol.user.IdUCommandType;
import com.io7m.idstore.protocol.user.IdUCommandUserSelf;
import com.io7m.idstore.protocol.user.IdUResponseEmailAddBegin;
import com.io7m.idstore.protocol.user.IdUResponseEmailAddDeny;
import com.io7m.idstore.protocol.user.IdUResponseEmailAddPermit;
import com.io7m.idstore.protocol.user.IdUResponseEmailRemoveBegin;
import com.io7m.idstore.protocol.user.IdUResponseEmailRemoveDeny;
import com.io7m.idstore.protocol.user.IdUResponseEmailRemovePermit;
import com.io7m.idstore.protocol.user.IdUResponseError;
import com.io7m.idstore.protocol.user.IdUResponseLogin;
import com.io7m.idstore.protocol.user.IdUResponseType;
import com.io7m.idstore.protocol.user.IdUResponseUserSelf;
import com.io7m.idstore.protocol.user.IdUResponseUserUpdate;
import com.io7m.idstore.protocol.user.cb.IdUCB1Messages;
import com.io7m.idstore.user_client.api.IdUClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.AUTHENTICATION_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.IO_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.idstore.user_client.internal.IdUCompression.decompressResponse;
import static java.net.http.HttpResponse.BodyHandlers;
import static java.util.Optional.empty;

/**
 * The version 1 protocol handler.
 */

public final class IdUClientProtocolHandler1
  extends IdUClientProtocolHandlerAbstract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdUClientProtocolHandler1.class);

  private final URI commandURI;
  private final IdUCB1Messages messages;
  private final URI loginURI;
  private IdUCommandLogin mostRecentLogin;

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
      new IdUCB1Messages();

    this.loginURI =
      inBase.resolve("login")
        .normalize();
    this.commandURI =
      inBase.resolve("command")
        .normalize();
  }

  @Override
  public IdUNewHandler login(
    final String user,
    final String password,
    final URI base,
    final Map<String, String> metadata)
    throws IdUClientException, InterruptedException
  {
    this.mostRecentLogin =
      new IdUCommandLogin(new IdName(user), password, metadata);
    final var result = this.sendLogin(this.mostRecentLogin).user();
    return new IdUNewHandler(result, this);
  }

  private IdUResponseLogin sendLogin(
    final IdUCommandLogin message)
    throws InterruptedException, IdUClientException
  {
    return this.send(1, this.loginURI, IdUResponseLogin.class, true, message);
  }

  private <T extends IdUResponseType> T sendCommand(
    final Class<T> responseClass,
    final IdUCommandType<T> message)
    throws InterruptedException, IdUClientException
  {
    return this.send(1, this.commandURI, responseClass, false, message);
  }

  private <T extends IdUResponseType> T send(
    final int attempt,
    final URI uri,
    final Class<T> responseClass,
    final boolean isLoggingIn,
    final IdUCommandType<T> message)
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

      final var responseHeaders =
        response.headers();

      final var contentType =
        responseHeaders.firstValue("content-type")
          .orElse("application/octet-stream");

      final var expectedContentType = IdUCB1Messages.contentType();
      final var str = this.strings();
      if (!contentType.equals(expectedContentType)) {
        throw new IdUClientException(
          str.format("errorReasonContentType"),
          PROTOCOL_ERROR,
          Map.ofEntries(
            Map.entry("Expected", expectedContentType),
            Map.entry("Received", contentType)
          ),
          empty(),
          empty()
        );
      }

      final var responseMessage =
        this.messages.parse(decompressResponse(response, responseHeaders));

      if (!(responseMessage instanceof IdUResponseType)) {
        throw new IdUClientException(
          str.format("errorReasonResponseType"),
          PROTOCOL_ERROR,
          Map.ofEntries(
            Map.entry("Expected", IdUResponseType.class.getSimpleName()),
            Map.entry("Received", responseMessage.getClass().getSimpleName())
          ),
          empty(),
          empty()
        );
      }

      final var responseActual = (IdUResponseType) responseMessage;
      if (responseActual instanceof IdUResponseError error) {
        if (attempt < 3) {
          if (isAuthenticationError(error) && !isLoggingIn) {
            LOG.debug("attempting re-login");
            this.sendLogin(this.mostRecentLogin);
            return this.send(
              attempt + 1,
              uri,
              responseClass,
              false,
              message
            );
          }
        }

        throw new IdUClientException(
          error.message(),
          new IdErrorCode(error.errorCode()),
          error.attributes(),
          error.remediatingAction(),
          Optional.of(error.requestId())
        );
      }

      if (!Objects.equals(responseActual.getClass(), responseClass)) {
        throw new IdUClientException(
          str.format("errorReasonResponseType"),
          PROTOCOL_ERROR,
          Map.ofEntries(
            Map.entry("Expected", responseClass.getSimpleName()),
            Map.entry("Received", responseActual.getClass().getSimpleName())
          ),
          empty(),
          empty()
        );
      }

      return responseClass.cast(responseMessage);
    } catch (final IOException e) {
      throw new IdUClientException(
        Objects.requireNonNullElse(e.getMessage(), e.getClass().getSimpleName()),
        e,
        IO_ERROR,
        Map.of(),
        empty(),
        empty()
      );
    } catch (final IdProtocolException e) {
      throw new IdUClientException(
        e.getMessage(),
        e,
        e.errorCode(),
        e.attributes(),
        e.remediatingAction(),
        empty()
      );
    }
  }

  private static boolean isAuthenticationError(
    final IdUResponseError error)
  {
    return Objects.equals(error.errorCode(), AUTHENTICATION_ERROR.id());
  }

  @Override
  public IdUser userSelf()
    throws IdUClientException, InterruptedException
  {
    final var response =
      this.sendCommand(
        IdUResponseUserSelf.class,
        new IdUCommandUserSelf()
      );

    return response.user();
  }

  @Override
  public void userEmailAddBegin(final IdEmail email)
    throws IdUClientException, InterruptedException
  {
    this.sendCommand(
      IdUResponseEmailAddBegin.class,
      new IdUCommandEmailAddBegin(email)
    );
  }

  @Override
  public void userEmailAddPermit(
    final IdToken token)
    throws IdUClientException, InterruptedException
  {
    this.sendCommand(
      IdUResponseEmailAddPermit.class,
      new IdUCommandEmailAddPermit(token)
    );
  }

  @Override
  public void userEmailAddDeny(
    final IdToken token)
    throws IdUClientException, InterruptedException
  {
    this.sendCommand(
      IdUResponseEmailAddDeny.class,
      new IdUCommandEmailAddDeny(token)
    );
  }

  @Override
  public void userEmailRemoveBegin(
    final IdEmail email)
    throws IdUClientException, InterruptedException
  {
    this.sendCommand(
      IdUResponseEmailRemoveBegin.class,
      new IdUCommandEmailRemoveBegin(email)
    );
  }

  @Override
  public void userEmailRemovePermit(
    final IdToken token)
    throws IdUClientException, InterruptedException
  {
    this.sendCommand(
      IdUResponseEmailRemovePermit.class,
      new IdUCommandEmailRemovePermit(token)
    );
  }

  @Override
  public void userEmailRemoveDeny(
    final IdToken token)
    throws IdUClientException, InterruptedException
  {
    this.sendCommand(
      IdUResponseEmailRemoveDeny.class,
      new IdUCommandEmailRemoveDeny(token)
    );
  }

  @Override
  public void userRealNameUpdate(
    final IdRealName realName)
    throws IdUClientException, InterruptedException
  {
    this.sendCommand(
      IdUResponseUserUpdate.class,
      new IdUCommandRealnameUpdate(realName)
    );
  }

  @Override
  public void userPasswordUpdate(
    final String password,
    final String passwordConfirm)
    throws IdUClientException, InterruptedException
  {
    this.sendCommand(
      IdUResponseUserUpdate.class,
      new IdUCommandPasswordUpdate(password, passwordConfirm)
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
}

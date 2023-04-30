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

package com.io7m.idstore.admin_client.internal;

import com.io7m.hibiscus.api.HBResultFailure;
import com.io7m.hibiscus.api.HBResultSuccess;
import com.io7m.hibiscus.api.HBResultType;
import com.io7m.hibiscus.basic.HBClientNewHandler;
import com.io7m.idstore.admin_client.api.IdAClientConfiguration;
import com.io7m.idstore.admin_client.api.IdAClientCredentials;
import com.io7m.idstore.admin_client.api.IdAClientEventType;
import com.io7m.idstore.admin_client.api.IdAClientException;
import com.io7m.idstore.error_codes.IdStandardErrorCodes;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.protocol.admin.IdACommandLogin;
import com.io7m.idstore.protocol.admin.IdACommandType;
import com.io7m.idstore.protocol.admin.IdAMessageType;
import com.io7m.idstore.protocol.admin.IdAResponseError;
import com.io7m.idstore.protocol.admin.IdAResponseLogin;
import com.io7m.idstore.protocol.admin.IdAResponseType;
import com.io7m.idstore.protocol.admin.cb.IdACB1Messages;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.junreachable.UnreachableCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.idstore.admin_client.internal.IdACompression.decompressResponse;
import static com.io7m.idstore.admin_client.internal.IdAUUIDs.nullUUID;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.IO_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static java.util.Objects.requireNonNullElse;

/**
 * The version 1 protocol handler.
 */

public final class IdAHandler1 extends IdAHandlerAbstract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdAHandler1.class);

  private final IdACB1Messages messages;
  private final URI loginURI;
  private final URI commandURI;
  private IdACommandLogin mostRecentLogin;

  /**
   * The protocol 1 handler.
   *
   * @param inConfiguration The client configuration
   * @param inStrings       String resources
   * @param inHttpClient    The HTTP client
   * @param baseURI         The base URI returned by the server during version
   *                        negotiation
   */

  IdAHandler1(
    final IdAClientConfiguration inConfiguration,
    final IdAStrings inStrings,
    final HttpClient inHttpClient,
    final URI baseURI)
  {
    super(inConfiguration, inStrings, inHttpClient);

    this.messages =
      new IdACB1Messages();
    this.loginURI =
      baseURI.resolve("login")
        .normalize();
    this.commandURI =
      baseURI.resolve("command")
        .normalize();
  }

  private static boolean isAuthenticationError(
    final IdAResponseError error)
  {
    return Objects.equals(
      error.errorCode(),
      IdStandardErrorCodes.AUTHENTICATION_ERROR
    );
  }

  private static String userAgent()
  {
    final String version;
    final var pack = IdAHandler1.class.getPackage();
    if (pack != null) {
      version = pack.getImplementationVersion();
    } else {
      version = "0.0.0";
    }
    return "com.io7m.cardant.client/%s".formatted(version);
  }

  private <R extends IdAResponseType, C extends IdACommandType<R>>
  HBResultType<R, IdAResponseError>
  send(
    final int attempt,
    final URI uri,
    final boolean isLoggingIn,
    final C message)
    throws InterruptedException
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
          .send(request, HttpResponse.BodyHandlers.ofByteArray());

      LOG.debug("server: status {}", response.statusCode());

      final var responseHeaders =
        response.headers();

      /*
       * Check the content type. Fail if it's not what we expected.
       */

      final var contentType =
        responseHeaders.firstValue("content-type")
          .orElse("application/octet-stream");

      final var expectedContentType = IdACB1Messages.contentType();
      if (!contentType.equals(expectedContentType)) {
        return this.errorContentType(contentType, expectedContentType);
      }

      /*
       * Parse the response message, decompressing if necessary. If the
       * parsed message isn't a response... fail.
       */

      final var responseMessage =
        this.messages.parse(decompressResponse(response, responseHeaders));

      if (!(responseMessage instanceof final IdAResponseType responseActual)) {
        return this.errorUnexpectedResponseType(message, responseMessage);
      }

      /*
       * If the response is an error, then perhaps retry. We only attempt
       * to retry if the response indicates an authentication error; if this
       * happens, we try to log in again and then re-send the original message.
       *
       * We don't try to blanket re-send any message that "failed" because
       * messages might have side effects on the server.
       */

      if (responseActual instanceof final IdAResponseError error) {
        if (attempt < 3) {
          if (isAuthenticationError(error) && !isLoggingIn) {
            return this.reLoginAndSend(attempt, uri, message);
          }
        }
        return new HBResultFailure<>(error);
      }

      /*
       * We know that the response is an error, but we don't know that the
       * response is of the expected type. Check that here, and fail if it
       * isn't.
       */

      if (!Objects.equals(responseActual.getClass(), message.responseClass())) {
        return this.errorUnexpectedResponseType(message, responseActual);
      }

      return new HBResultSuccess<>(
        message.responseClass().cast(responseMessage)
      );

    } catch (final IdProtocolException e) {
      LOG.debug("protocol exception: ", e);
      return new HBResultFailure<>(
        new IdAResponseError(
          nullUUID(),
          e.message(),
          e.errorCode(),
          e.attributes(),
          Optional.empty()
        )
      );
    } catch (final IOException e) {
      LOG.debug("i/o exception: ", e);
      return new HBResultFailure<>(
        new IdAResponseError(
          nullUUID(),
          requireNonNullElse(
            e.getMessage(),
            this.strings().format("connectFailure")
          ),
          IO_ERROR,
          Map.of(),
          Optional.empty()
        )
      );
    }
  }

  private <R extends IdAResponseType, C extends IdACommandType<R>> HBResultType<R, IdAResponseError>
  reLoginAndSend(
    final int attempt,
    final URI uri,
    final C message)
    throws InterruptedException
  {
    LOG.debug("attempting re-login");
    final var loginResponse =
      this.sendLogin(this.mostRecentLogin);

    if (loginResponse instanceof HBResultSuccess<IdAResponseLogin, IdAResponseError>) {
      return this.send(
        attempt + 1,
        uri,
        false,
        message
      );
    }
    if (loginResponse instanceof final HBResultFailure<IdAResponseLogin, IdAResponseError> failure) {
      return failure.cast();
    }

    throw new UnreachableCodeException();
  }

  private HBResultType<IdAResponseLogin, IdAResponseError> sendLogin(
    final IdACommandLogin login)
    throws InterruptedException
  {
    return this.send(1, this.loginURI, true, login);
  }

  private <R extends IdAResponseType> HBResultFailure<R, IdAResponseError> errorContentType(
    final String contentType,
    final String expectedContentType)
  {
    final var attributes = new HashMap<String, String>();
    attributes.put(
      this.local("Expected Content Type"),
      expectedContentType
    );
    attributes.put(
      this.local("Received Content Type"),
      contentType
    );

    return new HBResultFailure<>(
      new IdAResponseError(
        nullUUID(),
        this.local("Received an unexpected content type."),
        PROTOCOL_ERROR,
        attributes,
        Optional.empty()
      )
    );
  }

  private <R extends IdAResponseType, C extends IdACommandType<R>>
  HBResultFailure<R, IdAResponseError>
  errorUnexpectedResponseType(
    final C message,
    final IdAMessageType responseActual)
  {
    final var attributes = new HashMap<String, String>();
    attributes.put(
      this.local("Expected Response Type"),
      message.responseClass().getSimpleName()
    );
    attributes.put(
      this.local("Received Response Type"),
      responseActual.getClass().getSimpleName()
    );

    return new HBResultFailure<>(
      new IdAResponseError(
        nullUUID(),
        this.local("Received an unexpected response type."),
        PROTOCOL_ERROR,
        attributes,
        Optional.empty()
      )
    );
  }

  private String local(
    final String id,
    final Object... args)
  {
    return this.strings().format(id, args);
  }

  @Override
  public boolean onIsConnected()
  {
    return true;
  }

  @Override
  public List<IdAClientEventType> onPollEvents()
  {
    return List.of();
  }


  @Override
  public HBResultType<
    HBClientNewHandler<
      IdAClientException,
      IdACommandType<?>,
      IdAResponseType,
      IdAResponseType,
      IdAResponseError,
      IdAClientEventType,
      IdAClientCredentials>,
    IdAResponseError>
  onExecuteLogin(
    final IdAClientCredentials credentials)
    throws InterruptedException
  {
    LOG.debug("login: {}", credentials.baseURI());

    this.mostRecentLogin =
      new IdACommandLogin(
        new IdName(credentials.userName()),
        credentials.password(),
        credentials.attributes()
      );

    final var response =
      this.sendLogin(this.mostRecentLogin);

    if (response instanceof final HBResultSuccess<IdAResponseLogin, IdAResponseError> success) {
      LOG.debug("login: succeeded");
      return new HBResultSuccess<>(
        new HBClientNewHandler<>(this, success.result())
      );
    }
    if (response instanceof final HBResultFailure<IdAResponseLogin, IdAResponseError> failure) {
      LOG.debug("login: failed ({})", failure.result().message());
      return failure.cast();
    }

    throw new UnreachableCodeException();
  }

  @Override
  public HBResultType<IdAResponseType, IdAResponseError>
  onExecuteCommand(
    final IdACommandType<?> command)
    throws InterruptedException
  {
    return this.send(1, this.commandURI, false, command)
      .map(x -> x);
  }

  @Override
  public void onDisconnect()
  {

  }

  @Override
  public String toString()
  {
    return String.format(
      "[IdAHandler1 0x%08x]",
      Integer.valueOf(this.hashCode())
    );
  }
}

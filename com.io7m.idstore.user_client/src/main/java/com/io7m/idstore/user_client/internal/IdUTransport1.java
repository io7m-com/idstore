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

import com.io7m.hibiscus.api.HBReadNothing;
import com.io7m.hibiscus.api.HBReadResponse;
import com.io7m.hibiscus.api.HBReadType;
import com.io7m.idstore.error_codes.IdStandardErrorCodes;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.user.IdUCommandLogin;
import com.io7m.idstore.protocol.user.IdUCommandType;
import com.io7m.idstore.protocol.user.IdUMessageType;
import com.io7m.idstore.protocol.user.IdUResponseError;
import com.io7m.idstore.protocol.user.IdUResponseType;
import com.io7m.idstore.protocol.user.cb.IdUCB1Messages;
import com.io7m.idstore.strings.IdStringConstants;
import com.io7m.idstore.strings.IdStrings;
import com.io7m.idstore.user_client.api.IdUClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.idstore.strings.IdStringConstants.ERROR_EXPECTED_COMMAND_TYPE;
import static com.io7m.idstore.strings.IdStringConstants.ERROR_UNEXPECTED_CONTENT_TYPE;
import static com.io7m.idstore.strings.IdStringConstants.ERROR_UNEXPECTED_RESPONSE_TYPE;
import static com.io7m.idstore.strings.IdStringConstants.EXPECTED_CONTENT_TYPE;
import static com.io7m.idstore.strings.IdStringConstants.EXPECTED_RESPONSE_TYPE;
import static com.io7m.idstore.strings.IdStringConstants.RECEIVED_CONTENT_TYPE;
import static com.io7m.idstore.strings.IdStringConstants.RECEIVED_RESPONSE_TYPE;
import static com.io7m.idstore.user_client.internal.IdUCompression.decompressResponse;

/**
 * The version 1 transport.
 */

public final class IdUTransport1
  implements IdUTransportType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdUTransport1.class);

  private final IdUCB1Messages messages;
  private final URI loginURI;
  private final URI commandURI;
  private final HttpClient http;
  private final IdStrings strings;
  private final LinkedBlockingQueue<MessageAndResponse> inbox;

  private record MessageAndResponse(
    IdUMessageType sent,
    IdUMessageType received)
  {

  }

  /**
   * The version 1 transport.
   *
   * @param inStrings    The string resources
   * @param inHttpClient The HTTP client
   * @param baseURI      The base URI
   */

  public IdUTransport1(
    final IdStrings inStrings,
    final HttpClient inHttpClient,
    final URI baseURI)
  {
    this.http =
      Objects.requireNonNull(inHttpClient, "inHttpClient");
    this.strings =
      Objects.requireNonNull(inStrings, "inStrings");

    this.inbox =
      new LinkedBlockingQueue<>();

    this.messages =
      new IdUCB1Messages();
    this.loginURI =
      baseURI.resolve("login")
        .normalize();
    this.commandURI =
      baseURI.resolve("command")
        .normalize();
  }

  private IdUClientException errorClosed()
  {
    return new IdUClientException(
      this.strings.format(IdStringConstants.ERROR_CLOSED_CHANNEL),
      new ClosedChannelException(),
      IdStandardErrorCodes.API_MISUSE_ERROR,
      Map.of(),
      Optional.empty(),
      Optional.empty()
    );
  }

  private MessageAndResponse sendMessage(
    final IdUMessageType message,
    final URI targetURI,
    final Optional<Duration> timeout)
    throws
    IdUClientException,
    IOException,
    IdProtocolException,
    InterruptedException
  {
    if (message instanceof final IdUCommandType<?> command) {
      return this.sendCommand(targetURI, command, timeout);
    } else {
      throw this.errorNotCommand(message);
    }
  }

  private MessageAndResponse sendCommand(
    final URI targetURI,
    final IdUCommandType<?> command,
    final Optional<Duration> timeout)
    throws
    IOException,
    InterruptedException,
    IdUClientException,
    IdProtocolException
  {
    final var data =
      this.messages.serialize(command);

    final var requestBuilder =
      HttpRequest.newBuilder()
        .uri(targetURI)
        .POST(HttpRequest.BodyPublishers.ofByteArray(data));

    timeout.ifPresent(requestBuilder::timeout);

    final var response =
      this.http.send(
        requestBuilder.build(),
        HttpResponse.BodyHandlers.ofByteArray()
      );

    LOG.debug("Write: Status {}", Integer.valueOf(response.statusCode()));

    final var responseHeaders =
      response.headers();

    /*
     * Check the content type. Fail if it's not what we expected.
     */

    final var contentType =
      responseHeaders.firstValue("content-type")
        .orElse("application/octet-stream");

    final var expectedContentType = IdUCB1Messages.contentType();
    if (!contentType.equals(expectedContentType)) {
      throw this.errorContentType(contentType, expectedContentType);
    }

    /*
     * Parse the response message, decompressing if necessary. If the
     * parsed message isn't a response... fail.
     */

    final var responseMessage =
      this.messages.parse(decompressResponse(response, responseHeaders));

    if (!(responseMessage instanceof IdUResponseType)) {
      throw this.errorUnexpectedResponseType(command, responseMessage);
    }

    /*
     * If the response is an error, accept it.
     */

    if (responseMessage instanceof final IdUResponseError error) {
      return new MessageAndResponse(command, error);
    }

    /*
     * Otherwise, reject the response if it isn't of the correct type.
     */

    if (!Objects.equals(command.responseClass(), responseMessage.getClass())) {
      throw this.errorUnexpectedResponseType(command, responseMessage);
    }

    return new MessageAndResponse(command, responseMessage);
  }

  @Override
  public boolean isClosed()
  {
    return this.http.isTerminated();
  }

  @Override
  public void close()
    throws IdUClientException
  {
    this.http.close();
  }

  private IdUClientException errorNotCommand(
    final IdUMessageType message)
  {
    final var attributes = new HashMap<String, String>();
    attributes.put(
      this.strings.format(IdStringConstants.MESSAGE_TYPE),
      message.getClass().getSimpleName()
    );

    return new IdUClientException(
      this.strings.format(ERROR_EXPECTED_COMMAND_TYPE),
      PROTOCOL_ERROR,
      Map.copyOf(attributes),
      Optional.empty(),
      Optional.empty()
    );
  }

  private IdUClientException errorContentType(
    final String contentType,
    final String expectedContentType)
  {
    final var attributes = new HashMap<String, String>();
    attributes.put(
      this.strings.format(EXPECTED_CONTENT_TYPE),
      expectedContentType
    );
    attributes.put(
      this.strings.format(RECEIVED_CONTENT_TYPE),
      contentType
    );

    return new IdUClientException(
      this.strings.format(ERROR_UNEXPECTED_CONTENT_TYPE),
      PROTOCOL_ERROR,
      Map.copyOf(attributes),
      Optional.empty(),
      Optional.empty()
    );
  }

  private IdUClientException errorUnexpectedResponseType(
    final IdUMessageType message,
    final IdUMessageType responseActual)
  {
    final var attributes = new HashMap<String, String>();
    if (message instanceof final IdUCommandType<?> cmd) {
      attributes.put(
        this.strings.format(EXPECTED_RESPONSE_TYPE),
        cmd.responseClass().getSimpleName()
      );
    }

    attributes.put(
      this.strings.format(RECEIVED_RESPONSE_TYPE),
      responseActual.getClass().getSimpleName()
    );

    return new IdUClientException(
      this.strings.format(ERROR_UNEXPECTED_RESPONSE_TYPE),
      PROTOCOL_ERROR,
      Map.copyOf(attributes),
      Optional.empty(),
      Optional.empty()
    );
  }

  @Override
  public HBReadType<IdUMessageType> receive(
    final Duration timeout)
    throws IdUClientException, InterruptedException
  {
    Objects.requireNonNull(timeout, "timeout");

    if (this.isClosed()) {
      throw this.errorClosed();
    }

    final var r =
      this.inbox.poll(timeout.toNanos(), TimeUnit.NANOSECONDS);

    if (r == null) {
      return new HBReadNothing<>();
    }

    return new HBReadResponse<>(r.sent(), r.received());
  }

  @Override
  public void send(
    final IdUMessageType message)
    throws IdUClientException, InterruptedException
  {
    if (this.isClosed()) {
      throw this.errorClosed();
    }

    try {
      this.inbox.put(
        switch (message) {
          case final IdUCommandLogin m -> {
            yield this.sendMessage(
              message,
              this.loginURI,
              Optional.empty()
            );
          }
          default -> {
            yield this.sendMessage(
              message,
              this.commandURI,
              Optional.empty()
            );
          }
        }
      );
    } catch (final IOException | IdProtocolException e) {
      throw IdUClientException.ofException(e);
    }
  }

  @Override
  public void sendAndForget(
    final IdUMessageType message)
    throws IdUClientException, InterruptedException
  {
    if (this.isClosed()) {
      throw this.errorClosed();
    }

    try {
      final var targetURI =
        switch (message) {
          case final IdUCommandLogin ignored -> this.loginURI;
          default -> this.commandURI;
        };

      final var data =
        this.messages.serialize(message);

      final var response =
        this.http.send(
          HttpRequest.newBuilder()
            .uri(targetURI)
            .POST(HttpRequest.BodyPublishers.ofByteArray(data))
            .build(),
          HttpResponse.BodyHandlers.discarding()
        );

      LOG.debug("Send: Status {}", Integer.valueOf(response.statusCode()));
    } catch (final IOException e) {
      throw IdUClientException.ofException(e);
    }
  }

  @Override
  public IdUMessageType sendAndWait(
    final IdUMessageType message,
    final Duration timeout)
    throws IdUClientException, InterruptedException, TimeoutException
  {
    if (this.isClosed()) {
      throw this.errorClosed();
    }

    try {
      return (
        switch (message) {
          case final IdUCommandLogin m -> {
            yield this.sendMessage(
              message,
              this.loginURI,
              Optional.of(timeout)
            );
          }
          default -> {
            yield this.sendMessage(
              message,
              this.commandURI,
              Optional.of(timeout)
            );
          }
        }
      ).received();
    } catch (final IOException | IdProtocolException e) {
      throw IdUClientException.ofException(e);
    }
  }
}

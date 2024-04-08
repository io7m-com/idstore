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

import com.io7m.idstore.admin_client.api.IdAClientException;
import com.io7m.idstore.error_codes.IdStandardErrorCodes;
import com.io7m.idstore.protocol.admin.IdACommandLogin;
import com.io7m.idstore.protocol.admin.IdACommandType;
import com.io7m.idstore.protocol.admin.IdAMessageType;
import com.io7m.idstore.protocol.admin.IdAResponseType;
import com.io7m.idstore.protocol.admin.cb.IdACB2Messages;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.strings.IdStringConstants;
import com.io7m.idstore.strings.IdStrings;
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

import static com.io7m.idstore.admin_client.internal.IdACompression.decompressResponse;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.idstore.strings.IdStringConstants.ERROR_EXPECTED_COMMAND_TYPE;
import static com.io7m.idstore.strings.IdStringConstants.ERROR_UNEXPECTED_CONTENT_TYPE;
import static com.io7m.idstore.strings.IdStringConstants.ERROR_UNEXPECTED_RESPONSE_TYPE;
import static com.io7m.idstore.strings.IdStringConstants.EXPECTED_CONTENT_TYPE;
import static com.io7m.idstore.strings.IdStringConstants.EXPECTED_RESPONSE_TYPE;
import static com.io7m.idstore.strings.IdStringConstants.RECEIVED_CONTENT_TYPE;
import static com.io7m.idstore.strings.IdStringConstants.RECEIVED_RESPONSE_TYPE;

/**
 * The version 1 transport.
 */

public final class IdATransport2
  implements IdATransportType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdATransport2.class);

  private final IdACB2Messages messages;
  private final URI loginURI;
  private final URI commandURI;
  private final HttpClient http;
  private final IdStrings strings;
  private final LinkedBlockingQueue<MessageAndResponse> inbox;

  private record MessageAndResponse(
    IdAMessageType sent,
    IdAMessageType received)
  {

  }

  /**
   * The version 1 transport.
   *
   * @param inStrings    The string resources
   * @param inHttpClient The HTTP client
   * @param baseURI      The base URI
   */

  public IdATransport2(
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
      new IdACB2Messages();
    this.loginURI =
      baseURI.resolve("login")
        .normalize();
    this.commandURI =
      baseURI.resolve("command")
        .normalize();
  }

  @Override
  public Optional<IdAMessageType> read(
    final Duration timeout)
    throws IdAClientException, InterruptedException
  {
    Objects.requireNonNull(timeout, "timeout");

    if (this.isClosed()) {
      throw this.errorClosed();
    }

    return Optional.ofNullable(
      this.inbox.poll(timeout.toNanos(), TimeUnit.NANOSECONDS)
    ).map(x -> x.received);
  }

  private IdAClientException errorClosed()
  {
    return new IdAClientException(
      this.strings.format(IdStringConstants.ERROR_CLOSED_CHANNEL),
      new ClosedChannelException(),
      IdStandardErrorCodes.API_MISUSE_ERROR,
      Map.of(),
      Optional.empty(),
      Optional.empty()
    );
  }

  @Override
  public void write(
    final IdAMessageType message)
    throws IdAClientException, InterruptedException
  {
    if (this.isClosed()) {
      throw this.errorClosed();
    }

    try {
      switch (message) {
        case final IdACommandLogin m -> {
          this.send(message, this.loginURI);
        }
        default -> {
          this.send(message, this.commandURI);
        }
      }
    } catch (final IOException | IdProtocolException e) {
      throw IdAClientException.ofException(e);
    }
  }

  private void send(
    final IdAMessageType message,
    final URI targetURI)
    throws
    IdAClientException,
    IOException,
    IdProtocolException,
    InterruptedException
  {
    if (message instanceof final IdACommandType<?> command) {
      this.sendCommand(targetURI, command);
    } else {
      throw this.errorNotCommand(message);
    }
  }

  private void sendCommand(
    final URI targetURI,
    final IdACommandType<?> command)
    throws
    IOException,
    InterruptedException,
    IdAClientException,
    IdProtocolException
  {
    final var data =
      this.messages.serialize(command);

    final var response =
      this.http.send(
        HttpRequest.newBuilder()
          .uri(targetURI)
          .POST(HttpRequest.BodyPublishers.ofByteArray(data))
          .build(),
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

    final var expectedContentType = IdACB2Messages.contentType();
    if (!contentType.equals(expectedContentType)) {
      throw this.errorContentType(contentType, expectedContentType);
    }

    /*
     * Parse the response message, decompressing if necessary. If the
     * parsed message isn't a response... fail.
     */

    final var responseMessage =
      this.messages.parse(decompressResponse(response, responseHeaders));

    if (!(responseMessage instanceof IdAResponseType)) {
      throw this.errorUnexpectedResponseType(command, responseMessage);
    }

    if (!Objects.equals(command.responseClass(), responseMessage.getClass())) {
      throw this.errorUnexpectedResponseType(command, responseMessage);
    }

    this.inbox.add(new MessageAndResponse(command, responseMessage));
  }

  @Override
  public boolean isClosed()
  {
    return this.http.isTerminated();
  }

  @Override
  public void close()
    throws IdAClientException
  {
    this.http.close();
  }

  private IdAClientException errorNotCommand(
    final IdAMessageType message)
  {
    final var attributes = new HashMap<String, String>();
    attributes.put(
      this.strings.format(IdStringConstants.MESSAGE_TYPE),
      message.getClass().getSimpleName()
    );

    return new IdAClientException(
      this.strings.format(ERROR_EXPECTED_COMMAND_TYPE),
      PROTOCOL_ERROR,
      Map.copyOf(attributes),
      Optional.empty(),
      Optional.empty()
    );
  }

  private IdAClientException errorContentType(
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

    return new IdAClientException(
      this.strings.format(ERROR_UNEXPECTED_CONTENT_TYPE),
      PROTOCOL_ERROR,
      Map.copyOf(attributes),
      Optional.empty(),
      Optional.empty()
    );
  }

  private IdAClientException errorUnexpectedResponseType(
    final IdAMessageType message,
    final IdAMessageType responseActual)
  {
    final var attributes = new HashMap<String, String>();
    if (message instanceof final IdACommandType<?> cmd) {
      attributes.put(
        this.strings.format(EXPECTED_RESPONSE_TYPE),
        cmd.responseClass().getSimpleName()
      );
    }

    attributes.put(
      this.strings.format(RECEIVED_RESPONSE_TYPE),
      responseActual.getClass().getSimpleName()
    );

    return new IdAClientException(
      this.strings.format(ERROR_UNEXPECTED_RESPONSE_TYPE),
      PROTOCOL_ERROR,
      Map.copyOf(attributes),
      Optional.empty(),
      Optional.empty()
    );
  }
}

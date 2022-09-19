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


package com.io7m.idstore.server.internal.user_v1;

import com.io7m.idstore.error_codes.IdErrorCode;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.user_v1.IdU1MessageType;
import com.io7m.idstore.protocol.user_v1.IdU1Messages;
import com.io7m.idstore.protocol.user_v1.IdU1ResponseError;
import com.io7m.idstore.services.api.IdServiceType;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

/**
 * Convenient functions to send messages.
 */

public final class IdU1Sends implements IdServiceType
{
  private final IdU1Messages messages;

  /**
   * Convenient functions to send messages.
   *
   * @param inMessages A message codec
   */

  public IdU1Sends(
    final IdU1Messages inMessages)
  {
    this.messages = Objects.requireNonNull(inMessages, "messages");
  }

  /**
   * Send an error message.
   *
   * @param response   The servlet response
   * @param requestId  The request ID
   * @param statusCode The HTTP status code
   * @param errorCode  The error code
   * @param message    The message
   *
   * @throws IOException On errors
   */

  public void sendError(
    final HttpServletResponse response,
    final UUID requestId,
    final int statusCode,
    final IdErrorCode errorCode,
    final String message)
    throws IOException
  {
    this.send(
      response,
      statusCode,
      new IdU1ResponseError(requestId, errorCode.id(), message)
    );
  }

  /**
   * Send a message.
   *
   * @param response   The servlet response
   * @param statusCode The HTTP status code
   * @param message    The message
   *
   * @throws IOException On errors
   */

  public void send(
    final HttpServletResponse response,
    final int statusCode,
    final IdU1MessageType message)
    throws IOException
  {
    response.setStatus(statusCode);
    response.setContentType(IdU1Messages.CONTENT_TYPE);

    try {
      final var data = this.messages.serialize(message);
      response.setContentLength(data.length + 2);
      try (var output = response.getOutputStream()) {
        output.write(data);
        output.write('\r');
        output.write('\n');
      }
    } catch (final IdProtocolException e) {
      throw new IOException(e);
    }
  }

  @Override
  public String description()
  {
    return "User message sending service.";
  }

  @Override
  public String toString()
  {
    return "[IdACB1Sends 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode()));
  }
}

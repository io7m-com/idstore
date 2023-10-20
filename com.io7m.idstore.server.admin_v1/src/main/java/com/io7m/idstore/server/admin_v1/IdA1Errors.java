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


package com.io7m.idstore.server.admin_v1;

import com.io7m.idstore.error_codes.IdException;
import com.io7m.idstore.protocol.admin.IdAResponseBlame;
import com.io7m.idstore.protocol.admin.IdAResponseError;
import com.io7m.idstore.protocol.admin.cb.IdACB1Messages;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.http.IdHTTPRequestInformation;
import com.io7m.idstore.server.http.IdHTTPResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPResponseType;

import java.util.Set;

/**
 * Functions to transform exceptions.
 */

public final class IdA1Errors
{
  private IdA1Errors()
  {

  }

  /**
   * Transform an exception into an error response.
   *
   * @param information The request information
   * @param blame       The blame assignment
   * @param exception   The exception
   *
   * @return An error response
   */

  public static IdAResponseError errorOf(
    final IdHTTPRequestInformation information,
    final IdAResponseBlame blame,
    final IdException exception)
  {
    return new IdAResponseError(
      information.requestId(),
      exception.getMessage(),
      exception.errorCode(),
      exception.attributes(),
      exception.remediatingAction(),
      blame
    );
  }

  /**
   * Transform an exception into an error response.
   *
   * @param messages    A message serializer
   * @param information The request information
   * @param blame       The blame assignment
   * @param exception   The exception
   *
   * @return An error response
   */

  public static IdHTTPResponseType errorResponseOf(
    final IdACB1Messages messages,
    final IdHTTPRequestInformation information,
    final IdAResponseBlame blame,
    final IdException exception)
  {
    return new IdHTTPResponseFixedSize(
      switch (blame) {
        case BLAME_CLIENT -> 400;
        case BLAME_SERVER -> 500;
      },
      Set.of(),
      IdACB1Messages.contentType(),
      messages.serialize(errorOf(information, blame, exception))
    );
  }

  /**
   * Transform an exception into an error response.
   *
   * @param messages    A message serializer
   * @param information The request information
   * @param exception   The exception
   *
   * @return An error response
   */

  public static IdHTTPResponseType errorResponseOf(
    final IdACB1Messages messages,
    final IdHTTPRequestInformation information,
    final IdCommandExecutionFailure exception)
  {
    final IdAResponseBlame blame;
    if (exception.httpStatusCode() < 500) {
      blame = IdAResponseBlame.BLAME_CLIENT;
    } else {
      blame = IdAResponseBlame.BLAME_SERVER;
    }

    return new IdHTTPResponseFixedSize(
      exception.httpStatusCode(),
      Set.of(),
      IdACB1Messages.contentType(),
      messages.serialize(errorOf(information, blame, exception))
    );
  }
}

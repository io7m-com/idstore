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

package com.io7m.idstore.admin_client.api;

import com.io7m.hibiscus.api.HBClientType;
import com.io7m.hibiscus.api.HBConnectionError;
import com.io7m.hibiscus.api.HBConnectionFailed;
import com.io7m.hibiscus.api.HBConnectionParametersType;
import com.io7m.hibiscus.api.HBConnectionSucceeded;
import com.io7m.hibiscus.api.HBMessageType;
import com.io7m.idstore.protocol.admin.IdACommandType;
import com.io7m.idstore.protocol.admin.IdAMessageType;
import com.io7m.idstore.protocol.admin.IdAResponseError;
import com.io7m.idstore.protocol.admin.IdAResponseType;
import com.io7m.repetoir.core.RPServiceType;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * The type of synchronous clients.
 */

public interface IdAClientType
  extends HBClientType<IdAMessageType, IdAClientConnectionParameters, IdAClientException>,
  RPServiceType
{
  /**
   * Call {@link #connect(HBConnectionParametersType)} but throw an exception
   * if the result is an {@link IdAResponseError}.
   *
   * @param parameters The connection parameters
   *
   * @return The success message
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  default IdAMessageType connectOrThrow(
    final IdAClientConnectionParameters parameters)
    throws IdAClientException, InterruptedException
  {
    final var r =
      this.connect(parameters);

    return switch (r) {
      case final HBConnectionError<
        IdAMessageType, IdAClientConnectionParameters, ?, IdAClientException>
        error -> {
        throw IdAClientException.ofException(error.exception());
      }
      case final HBConnectionFailed<
        IdAMessageType, IdAClientConnectionParameters, ?, IdAClientException>
        failed -> {
        if (failed.message() instanceof final IdAResponseError error) {
          throw IdAClientException.ofError(error);
        }
        throw new IllegalStateException();
      }
      case final HBConnectionSucceeded<
        IdAMessageType, IdAClientConnectionParameters, ?, IdAClientException>
        succeeded -> {
        yield succeeded.message();
      }
    };
  }

  /**
   * Call {@link #sendAndWait(HBMessageType, Duration)} but throw an exception
   * if the result is an {@link IdAResponseError}.
   *
   * @param message The message
   * @param timeout The timeout
   * @param <R>     The type of results
   *
   * @return The result
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   * @throws TimeoutException     On timeouts
   */

  default <R extends IdAResponseType> R sendAndWaitOrThrow(
    final IdACommandType<R> message,
    final Duration timeout)
    throws IdAClientException, InterruptedException, TimeoutException
  {
    final var r =
      this.sendAndWait(message, timeout);

    return switch (r) {
      case final IdAResponseError error -> {
        throw IdAClientException.ofError(error);
      }
      default -> (R) r;
    };
  }

  @Override
  default String description()
  {
    return "idstore admin client service.";
  }
}

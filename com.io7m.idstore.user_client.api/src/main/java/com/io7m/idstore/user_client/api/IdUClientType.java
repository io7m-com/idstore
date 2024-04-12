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

package com.io7m.idstore.user_client.api;

import com.io7m.hibiscus.api.HBClientType;
import com.io7m.hibiscus.api.HBConnectionError;
import com.io7m.hibiscus.api.HBConnectionFailed;
import com.io7m.hibiscus.api.HBConnectionParametersType;
import com.io7m.hibiscus.api.HBConnectionSucceeded;
import com.io7m.hibiscus.api.HBMessageType;
import com.io7m.idstore.protocol.user.IdUCommandType;
import com.io7m.idstore.protocol.user.IdUMessageType;
import com.io7m.idstore.protocol.user.IdUResponseError;
import com.io7m.idstore.protocol.user.IdUResponseType;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * The type of clients.
 */

public interface IdUClientType
  extends HBClientType<IdUMessageType, IdUClientConnectionParameters, IdUClientException>
{
  /**
   * Call {@link #connect(HBConnectionParametersType)} but throw an exception
   * if the result is an {@link IdUResponseError}.
   *
   * @param parameters The connection parameters
   *
   * @return The success message
   *
   * @throws IdUClientException   On errors
   * @throws InterruptedException On interruption
   */

  default IdUMessageType connectOrThrow(
    final IdUClientConnectionParameters parameters)
    throws IdUClientException, InterruptedException
  {
    final var r =
      this.connect(parameters);

    return switch (r) {
      case final HBConnectionError<
        IdUMessageType, IdUClientConnectionParameters, ?, IdUClientException>
        error -> {
        throw IdUClientException.ofException(error.exception());
      }
      case final HBConnectionFailed<
        IdUMessageType, IdUClientConnectionParameters, ?, IdUClientException>
        failed -> {
        if (failed.message() instanceof final IdUResponseError error) {
          throw IdUClientException.ofError(error);
        }
        throw new IllegalStateException();
      }
      case final HBConnectionSucceeded<
        IdUMessageType, IdUClientConnectionParameters, ?, IdUClientException>
        succeeded -> {
        yield succeeded.message();
      }
    };
  }

  /**
   * Call {@link #sendAndWait(HBMessageType, Duration)} but throw an exception
   * if the result is an {@link IdUResponseError}.
   *
   * @param message The message
   * @param timeout The timeout
   * @param <R>     The type of results
   *
   * @return The result
   *
   * @throws IdUClientException   On errors
   * @throws InterruptedException On interruption
   * @throws TimeoutException     On timeouts
   */

  default <R extends IdUResponseType> R sendAndWaitOrThrow(
    final IdUCommandType<R> message,
    final Duration timeout)
    throws IdUClientException, InterruptedException, TimeoutException
  {
    final var r =
      this.sendAndWait(message, timeout);

    return switch (r) {
      case final IdUResponseError error -> {
        throw IdUClientException.ofError(error);
      }
      default -> (R) r;
    };
  }
}

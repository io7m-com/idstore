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

package com.io7m.idstore.server.service.telemetry.api;

import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdUserDomain;
import com.io7m.repetoir.core.RPServiceType;

import java.time.Duration;

/**
 * The interface exposed by the metrics service.
 */

public interface IdMetricsServiceType extends AutoCloseable, RPServiceType
{
  /**
   * An HTTP request was received.
   *
   * @param type The user domain
   */

  void onHttpRequested(IdUserDomain type);

  /**
   * An HTTP request resulted in a 5xx error.
   *
   * @param type The user domain
   */

  void onHttp5xx(IdUserDomain type);

  /**
   * An HTTP request resulted in a 2xx success.
   *
   * @param type The user domain
   */

  void onHttp2xx(IdUserDomain type);

  /**
   * An HTTP request resulted in a 4xx error.
   *
   * @param type The user domain
   */

  void onHttp4xx(IdUserDomain type);

  /**
   * An HTTP request was received of a given size.
   *
   * @param type The user domain
   * @param size The size
   */

  void onHttpRequestSize(
    IdUserDomain type,
    long size);

  /**
   * An HTTP response was produced of a given size.
   *
   * @param type The user domain
   * @param size The size
   */

  void onHttpResponseSize(
    IdUserDomain type,
    long size);

  /**
   * Mail was sent to the given address.
   *
   * @param address The address
   * @param time    The time it took
   */

  void onMailSent(
    IdEmail address,
    Duration time
  );

  /**
   * Mail could not be sent to the given address.
   *
   * @param address The address
   * @param time    The time it took
   */

  void onMailFailed(
    IdEmail address,
    Duration time
  );

  /**
   * A rate limit was triggered.
   *
   * @param name      The rate name
   * @param host      The host
   * @param user      The user
   * @param operation The operation
   */

  void onRateLimitTriggered(
    String name,
    String host,
    String user,
    String operation);

  /**
   * An HTTP response was produced in the given time.
   *
   * @param type The user domain
   * @param time The time
   */

  void onHttpResponseTime(
    IdUserDomain type,
    Duration time);

  /**
   * A login session was created.
   *
   * @param type     The type of session
   * @param countNow The number of sessions now active
   */

  void onLogin(
    IdUserDomain type,
    long countNow);

  /**
   * A login session was closed or expired.
   *
   * @param type     The type of session
   * @param countNow The number of sessions now active
   */

  void onLoginClosed(
    IdUserDomain type,
    long countNow);

  /**
   * The login pause time is known.
   *
   * @param type     The user domain
   * @param duration The pause time
   */

  void onLoginPauseTime(
    IdUserDomain type,
    Duration duration);
}

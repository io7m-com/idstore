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

package com.io7m.idstore.server.service.sessions;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.io7m.jaffirm.core.Preconditions;
import com.io7m.repetoir.core.RPServiceType;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;

/**
 * A service to create and manage sessions.
 *
 * @param <S> The type of sessions
 */

public abstract class IdSessionService<S extends IdSessionType>
  implements RPServiceType
{
  private final ObservableLongMeasurement sessionsGauge;
  private final Cache<IdSessionSecretIdentifier, S> sessions;
  private final ConcurrentMap<IdSessionSecretIdentifier, S> sessionsMap;
  private final BiFunction<UUID, IdSessionSecretIdentifier, S> sessionCreator;

  /**
   * A service to create and manage sessions.
   *
   * @param inTelemetry      The telemetry service
   * @param inExpiration     The session expiration time
   * @param type             The session type
   * @param inSessionCreator A session creator function
   */

  protected IdSessionService(
    final OpenTelemetry inTelemetry,
    final Duration inExpiration,
    final String type,
    final BiFunction<UUID, IdSessionSecretIdentifier, S> inSessionCreator)
  {
    this.sessionCreator =
      Objects.requireNonNull(inSessionCreator, "sessionCreator");

    this.sessions =
      Caffeine.newBuilder()
        .expireAfterAccess(inExpiration)
        .<IdSessionSecretIdentifier, S>evictionListener(
          (key, val, removalCause) -> this.onSessionRemoved(removalCause))
        .build();

    this.sessionsMap =
      this.sessions.asMap();

    final var meter =
      inTelemetry.meterBuilder(IdSessionService.class.getCanonicalName())
        .build();

    this.sessionsGauge =
      meter.gaugeBuilder("idstore.active%sSessions".formatted(type))
        .setDescription("Active %s sessions.".formatted(type))
        .ofLongs()
        .buildObserver();
  }

  protected abstract Logger logger();

  private void onSessionRemoved(
    final RemovalCause removalCause)
  {
    final var sizeNow = this.sessions.estimatedSize();
    this.logger().debug(
      "delete session ({}) ({} now active)",
      removalCause,
      Long.toUnsignedString(sizeNow)
    );
    this.sessionsGauge.record(sizeNow);
  }

  /**
   * Find a session with the given identifier.
   *
   * @param id The identifier
   *
   * @return The session
   */

  public final Optional<S> findSession(
    final IdSessionSecretIdentifier id)
  {
    return Optional.ofNullable(
      this.sessionsMap.get(Objects.requireNonNull(id, "id"))
    );
  }

  /**
   * Create a new session.
   *
   * @param userId The admin ID
   *
   * @return A new session
   */

  public final S createSession(
    final UUID userId)
  {
    Objects.requireNonNull(userId, "userId");

    final var id =
      IdSessionSecretIdentifier.generate();

    Preconditions.checkPreconditionV(
      !this.sessionsMap.containsKey(id),
      "Session ID cannot already have been used."
    );

    final var session = this.sessionCreator.apply(userId, id);
    this.sessions.put(id, session);
    this.sessionsGauge.record(this.sessions.estimatedSize());
    return session;
  }

  /**
   * Delete an existing session.
   *
   * @param id The session ID
   */

  public void deleteSession(
    final IdSessionSecretIdentifier id)
  {
    Objects.requireNonNull(id, "id");

    this.sessions.invalidate(id);
    this.sessionsGauge.record(this.sessions.estimatedSize());
  }
}

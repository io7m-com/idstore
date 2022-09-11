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

package com.io7m.idstore.server.internal;

import com.io7m.idstore.services.api.IdServiceType;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A service to create and manage user controllers.
 */

public final class IdUserSessionService
  implements IdServiceType, HttpSessionListener
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdUserSessionService.class);

  private final ConcurrentHashMap<String, IdUserSession> controllers;

  /**
   * A service to create and manage user controllers.
   */

  public IdUserSessionService()
  {
    this.controllers = new ConcurrentHashMap<>();
  }

  @Override
  public String description()
  {
    return "User controller service.";
  }

  /**
   * Create or get an existing user controller.
   *
   * @param userId    The user ID
   * @param sessionId The session ID
   *
   * @return A user controller
   */

  public IdUserSession createOrGet(
    final UUID userId,
    final String sessionId)
  {
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(sessionId, "sessionId");

    final var id = "%s:%s".formatted(userId, sessionId);
    return this.controllers.computeIfAbsent(
      id,
      ignored -> {
        LOG.debug(
          "[{}] create controller ({} now active)",
          id,
          Integer.valueOf(this.controllers.size() + 1)
        );
        return new IdUserSession(userId, sessionId);
      }
    );
  }

  @Override
  public void sessionCreated(
    final HttpSessionEvent se)
  {

  }

  @Override
  public void sessionDestroyed(
    final HttpSessionEvent se)
  {
    final var session = se.getSession();
    final var userId = (UUID) session.getAttribute("UserID");
    final var sessionId = session.getId();

    if (userId != null) {
      this.delete(userId, sessionId);
    }
  }

  /**
   * Delete a user controller if one exists.
   *
   * @param userId    The user ID
   * @param sessionId The session ID
   */

  public void delete(
    final UUID userId,
    final String sessionId)
  {
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(sessionId, "sessionId");

    final var id = "%s:%s".formatted(userId, sessionId);
    this.controllers.remove(id);
    LOG.debug(
      "[{}] delete controller ({} now active)",
      id,
      Integer.valueOf(this.controllers.size())
    );
  }
}

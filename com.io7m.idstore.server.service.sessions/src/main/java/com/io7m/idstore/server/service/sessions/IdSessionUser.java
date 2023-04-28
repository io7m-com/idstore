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

package com.io7m.idstore.server.service.sessions;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A controller for a single user session.
 */

public final class IdSessionUser implements IdSessionType
{
  private final UUID userId;
  private final IdSessionSecretIdentifier sessionId;
  private Optional<IdSessionMessage> message;

  /**
   * A controller for a single user session.
   *
   * @param inUserId    The user ID
   * @param inSessionId The session ID
   */

  public IdSessionUser(
    final UUID inUserId,
    final IdSessionSecretIdentifier inSessionId)
  {
    this.userId =
      Objects.requireNonNull(inUserId, "userId");
    this.sessionId =
      Objects.requireNonNull(inSessionId, "sessionId");
    this.message =
      Optional.empty();
  }

  /**
   * Set the current session message.
   *
   * @param inMessage The message
   */

  public void messageCurrentSet(
    final IdSessionMessage inMessage)
  {
    this.message =
      Optional.of(Objects.requireNonNull(inMessage, "message"));
  }

  /**
   * @return The current session message
   */

  public Optional<IdSessionMessage> messageCurrent()
  {
    return this.message;
  }

  /**
   * Discard the current message.
   */

  public void messageDiscard()
  {
    this.message = Optional.empty();
  }

  @Override
  public IdSessionSecretIdentifier id()
  {
    return this.sessionId;
  }

  /**
   * @return The user ID
   */

  public UUID userId()
  {
    return this.userId;
  }
}

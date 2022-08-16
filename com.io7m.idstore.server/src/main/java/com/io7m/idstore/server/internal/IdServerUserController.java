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

import com.io7m.idstore.database.api.IdDatabaseUserListPaging;
import com.io7m.idstore.database.api.IdDatabaseUserListPagingType;
import com.io7m.idstore.model.IdUserListParameters;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A controller for a single user session.
 */

public final class IdServerUserController
{
  private final UUID userId;
  private final String sessionId;
  private IdUserListParameters userListParameters;
  private IdDatabaseUserListPagingType userPaging;
  private Optional<IdSessionMessage> message;

  /**
   * A controller for a single user session.
   *
   * @param inUserId    The user ID
   * @param inSessionId The session ID
   */

  public IdServerUserController(
    final UUID inUserId,
    final String inSessionId)
  {
    this.userId =
      Objects.requireNonNull(inUserId, "userId");
    this.sessionId =
      Objects.requireNonNull(inSessionId, "sessionId");
    this.userListParameters =
      IdUserListParameters.defaults();
    this.userPaging =
      IdDatabaseUserListPaging.create(this.userListParameters);
    this.message =
      Optional.empty();
  }

  /**
   * @return The most recent user list parameters
   */

  public IdUserListParameters userListParameters()
  {
    return this.userListParameters;
  }

  /**
   * @return The most recent user paging handler
   */

  public IdDatabaseUserListPagingType userPaging()
  {
    return this.userPaging;
  }

  /**
   * Set the user listing parameters.
   *
   * @param userParameters The user parameters
   */

  public void setUserListParameters(
    final IdUserListParameters userParameters)
  {
    this.userListParameters =
      Objects.requireNonNull(userParameters, "userParameters");

    if (!Objects.equals(this.userPaging.pageParameters(), userParameters)) {
      this.userPaging =
        IdDatabaseUserListPaging.create(userParameters);
    }
  }

  /**
   * @return The current message
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

  /**
   * Set the current message.
   *
   * @param inMessage The message
   */

  public void messageCurrentSet(
    final IdSessionMessage inMessage)
  {
    this.message = Optional.of(
      Objects.requireNonNull(inMessage, "message")
    );
  }
}

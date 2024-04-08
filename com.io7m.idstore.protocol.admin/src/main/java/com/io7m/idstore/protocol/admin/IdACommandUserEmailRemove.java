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

package com.io7m.idstore.protocol.admin;

import com.io7m.idstore.model.IdEmail;

import java.util.Objects;
import java.util.UUID;

/**
 * Remove an email address from the given user.
 *
 * @param messageId The message ID
 * @param user      The user ID
 * @param email     The email
 */

public record IdACommandUserEmailRemove(
  UUID messageId,
  UUID user,
  IdEmail email)
  implements IdACommandType<IdAResponseUserUpdate>
{
  /**
   * Remove an email address from the given user.
   */

  public IdACommandUserEmailRemove
  {
    Objects.requireNonNull(messageId, "messageId");
    Objects.requireNonNull(user, "id");
    Objects.requireNonNull(email, "email");
  }

  @Override
  public Class<IdAResponseUserUpdate> responseClass()
  {
    return IdAResponseUserUpdate.class;
  }
}

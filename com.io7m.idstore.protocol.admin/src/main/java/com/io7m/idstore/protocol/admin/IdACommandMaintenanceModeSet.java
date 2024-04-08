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

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Move the server to/from maintenance mode. If a message is specified, the
 * server is moved into maintenance mode. If no message is specified, the
 * server is moved out of maintenance mode.
 *
 * @param messageId The message ID
 * @param message   The message
 */

public record IdACommandMaintenanceModeSet(
  UUID messageId,
  Optional<String> message)
  implements IdACommandType<IdAResponseMaintenanceModeSet>
{
  /**
   * Move the server to/from maintenance mode. If a message is specified, the
   * server is moved into maintenance mode. If no message is specified, the
   * server is moved out of maintenance mode.
   *
   * @param message The message
   */

  public IdACommandMaintenanceModeSet
  {
    Objects.requireNonNull(messageId, "messageId");
    Objects.requireNonNull(message, "message");
  }

  @Override
  public Class<IdAResponseMaintenanceModeSet> responseClass()
  {
    return IdAResponseMaintenanceModeSet.class;
  }
}

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

package com.io7m.idstore.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * An audit event.
 *
 * @param id           The unique event ID
 * @param owner        The event owner
 * @param time         The event time
 * @param message      The event message
 * @param type         The event type
 * @param confidential {@code true} if the message contains confidential
 *                     information
 */

public record IdAuditEvent(
  long id,
  UUID owner,
  OffsetDateTime time,
  String type,
  String message,
  boolean confidential)
{
  /**
   * An audit event.
   *
   * @param id           The unique event ID
   * @param owner        The event owner
   * @param time         The event time
   * @param message      The event message
   * @param type         The event type
   * @param confidential {@code true} if the message contains confidential
   *                     information
   */

  public IdAuditEvent
  {
    Objects.requireNonNull(owner, "owner");
    Objects.requireNonNull(time, "time");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(message, "message");
  }

  /**
   * An audit event without confidential information.
   *
   * @param id      The unique event ID
   * @param owner   The event owner
   * @param time    The event time
   * @param message The event message
   * @param type    The event type
   */

  public IdAuditEvent(
    final long id,
    final UUID owner,
    final OffsetDateTime time,
    final String type,
    final String message)
  {
    this(id, owner, time, type, message, false);
  }
}

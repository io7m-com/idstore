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

package com.io7m.idstore.protocol.admin_v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.io7m.idstore.model.IdAuditEvent;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.api.IdProtocolToModelType;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * An audit event.
 *
 * @param id      The unique event ID
 * @param owner   The event owner
 * @param time    The event time
 * @param message The event message
 * @param type    The event type
 */

public record IdA1AuditEvent(
  @JsonProperty(value = "ID", required = true)
  long id,
  @JsonProperty(value = "Owner", required = true)
  UUID owner,
  @JsonProperty(value = "Time", required = true)
  OffsetDateTime time,
  @JsonProperty(value = "Type", required = true)
  String type,
  @JsonProperty(value = "Message", required = true)
  String message)
  implements IdProtocolToModelType<IdAuditEvent>
{
  /**
   * An audit event.
   *
   * @param id      The unique event ID
   * @param owner   The event owner
   * @param time    The event time
   * @param message The event message
   * @param type    The event type
   */

  public IdA1AuditEvent
  {
    Objects.requireNonNull(owner, "owner");
    Objects.requireNonNull(time, "time");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(message, "message");
  }

  @Override
  public IdAuditEvent toModel()
    throws IdProtocolException
  {
    return new IdAuditEvent(
      this.id,
      this.owner,
      this.time,
      this.type,
      this.message
    );
  }

  /**
   * @param event The event
   *
   * @return The given event as an A1 event
   */

  public static IdA1AuditEvent of(
    final IdAuditEvent event)
  {
    return new IdA1AuditEvent(
      event.id(),
      event.owner(),
      event.time(),
      event.type(),
      event.message()
    );
  }
}

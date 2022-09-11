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

import java.util.Objects;
import java.util.Optional;

/**
 * The immutable parameters required to list audit events.
 *
 * @param timeRange Only events created within this time range are returned
 * @param limit     The limit on the number of returned events
 * @param owner     Only include events with this owner
 * @param message   Only include events with this message
 * @param type      Only include events with this type
 */

public record IdAuditListParameters(
  IdTimeRange timeRange,
  Optional<String> owner,
  Optional<String> type,
  Optional<String> message,
  int limit)
{
  /**
   * The immutable parameters required to list events.
   *
   * @param timeRange Only events created within this time range are returned
   * @param limit     The limit on the number of returned events
   * @param owner     Only include events with this owner
   * @param message   Only include events with this message
   * @param type      Only include events with this type
   */

  public IdAuditListParameters
  {
    Objects.requireNonNull(timeRange, "timeRange");
    Objects.requireNonNull(owner, "owner");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(message, "message");
  }

  /**
   * @return Sensible default values
   */

  public static IdAuditListParameters defaults()
  {
    return new IdAuditListParameters(
      IdTimeRange.largest(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      100
    );
  }

  /**
   * @return The limit on the number of returned events
   */

  @Override
  public int limit()
  {
    return Math.max(1, this.limit);
  }

}

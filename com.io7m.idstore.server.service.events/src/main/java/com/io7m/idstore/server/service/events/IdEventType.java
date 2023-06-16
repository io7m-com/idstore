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

package com.io7m.idstore.server.service.events;

import java.util.Map;

/**
 * The base type of events.
 */

public sealed interface IdEventType
  permits IdEventAdminLoginRateLimitExceeded,
  IdEventAdminType,
  IdEventMailFailed,
  IdEventMailSent,
  IdEventUserLoginRateLimitExceeded,
  IdEventUserPasswordResetRateLimitExceeded,
  IdEventUserType
{
  /**
   * @return The event domain
   */

  default String domain()
  {
    return "server";
  }

  /**
   * @return The event severity
   */

  IdEventSeverity severity();

  /**
   * @return The event name
   */

  String name();

  /**
   * @return The formatted event message
   */

  String message();

  /**
   * @return The complete event attributes
   */

  Map<String, String> asAttributes();
}

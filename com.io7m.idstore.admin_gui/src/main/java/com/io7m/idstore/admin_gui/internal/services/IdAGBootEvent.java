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

package com.io7m.idstore.admin_gui.internal.services;

import com.io7m.idstore.admin_gui.internal.events.IdAGEventStatusInProgress;
import com.io7m.idstore.admin_gui.internal.events.IdAGEventStatusType;
import com.io7m.idstore.admin_gui.internal.events.IdAGEventType;

import java.util.Objects;
import java.util.OptionalDouble;

/**
 * A boot event.
 *
 * @param message  The boot message
 * @param progress The boot progress in the range {@code [0, 1]}
 */

public record IdAGBootEvent(
  String message,
  double progress)
  implements IdAGEventType
{
  /**
   * A boot event.
   *
   * @param message  The boot message
   * @param progress The boot progress in the range {@code [0, 1]}
   */

  public IdAGBootEvent
  {
    Objects.requireNonNull(message, "message");
  }

  @Override
  public IdAGEventStatusType status()
  {
    return new IdAGEventStatusInProgress(OptionalDouble.of(this.progress));
  }
}

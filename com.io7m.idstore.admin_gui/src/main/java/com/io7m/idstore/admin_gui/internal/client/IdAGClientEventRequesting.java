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


package com.io7m.idstore.admin_gui.internal.client;

import com.io7m.idstore.admin_gui.internal.events.IdAGEventStatusInProgress;
import com.io7m.idstore.admin_gui.internal.events.IdAGEventStatusType;

import java.util.Objects;
import java.util.OptionalDouble;

import static com.io7m.idstore.admin_gui.internal.client.IdAGClientStatus.REQUESTING;

/**
 * The client is sending a request to the server.
 *
 * @param message The message
 */

public record IdAGClientEventRequesting(
  String message)
  implements IdAGClientEventType
{
  /**
   * The client is sending a request to the server.
   */

  public IdAGClientEventRequesting
  {
    Objects.requireNonNull(message, "message");
  }

  @Override
  public IdAGEventStatusType status()
  {
    return new IdAGEventStatusInProgress(OptionalDouble.empty());
  }

  @Override
  public IdAGClientStatus clientStatus()
  {
    return REQUESTING;
  }
}

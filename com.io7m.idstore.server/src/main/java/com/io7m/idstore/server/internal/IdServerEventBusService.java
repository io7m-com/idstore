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

import com.io7m.idstore.server.api.events.IdServerEventType;
import com.io7m.idstore.services.api.IdServiceType;

import java.util.Objects;
import java.util.concurrent.SubmissionPublisher;

/**
 * A service exposing the event bus.
 */

public final class IdServerEventBusService
  implements IdServiceType, AutoCloseable
{
  private final SubmissionPublisher<IdServerEventType> subject;

  /**
   * A service exposing the event bus.
   *
   * @param inSubject An event subject
   */

  public IdServerEventBusService(
    final SubmissionPublisher<IdServerEventType> inSubject)
  {
    this.subject =
      Objects.requireNonNull(inSubject, "subject");
  }

  @Override
  public String description()
  {
    return "Event bus service.";
  }

  /**
   * Publish an event.
   *
   * @param event The event
   */

  public void publish(
    final IdServerEventType event)
  {
    this.subject.submit(
      Objects.requireNonNull(event, "event")
    );
  }

  @Override
  public void close()
  {
    this.subject.close();
  }

  @Override
  public String toString()
  {
    return "[IdServerEventBusService 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode(), 16));
  }
}

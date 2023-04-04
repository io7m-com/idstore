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

package com.io7m.idstore.admin_gui.internal;

import com.io7m.repetoir.core.RPServiceType;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * A scheduling service for running background UI tasks.
 */

public final class IdAGBackgroundSchedulerService
  implements RPServiceType, AutoCloseable
{
  private final ScheduledExecutorService executor;

  /**
   * A scheduling service for running background UI tasks.
   */

  public IdAGBackgroundSchedulerService()
  {
    this.executor = Executors.newScheduledThreadPool(1, runnable -> {
      final var thread = new Thread(runnable);
      thread.setDaemon(true);
      thread.setName(
        String.format(
          "com.io7m.idstore.admin_gui[%d]",
          Long.valueOf(thread.getId()))
      );
      return thread;
    });
  }

  /**
   * @return The executor
   */

  public ScheduledExecutorService executor()
  {
    return this.executor;
  }

  @Override
  public void close()
  {
    this.executor.shutdown();
  }

  @Override
  public String toString()
  {
    return String.format(
      "[IdAGBackgroundSchedulerService 0x%08x]",
      Integer.valueOf(this.hashCode())
    );
  }

  @Override
  public String description()
  {
    return "UI background scheduler";
  }
}

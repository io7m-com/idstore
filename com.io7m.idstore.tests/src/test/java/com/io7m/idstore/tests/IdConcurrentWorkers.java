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


package com.io7m.idstore.tests;

import com.io7m.idstore.user_client.IdUClients;

import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class IdConcurrentWorkers
{
  private IdConcurrentWorkers()
  {

  }

  public static void main(
    final String[] args)
    throws Exception
  {
    final var clients =
      new IdUClients();
    final var executor =
      Executors.newScheduledThreadPool(100);

    executor.scheduleAtFixedRate(() -> {
      if (Math.random() > 0.8) {
        executor.execute(() -> {
          try (var client = clients.create(Locale.ROOT)) {
            client.login(
              "someone",
              "abc",
              URI.create("http://localhost:50000/"),
              Map.of()
            );

            for (int index = 0; index < 10; ++index) {
              Thread.sleep((long) (Math.random() * 10_000L));
              client.userSelf();
            }
          } catch (final Exception e) {
            throw new RuntimeException(e);
          }
        });
      }
    }, 0L, 1L, TimeUnit.SECONDS);
  }
}

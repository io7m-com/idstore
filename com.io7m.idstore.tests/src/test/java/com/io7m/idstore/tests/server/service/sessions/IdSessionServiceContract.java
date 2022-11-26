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


package com.io7m.idstore.tests.server.service.sessions;

import com.io7m.idstore.server.service.sessions.IdSessionService;
import com.io7m.idstore.server.service.sessions.IdSessionType;
import com.io7m.idstore.tests.server.service.IdServiceContract;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public abstract class IdSessionServiceContract<T extends IdSessionType, S extends IdSessionService<T>>
  extends IdServiceContract<S>
{
  protected abstract S createWithExpiration(
    Duration expiration);

  /**
   * Sessions have unique identifiers.
   */

  @Test
  public void testSessionCreateUnique()
  {
    final var sessions =
      this.createInstanceA();
    final var id =
      UUID.randomUUID();
    final var session0 =
      sessions.createSession(id);
    final var session1 =
      sessions.createSession(id);

    assertNotEquals(session0.id(), session1.id());
  }

  /**
   * Sessions expire.
   *
   * @throws Exception On errors
   */

  @Test
  public void testSessionExpires()
    throws Exception
  {
    final var sessions =
      this.createWithExpiration(Duration.ofMillis(5L));
    final var id =
      UUID.randomUUID();
    final var session0 =
      sessions.createSession(id);
    final var session1 =
      sessions.findSession(session0.id())
        .orElseThrow();

    Thread.sleep(10L);

    assertEquals(
      Optional.empty(),
      sessions.findSession(session0.id())
    );
  }

  /**
   * Sessions can be deleted.
   *
   * @throws Exception On errors
   */

  @Test
  public void testSessionDeleted()
    throws Exception
  {
    final var sessions =
      this.createInstanceA();
    final var id =
      UUID.randomUUID();
    final var session0 =
      sessions.createSession(id);

    sessions.deleteSession(session0.id());

    assertEquals(
      Optional.empty(),
      sessions.findSession(session0.id())
    );
  }
}

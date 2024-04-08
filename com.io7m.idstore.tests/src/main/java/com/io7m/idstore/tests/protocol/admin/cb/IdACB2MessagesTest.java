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

package com.io7m.idstore.tests.protocol.admin.cb;

import com.io7m.idstore.protocol.admin.IdAMessageType;
import com.io7m.idstore.protocol.admin.cb.IdACB2Messages;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class IdACB2MessagesTest
{
  private static final IdACB2Messages MESSAGES =
    new IdACB2Messages();

  @Property(tries = 2000)
  public void testSerialization(
    final @ForAll IdAMessageType message)
    throws Exception
  {
    final var data =
      MESSAGES.serialize(message);
    final var m =
      MESSAGES.parse(data);

    assertEquals(message, m);
  }

  @Test
  public void testProtocolId()
  {
    assertEquals(
      UUID.fromString("de1ef9f2-5ea7-388a-9b79-788c132abfd1"),
      IdACB2Messages.protocolId()
    );
  }
}

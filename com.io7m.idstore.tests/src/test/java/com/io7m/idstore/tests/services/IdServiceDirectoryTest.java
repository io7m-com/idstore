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

package com.io7m.idstore.tests.services;

import com.io7m.idstore.services.api.IdServiceDirectory;
import com.io7m.idstore.services.api.IdServiceException;
import com.io7m.idstore.services.api.IdServiceType;
import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class IdServiceDirectoryTest
{
  private static final class FakeService
    implements IdServiceType
  {
    @Override
    public String description()
    {
      return "Fake service";
    }
  }

  private static final class CrashClosedService
    implements Closeable, IdServiceType
  {
    @Override
    public String description()
    {
      return "Crash closed service";
    }

    @Override
    public void close()
      throws IOException
    {
      throw new IOException("Cannot close!");
    }
  }

  /**
   * Retrieving a registered service works.
   *
   * @throws IOException On errors
   */

  @Test
  public void testRegisterGet()
    throws IOException
  {
    final var s = new IdServiceDirectory();
    assertThrows(IdServiceException.class, () -> {
      s.requireService(FakeService.class);
    });

    final var f = new FakeService();
    s.register(FakeService.class, f);
    assertEquals(f, s.requireService(FakeService.class));
    assertEquals(f, s.optionalService(FakeService.class).orElseThrow());

    assertEquals(List.of(f), s.services());

    s.close();
  }

  /**
   * Retrieving registered services works.
   *
   * @throws IOException On errors
   */

  @Test
  public void testRegisterGetMultiple()
    throws IOException
  {
    final var s = new IdServiceDirectory();
    assertThrows(IdServiceException.class, () -> {
      s.requireService(FakeService.class);
    });

    final var f0 = new FakeService();
    final var f1 = new FakeService();
    final var f2 = new FakeService();

    s.register(FakeService.class, f0);
    s.register(FakeService.class, f1);
    s.register(FakeService.class, f2);

    assertEquals(
      List.of(f0, f1, f2),
      s.optionalServices(FakeService.class)
    );

    s.close();
  }

  /**
   * A service that crashes on closing results in an exception.
   *
   * @throws IOException On errors
   */

  @Test
  public void testCrashClosed()
    throws IOException
  {
    final var s = new IdServiceDirectory();
    final var f = new CrashClosedService();
    s.register(CrashClosedService.class, f);

    final var ex = assertThrows(IOException.class, s::close);
    assertTrue(ex.getMessage().contains("Cannot close!"));
  }
}

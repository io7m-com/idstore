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

package com.io7m.idstore.tests.server.service.reqlimit;

import com.io7m.idstore.server.service.reqlimit.IdRequestLimitExceeded;
import com.io7m.idstore.server.service.reqlimit.IdRequestLimits;
import com.io7m.idstore.tests.server.service.IdServiceContract;
import io.helidon.http.HeaderNames;
import io.helidon.http.HeaderValues;
import io.helidon.http.ServerRequestHeaders;
import io.helidon.http.media.ReadableEntity;
import io.helidon.webserver.http.ServerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class IdRequestLimitsTest
  extends IdServiceContract<IdRequestLimits>
{
  private IdRequestLimits limits;

  @Override
  protected IdRequestLimits createInstanceA()
  {
    return new IdRequestLimits(
      "size %d is too large"::formatted
    );
  }

  @Override
  protected IdRequestLimits createInstanceB()
  {
    return new IdRequestLimits(
      "size %d is not small enough"::formatted
    );
  }

  @BeforeEach
  public void setup()
  {
    this.limits = new IdRequestLimits("size %d is too large"::formatted);
  }

  /**
   * It's only possible to read the limited data, even if the request specifies
   * fewer bytes than are actually provided.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLimitNotExceeded()
    throws Exception
  {
    final var request =
      Mockito.mock(ServerRequest.class);
    final var headers =
      Mockito.mock(ServerRequestHeaders.class);
    final var content =
      Mockito.mock(ReadableEntity.class);

    Mockito.when(request.headers())
      .thenReturn(headers);
    Mockito.when(headers.get(HeaderNames.CONTENT_LENGTH))
      .thenReturn(HeaderValues.create(HeaderNames.CONTENT_LENGTH, "20"));

    final var data = new byte[200];
    SecureRandom.getInstanceStrong().nextBytes(data);
    final var slice = new byte[20];
    System.arraycopy(data, 0, slice, 0, 20);

    final var realStream =
      new ByteArrayInputStream(data);

    Mockito.when(request.content())
      .thenReturn(content);
    Mockito.when(content.inputStream())
      .thenReturn(realStream);

    final var input =
      this.limits.boundedMaximumInput(request, 100L);

    assertArrayEquals(
      slice,
      input.readAllBytes()
    );
  }

  /**
   * Exceeding the size limit is not allowed.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLimitExceeded()
    throws Exception
  {
    final var request =
      Mockito.mock(ServerRequest.class);
    final var headers =
      Mockito.mock(ServerRequestHeaders.class);

    Mockito.when(request.headers())
      .thenReturn(headers);
    Mockito.when(headers.get(HeaderNames.CONTENT_LENGTH))
      .thenReturn(HeaderValues.create(HeaderNames.CONTENT_LENGTH, "101"));

    final var ex =
      assertThrows(IdRequestLimitExceeded.class, () -> {
        this.limits.boundedMaximumInput(request, 100L);
      });

    assertEquals(101L, ex.sizeProvided());
    assertEquals(100L, ex.sizeLimit());
    assertEquals("size 101 is too large", ex.getMessage());
  }

  /**
   * It's possible to read the full data if no limit is provided.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLimitUnlimited()
    throws Exception
  {
    final var request =
      Mockito.mock(ServerRequest.class);
    final var headers =
      Mockito.mock(ServerRequestHeaders.class);
    final var content =
      Mockito.mock(ReadableEntity.class);

    Mockito.when(request.headers())
      .thenReturn(headers);
    Mockito.when(headers.get(HeaderNames.CONTENT_LENGTH))
      .thenReturn(HeaderValues.create(HeaderNames.CONTENT_LENGTH, "-1"));

    final var data = new byte[200];
    SecureRandom.getInstanceStrong().nextBytes(data);

    final var realStream =
      new ByteArrayInputStream(data);

    Mockito.when(request.content())
      .thenReturn(content);
    Mockito.when(content.inputStream())
      .thenReturn(realStream);

    final var input =
      this.limits.boundedMaximumInput(request, 200L);

    assertArrayEquals(
      data,
      input.readAllBytes()
    );
  }
}

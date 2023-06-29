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


package com.io7m.idstore.tests.model;

import com.io7m.idstore.model.IdOptional;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests over partial optional functions.
 */

public final class IdOptionalTest
{
  /**
   * mapPartial == map
   *
   * @param x The optional
   */

  @Property
  public void testMapPartialEq(
    final @ForAll Optional<Integer> x)
  {
    final Function<Integer, Integer> f0 =
      z -> z;
    final IdOptional.IdPartialFunctionType<Integer, Integer, RuntimeException> f1 =
      z -> z;

    assertEquals(x.map(f0), IdOptional.mapPartial(x, f1));
  }

  /**
   * flatMapPartial == flatMap
   *
   * @param x The optional
   */

  @Property
  public void testFlatMapPartialEq(
    final @ForAll Optional<Integer> x)
  {
    final Function<Integer, Optional<Integer>> f0 =
      z -> Optional.of(z);
    final IdOptional.IdPartialFunctionType<Integer, Optional<Integer>, RuntimeException> f1 =
      z -> Optional.of(z);

    assertEquals(x.flatMap(f0), IdOptional.flatMapPartial(x, f1));
  }
}

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

import com.io7m.idstore.model.IdNonEmptyList;
import com.io7m.idstore.model.IdValidityException;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.Size;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Non-empty list tests.
 */

public final class IdNonEmptyListTest
{
  /**
   * Conversion to/from lists is correct.
   *
   * @param values The values
   */

  @Property
  public void testIdentity(
    final @ForAll @Size(min = 1) List<Integer> values)
  {
    final var ne =
      IdNonEmptyList.ofList(values);

    assertEquals(values, ne.toList());
    assertEquals(values.size(), ne.size());
  }

  /**
   * Empty lists cannot be empty.
   */

  @Test
  public void testNonEmpty()
  {
    assertThrows(IdValidityException.class, () -> {
      IdNonEmptyList.ofList(List.of());
    });
  }
}

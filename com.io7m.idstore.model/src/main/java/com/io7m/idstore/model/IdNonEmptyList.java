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

package com.io7m.idstore.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * A non-empty list.
 *
 * @param first The first element
 * @param rest  The rest of the elements
 * @param <T>   The type of elements
 */

public record IdNonEmptyList<T>(
  T first,
  List<T> rest)
  implements Iterable<T>
{
  /**
   * A non-empty list.
   *
   * @param first The first element
   * @param rest  The rest of the elements
   */

  public IdNonEmptyList(
    final T first,
    final List<T> rest)
  {
    this.first =
      Objects.requireNonNull(first, "first");
    this.rest =
      List.copyOf(Objects.requireNonNull(rest, "rest"));
  }

  /**
   * @return This list as a plain immutable list
   */

  public List<T> toList()
  {
    final var items = new ArrayList<T>();
    items.add(this.first);
    items.addAll(this.rest);
    return List.copyOf(items);
  }

  /**
   * The list as a non-empty list.
   *
   * @param items The list elements
   * @param <T>   The type of elements
   *
   * @return The list
   *
   * @throws IdValidityException On empty lists
   */

  public static <T> IdNonEmptyList<T> ofList(
    final List<T> items)
    throws IdValidityException
  {
    Objects.requireNonNull(items, "items");
    return switch (items.size()) {
      case 0 -> {
        throw new IdValidityException("List must be non-empty.");
      }
      case 1 -> {
        yield new IdNonEmptyList<>(
          items.get(0),
          List.of()
        );
      }
      default -> {
        yield new IdNonEmptyList<>(
          items.get(0),
          items.subList(1, items.size())
        );
      }
    };
  }

  /**
   * @param email The email address
   *
   * @return {@code true} if the list contains the given email
   */

  public boolean contains(final T email)
  {
    Objects.requireNonNull(email, "email");
    return Objects.equals(this.first, email) || this.rest.contains(email);
  }

  @Override
  public Iterator<T> iterator()
  {
    return this.toList().iterator();
  }

  /**
   * @return The list size
   */

  public int size()
  {
    return 1 + this.rest.size();
  }
}

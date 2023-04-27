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


package com.io7m.idstore.model;

import java.util.Optional;
import java.util.function.Function;

/**
 * Extra functions for {@link java.util.Optional}.
 */

public final class IdOptional
{
  private IdOptional()
  {

  }

  /**
   * The type of partial functions.
   *
   * @param <A> The type of source values
   * @param <B> The type of return values
   * @param <E> The type of exceptions raised
   */

  public interface IdPartialFunctionType<A, B, E extends Exception>
  {
    /**
     * Apply the function.
     *
     * @param x The input
     *
     * @return The output
     *
     * @throws E On errors
     */

    B apply(A x)
      throws E;
  }

  /**
   * See {@link Optional#map(Function)}.
   *
   * @param o   The optional
   * @param f   The function
   * @param <A> The type of source values
   * @param <B> The type of return values
   * @param <E> The type of exceptions raised
   *
   * @return The value of {@code f}
   *
   * @throws E On errors
   */

  public static <A, B, E extends Exception> Optional<B> mapPartial(
    final Optional<A> o,
    final IdPartialFunctionType<A, B, E> f)
    throws E
  {
    if (o.isPresent()) {
      return Optional.of(f.apply(o.get()));
    }
    return Optional.empty();
  }

  /**
   * See {@link Optional#flatMap(Function)}.
   *
   * @param o   The optional
   * @param f   The function
   * @param <A> The type of source values
   * @param <B> The type of return values
   * @param <E> The type of exceptions raised
   *
   * @return The value of {@code f}
   *
   * @throws E On errors
   */

  public static <A, B, E extends Exception> Optional<B> flatMapPartial(
    final Optional<A> o,
    final IdPartialFunctionType<A, Optional<B>, E> f)
    throws E
  {
    if (o.isPresent()) {
      return f.apply(o.get());
    }
    return Optional.empty();
  }
}

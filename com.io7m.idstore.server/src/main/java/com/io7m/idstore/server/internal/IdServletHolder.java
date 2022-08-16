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

package com.io7m.idstore.server.internal;

import jakarta.servlet.Servlet;
import org.eclipse.jetty.servlet.ServletHolder;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A servlet holder used to inject dependencies into servlets.
 *
 * @param <T> The type of servlet
 */

public final class IdServletHolder<T extends Servlet>
  extends ServletHolder
{
  private final Class<T> clazz;
  private final Supplier<T> constructor;

  /**
   * Construct a holder.
   *
   * @param inClazz       The servlet class
   * @param inConstructor A constructor function to produce servlet instances
   */

  public IdServletHolder(
    final Class<T> inClazz,
    final Supplier<T> inConstructor)
  {
    this.clazz =
      Objects.requireNonNull(inClazz, "clazz");
    this.constructor =
      Objects.requireNonNull(inConstructor, "constructor");

    this.setHeldClass(this.clazz);
  }

  @Override
  protected Servlet newInstance()
  {
    return this.constructor.get();
  }
}

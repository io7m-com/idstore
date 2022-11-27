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

package com.io7m.idstore.server.http;

import com.io7m.idstore.services.api.IdServiceDirectoryType;
import jakarta.servlet.Servlet;

import java.util.Objects;
import java.util.function.Function;

/**
 * Servlet holder functions used to inject dependencies into servlets.
 */

public final class IdServletHolders
{
  private final IdServiceDirectoryType services;

  /**
   * Servlet holder functions used to inject dependencies into servlets.
   *
   * @param inServices The service directory
   */

  public IdServletHolders(
    final IdServiceDirectoryType inServices)
  {
    this.services =
      Objects.requireNonNull(inServices, "services");
  }

  /**
   * Create a new servlet holder.
   *
   * @param clazz    The servlet class
   * @param servlets A function that instantiates a servlet, given a service
   *                 directory
   * @param <T>      The type of servlet
   *
   * @return A new servlet holder
   */

  public <T extends Servlet> IdServletHolder<T> create(
    final Class<T> clazz,
    final Function<IdServiceDirectoryType, T> servlets)
  {
    Objects.requireNonNull(clazz, "clazz");
    Objects.requireNonNull(servlets, "servlets");
    return new IdServletHolder<>(clazz, () -> servlets.apply(this.services));
  }
}

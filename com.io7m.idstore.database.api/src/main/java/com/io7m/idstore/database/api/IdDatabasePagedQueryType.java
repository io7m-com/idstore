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


package com.io7m.idstore.database.api;

import com.io7m.idstore.model.IdPage;

/**
 * The type of paged queries.
 *
 * @param <Q> The type of required query interfaces
 * @param <T> The type of result values
 */

public interface IdDatabasePagedQueryType<Q extends IdDatabaseQueriesType, T>
{
  /**
   * Get data for the current page.
   *
   * @param queries The query interface
   *
   * @return A page of results
   *
   * @throws IdDatabaseException On errors
   */

  IdPage<T> pageCurrent(Q queries)
    throws IdDatabaseException;

  /**
   * Get data for the next page. If the current page is the last page, the
   * function acts as {@link #pageCurrent(IdDatabaseQueriesType)}.
   *
   * @param queries The query interface
   *
   * @return A page of results
   *
   * @throws IdDatabaseException On errors
   */

  IdPage<T> pageNext(Q queries)
    throws IdDatabaseException;

  /**
   * Get data for the previous page. If the current page is the first page, the
   * function acts as {@link #pageCurrent(IdDatabaseQueriesType)}.
   *
   * @param queries The query interface
   *
   * @return A page of results
   *
   * @throws IdDatabaseException On errors
   */

  IdPage<T> pagePrevious(Q queries)
    throws IdDatabaseException;
}

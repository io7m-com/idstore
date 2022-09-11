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

package com.io7m.idstore.database.api;

import java.util.List;

/**
 * The base type of paging handlers for returning large result sets. A handler
 * accepts parameters of type {@code P} that can conceptually be thought of as
 * the raw query parameters that would return <i>all</i> of the results if no
 * limiting was specified. The paging handler implementation maintains
 * "pointers" to pages of results and allows for fast seeking between pages.
 *
 * @param <P> The type of immutable parameters used for the query
 * @param <T> The type of returned values
 * @param <Q> The database query interface used
 *
 * @see "https://blog.jooq.org/faster-sql-paging-with-jooq-using-the-seek-method/"
 */

public interface IdDatabasePagingType<P, T, Q extends IdDatabaseQueriesType>
{
  /**
   * @return The immutable page parameters used for the queries
   */

  P pageParameters();

  /**
   * @return The current page number
   */

  int pageNumber();

  /**
   * @return The index of the first item in the current page
   */

  long pageFirstOffset();

  /**
   * @return An approximate count of the number of available pages
   */

  int pageCount();

  /**
   * @return {@code true} If the next page is currently available
   */

  boolean pageNextAvailable();

  /**
   * @return {@code true} If the previous page is currently available
   */

  default boolean pagePreviousAvailable()
  {
    return this.pageNumber() > 0;
  }

  /**
   * Seek to the previous page and return values within that page.
   *
   * @param queries The queries
   *
   * @return The page's results
   *
   * @throws IdDatabaseException On database errors
   */

  List<T> pagePrevious(Q queries)
    throws IdDatabaseException;

  /**
   * Seek to the next page and return values within that page.
   *
   * @param queries The queries
   *
   * @return The page's results
   *
   * @throws IdDatabaseException On database errors
   */

  List<T> pageNext(Q queries)
    throws IdDatabaseException;

  /**
   * Obtain the current page's values.
   *
   * @param queries The queries
   *
   * @return The page's results
   *
   * @throws IdDatabaseException On database errors
   */

  List<T> pageCurrent(Q queries)
    throws IdDatabaseException;
}

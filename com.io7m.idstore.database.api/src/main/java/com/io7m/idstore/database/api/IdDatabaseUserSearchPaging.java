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

import com.io7m.idstore.model.IdUserOrdering;
import com.io7m.idstore.model.IdUserSearchParameters;
import com.io7m.idstore.model.IdUserSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A paging handler for user summaries.
 */

public final class IdDatabaseUserSearchPaging
  implements IdDatabaseUserSearchPagingType
{
  private final IdUserSearchParameters parameters;
  private volatile int currentPage;
  private final ConcurrentHashMap<Integer, Page> pages;
  private volatile int pagesCountApproximate;

  private IdDatabaseUserSearchPaging(
    final IdUserSearchParameters inParameters)
  {
    this.parameters =
      Objects.requireNonNull(inParameters, "parameters");

    this.currentPage = 0;
    this.pages = new ConcurrentHashMap<Integer, Page>();
    this.pages.put(Integer.valueOf(0), new Page());
  }

  private static final class Page
  {
    private volatile Optional<List<Object>> seek = Optional.empty();

    Page()
    {

    }
  }

  /**
   * A paging handler for user summaries.
   *
   * @param inParameters The user list parameters
   *
   * @return A paging handler
   */

  public static IdDatabaseUserSearchPagingType create(
    final IdUserSearchParameters inParameters)
  {
    return new IdDatabaseUserSearchPaging(inParameters);
  }

  @Override
  public IdUserSearchParameters pageParameters()
  {
    return this.parameters;
  }

  @Override
  public int pageNumber()
  {
    return this.currentPage;
  }

  @Override
  public long pageFirstOffset()
  {
    return Integer.toUnsignedLong(this.currentPage)
           * Integer.toUnsignedLong(this.parameters.limit());
  }

  @Override
  public int pageCount()
  {
    return this.pagesCountApproximate;
  }

  @Override
  public boolean pageNextAvailable()
  {
    return this.pages.containsKey(Integer.valueOf(this.currentPage + 1));
  }

  @Override
  public List<IdUserSummary> pagePrevious(
    final IdDatabaseUsersQueriesType queries)
    throws IdDatabaseException
  {
    if (this.currentPage > 0) {
      this.currentPage = this.currentPage - 1;
    }
    return this.pageCurrent(queries);
  }

  @Override
  public List<IdUserSummary> pageNext(
    final IdDatabaseUsersQueriesType queries)
    throws IdDatabaseException
  {
    final var nextPage = this.currentPage + 1;
    if (this.pages.containsKey(Integer.valueOf(nextPage))) {
      this.currentPage = nextPage;
    }
    return this.pageCurrent(queries);
  }

  @Override
  public List<IdUserSummary> pageCurrent(
    final IdDatabaseUsersQueriesType queries)
    throws IdDatabaseException
  {
    final var page =
      this.pages.get(Integer.valueOf(this.currentPage));

    final var count =
      (double) queries.userSearchCount(this.parameters);
    final var limit =
      (double) this.parameters.limit();
    final var eventPages =
      count / limit;

    this.pagesCountApproximate =
      Math.max(1, (int) eventPages);

    final var results =
      queries.userSearch(this.parameters, page.seek);

    if (results.size() == this.parameters.limit()) {
      final var nextPage =
        this.pages.computeIfAbsent(
          Integer.valueOf(this.currentPage + 1),
          integer -> new Page()
        );

      final var lastSummary = results.get(results.size() - 1);
      nextPage.seek = Optional.of(
        fieldsForSeek(lastSummary, this.parameters.ordering())
      );
    }

    return results;
  }

  private static List<Object> fieldsForSeek(
    final IdUserSummary summary,
    final IdUserOrdering ordering)
  {
    final var orderColumns =
      ordering.ordering();
    final var columnCount =
      orderColumns.size();
    final var fields =
      new ArrayList<>(columnCount);

    for (int index = 0; index < columnCount; ++index) {
      final var columnOrdering = orderColumns.get(index);
      fields.add(
        switch (columnOrdering.column()) {
          case BY_ID -> {
            yield summary.id();
          }
          case BY_IDNAME -> {
            yield summary.idName().value();
          }
          case BY_REALNAME -> {
            yield summary.realName().value();
          }
          case BY_TIME_CREATED -> {
            yield summary.timeCreated();
          }
          case BY_TIME_UPDATED -> {
            yield summary.timeUpdated();
          }
        });
      Objects.requireNonNull(fields.get(index), "fields.get(index)");
    }

    return List.copyOf(fields);
  }
}

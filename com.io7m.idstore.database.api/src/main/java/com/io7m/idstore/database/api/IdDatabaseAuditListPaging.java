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

import com.io7m.idstore.model.IdAuditEvent;
import com.io7m.idstore.model.IdAuditSearchParameters;

import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A paging handler for audit events.
 */

public final class IdDatabaseAuditListPaging
  implements IdDatabaseAuditListPagingType
{
  private final IdAuditSearchParameters parameters;
  private volatile int currentPage;
  private final ConcurrentHashMap<Integer, Page> pages;
  private volatile int pagesCountApproximate;

  private IdDatabaseAuditListPaging(
    final IdAuditSearchParameters inParameters)
  {
    this.parameters =
      Objects.requireNonNull(inParameters, "parameters");

    this.currentPage = 0;
    this.pages = new ConcurrentHashMap<Integer, Page>();
    this.pages.put(Integer.valueOf(0), new Page());
  }

  private static final class Page
  {
    private volatile OptionalLong seek = OptionalLong.empty();

    Page()
    {

    }
  }

  /**
   * A paging handler for audit events.
   *
   * @param inParameters The audit list parameters
   *
   * @return A paging handler
   */

  public static IdDatabaseAuditListPagingType create(
    final IdAuditSearchParameters inParameters)
  {
    return new IdDatabaseAuditListPaging(inParameters);
  }

  @Override
  public IdAuditSearchParameters pageParameters()
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
  public List<IdAuditEvent> pagePrevious(
    final IdDatabaseAuditQueriesType queries)
    throws IdDatabaseException
  {
    if (this.currentPage > 0) {
      this.currentPage = this.currentPage - 1;
    }
    return this.pageCurrent(queries);
  }

  @Override
  public List<IdAuditEvent> pageNext(
    final IdDatabaseAuditQueriesType queries)
    throws IdDatabaseException
  {
    final var nextPage = this.currentPage + 1;
    if (this.pages.containsKey(Integer.valueOf(nextPage))) {
      this.currentPage = nextPage;
    }
    return this.pageCurrent(queries);
  }

  @Override
  public List<IdAuditEvent> pageCurrent(
    final IdDatabaseAuditQueriesType queries)
    throws IdDatabaseException
  {
    final var page =
      this.pages.get(Integer.valueOf(this.currentPage));

    final var eventCount =
      (double) queries.auditCount(this.parameters);
    final var eventLimit =
      (double) this.parameters.limit();
    final var eventPages =
      eventCount / eventLimit;

    this.pagesCountApproximate =
      (int) Math.max(1, Math.round(eventPages));

    final var results =
      queries.auditEvents(this.parameters, page.seek);

    if (results.size() == this.parameters.limit()) {
      final var nextPage =
        this.pages.computeIfAbsent(
          Integer.valueOf(this.currentPage + 1),
          integer -> new Page()
        );

      nextPage.seek = OptionalLong.of(results.get(results.size() - 1).id());
    }

    return results;
  }

}

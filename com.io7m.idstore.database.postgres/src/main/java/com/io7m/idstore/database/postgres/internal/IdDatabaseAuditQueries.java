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

package com.io7m.idstore.database.postgres.internal;

import com.io7m.idstore.database.api.IdDatabaseAuditEventsSearchType;
import com.io7m.idstore.database.api.IdDatabaseAuditQueriesType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.postgres.internal.tables.records.AuditRecord;
import com.io7m.idstore.model.IdAuditEvent;
import com.io7m.idstore.model.IdAuditSearchParameters;
import com.io7m.idstore.model.IdPage;
import com.io7m.jqpage.core.JQField;
import com.io7m.jqpage.core.JQKeysetRandomAccessPageDefinition;
import com.io7m.jqpage.core.JQKeysetRandomAccessPagination;
import com.io7m.jqpage.core.JQOrder;
import org.jooq.Condition;
import org.jooq.Select;
import org.jooq.Table;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.io7m.idstore.database.postgres.internal.IdDatabaseExceptions.handleDatabaseException;
import static com.io7m.idstore.database.postgres.internal.Tables.AUDIT;

final class IdDatabaseAuditQueries
  extends IdBaseQueries
  implements IdDatabaseAuditQueriesType
{
  IdDatabaseAuditQueries(
    final IdDatabaseTransaction inTransaction)
  {
    super(inTransaction);
  }

  @Override
  public IdDatabaseAuditEventsSearchType auditEventsSearch(
    final IdAuditSearchParameters parameters)
    throws IdDatabaseException
  {
    Objects.requireNonNull(parameters, "parameters");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan(
        "IdDatabaseAuditQueries.auditEventsSearch.create");

    try {
      /*
       * The events must lie within the given time ranges.
       */

      final var timeCreatedCondition =
        DSL.condition(
          AUDIT.TIME.ge(parameters.timeRange().timeLower())
            .and(AUDIT.TIME.le(parameters.timeRange().timeUpper()))
        );

      /*
       * Search queries might be present.
       */

      Condition searchCondition = DSL.trueCondition();

      final var typeOpt = parameters.type();
      if (typeOpt.isPresent()) {
        final var q = "%%%s%%".formatted(typeOpt.get());
        searchCondition =
          searchCondition.and(DSL.condition(AUDIT.TYPE.likeIgnoreCase(q)));
      }

      final var ownerOpt = parameters.owner();
      if (ownerOpt.isPresent()) {
        final var q = "%%%s%%".formatted(ownerOpt.get());
        searchCondition =
          searchCondition.and(DSL.condition(AUDIT.USER_ID.likeIgnoreCase(q)));
      }

      final var msgOpt = parameters.message();
      if (msgOpt.isPresent()) {
        final var q = "%%%s%%".formatted(msgOpt.get());
        searchCondition =
          searchCondition.and(DSL.condition(AUDIT.MESSAGE.likeIgnoreCase(q)));
      }

      final var allConditions =
        timeCreatedCondition.and(searchCondition);

      final var baseTable =
        AUDIT.where(allConditions);

      final var pages =
        JQKeysetRandomAccessPagination.createPageDefinitions(
          context,
          baseTable,
          List.of(new JQField(AUDIT.ID, JQOrder.ASCENDING)),
          List.of(allConditions),
          List.of(),
          Integer.toUnsignedLong(parameters.limit())
        );

      return new AuditEventsSearch(baseTable, pages);
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(this.transaction(), e);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public void auditPut(
    final UUID userId,
    final OffsetDateTime time,
    final String type,
    final String message)
    throws IdDatabaseException
  {
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(time, "time");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(message, "message");

    final var transaction = this.transaction();
    final var context = transaction.createContext();

    final var querySpan =
      transaction.createQuerySpan("IdDatabaseAuditQueries.auditPut");

    try {
      context.insertInto(AUDIT)
        .set(AUDIT.TIME, time)
        .set(AUDIT.TYPE, type)
        .set(AUDIT.USER_ID, userId)
        .set(AUDIT.MESSAGE, message)
        .execute();
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e);
    } finally {
      querySpan.end();
    }
  }

  private static final class AuditEventsSearch
    extends IdAbstractSearch<IdDatabaseAuditQueries, IdDatabaseAuditQueriesType, IdAuditEvent>
    implements IdDatabaseAuditEventsSearchType
  {
    private final Table<AuditRecord> table;

    AuditEventsSearch(
      final Table<AuditRecord> inTable,
      final List<JQKeysetRandomAccessPageDefinition> inPages)
    {
      super(inPages);
      this.table = Objects.requireNonNull(inTable, "inTable");
    }

    @Override
    protected IdPage<IdAuditEvent> page(
      final IdDatabaseAuditQueries queries,
      final JQKeysetRandomAccessPageDefinition page)
      throws IdDatabaseException
    {
      final var transaction =
        queries.transaction();
      final var context =
        transaction.createContext();

      final var querySpan =
        transaction.createQuerySpan(
          "IdDatabaseAuditQueries.auditEventsSearch.page");

      try {
        final var query =
          context.selectFrom(this.table)
            .orderBy(page.orderBy());

        final var seek = page.seek();
        final Select<AuditRecord> select;
        if (seek.length != 0) {
          select = query.seek(seek).limit(Long.valueOf(page.limit()));
        } else {
          select = query.limit(Long.valueOf(page.limit()));
        }

        final var items =
          select.fetch().map(record -> {
            return new IdAuditEvent(
              record.getValue(AUDIT.ID).longValue(),
              record.getValue(AUDIT.USER_ID),
              record.getValue(AUDIT.TIME),
              record.getValue(AUDIT.TYPE),
              record.getValue(AUDIT.MESSAGE)
            );
          });

        return new IdPage<>(
          items,
          (int) page.index(),
          this.pageCount(),
          page.firstOffset()
        );
      } catch (final DataAccessException e) {
        querySpan.recordException(e);
        throw handleDatabaseException(transaction, e);
      } finally {
        querySpan.end();
      }
    }
  }
}

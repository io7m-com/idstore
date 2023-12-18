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

package com.io7m.idstore.database.postgres.internal;

import com.io7m.idstore.database.api.IdDatabaseAuditEventsSearchType;
import com.io7m.idstore.database.api.IdDatabaseAuditQueriesType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.model.IdAuditEvent;
import com.io7m.idstore.model.IdAuditSearchParameters;
import com.io7m.idstore.model.IdPage;
import com.io7m.jqpage.core.JQField;
import com.io7m.jqpage.core.JQKeysetRandomAccessPageDefinition;
import com.io7m.jqpage.core.JQKeysetRandomAccessPagination;
import com.io7m.jqpage.core.JQKeysetRandomAccessPaginationParameters;
import com.io7m.jqpage.core.JQOrder;
import org.jooq.Condition;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.postgres.extensions.bindings.HstoreBinding;
import org.jooq.postgres.extensions.types.Hstore;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.io7m.idstore.database.postgres.internal.IdDatabaseExceptions.handleDatabaseException;
import static com.io7m.idstore.database.postgres.internal.Tables.AUDIT;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.DB_STATEMENT;

final class IdDatabaseAuditQueries
  extends IdBaseQueries
  implements IdDatabaseAuditQueriesType
{
  private static final DataType<Hstore> AU_DATA_TYPE =
    SQLDataType.OTHER.asConvertedDataType(new HstoreBinding());

  static final Field<Hstore> AU_DATA =
    DSL.field("DATA", AU_DATA_TYPE);

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

      final var allConditions =
        timeCreatedCondition.and(searchCondition);

      final var baseTable =
        AUDIT.where(allConditions);

      final var pages =
        JQKeysetRandomAccessPagination.createPageDefinitions(
          context,
          JQKeysetRandomAccessPaginationParameters.forTable(baseTable)
            .setPageSize(Integer.toUnsignedLong(parameters.limit()))
            .addSortField(new JQField(AUDIT.ID, JQOrder.ASCENDING))
            .addWhereCondition(allConditions)
            .setStatementListener(statement -> {
              querySpan.setAttribute(DB_STATEMENT, statement.toString());
            })
            .build()
        );

      return new AuditEventsSearch(pages);
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(this.transaction(), e, Map.of());
    } finally {
      querySpan.end();
    }
  }

  @Override
  public void auditPut(
    final UUID userId,
    final OffsetDateTime time,
    final String type,
    final Map<String, String> data)
    throws IdDatabaseException
  {
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(time, "time");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(data, "data");

    final var transaction = this.transaction();
    final var context = transaction.createContext();

    final var querySpan =
      transaction.createQuerySpan("IdDatabaseAuditQueries.auditPut");

    try {
      context.insertInto(AUDIT)
        .set(AUDIT.TIME, time)
        .set(AUDIT.TYPE, type)
        .set(AUDIT.USER_ID, userId)
        .set(AU_DATA, Hstore.hstore(data))
        .execute();
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e, Map.of());
    } finally {
      querySpan.end();
    }
  }

  private static final class AuditEventsSearch
    extends IdAbstractSearch<IdDatabaseAuditQueries, IdDatabaseAuditQueriesType, IdAuditEvent>
    implements IdDatabaseAuditEventsSearchType
  {
    AuditEventsSearch(
      final List<JQKeysetRandomAccessPageDefinition> inPages)
    {
      super(inPages);
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
          page.queryFields(
            context,
            List.of(
              AUDIT.ID,
              AUDIT.USER_ID,
              AUDIT.TIME,
              AUDIT.TYPE,
              AU_DATA
            )
          );

        querySpan.setAttribute(DB_STATEMENT, query.toString());

        final var items =
          query.fetch().map(record -> {
            return new IdAuditEvent(
              record.getValue(AUDIT.ID).longValue(),
              record.getValue(AUDIT.USER_ID),
              record.getValue(AUDIT.TIME),
              record.getValue(AUDIT.TYPE),
              record.getValue(AU_DATA).data()
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
        throw handleDatabaseException(transaction, e, Map.of());
      } finally {
        querySpan.end();
      }
    }
  }
}

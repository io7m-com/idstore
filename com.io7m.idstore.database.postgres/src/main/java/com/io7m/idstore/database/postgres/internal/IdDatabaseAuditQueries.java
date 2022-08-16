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

import com.io7m.idstore.database.api.IdDatabaseAuditQueriesType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.postgres.internal.tables.records.AuditRecord;
import com.io7m.idstore.model.IdAuditEvent;
import com.io7m.idstore.model.IdSubsetMatch;
import org.jooq.exception.DataAccessException;

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

  private static IdAuditEvent toAuditEvent(
    final AuditRecord record)
  {
    return new IdAuditEvent(
      record.getId().longValue(),
      record.getUserId(),
      record.getTime(),
      record.getType(),
      record.getMessage()
    );
  }

  @Override
  public List<IdAuditEvent> auditEvents(
    final OffsetDateTime fromInclusive,
    final OffsetDateTime toInclusive,
    final IdSubsetMatch<String> owner,
    final IdSubsetMatch<String> type,
    final IdSubsetMatch<String> message)
    throws IdDatabaseException
  {
    Objects.requireNonNull(fromInclusive, "fromInclusive");
    Objects.requireNonNull(toInclusive, "toInclusive");
    Objects.requireNonNull(owner, "owner");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(message, "message");

    try (var transaction = this.transaction()) {
      final var context = transaction.createContext();
      try {
        var selection =
          context.selectFrom(AUDIT)
            .where(AUDIT.TIME.ge(fromInclusive))
            .and(AUDIT.TIME.le(toInclusive));

        final var ownerInclude = owner.include();
        if (!ownerInclude.isEmpty()) {
          final var q = "%%%s%%".formatted(ownerInclude);
          selection = selection.and(AUDIT.USER_ID.likeIgnoreCase(q));
        }
        final var ownerExclude = owner.exclude();
        if (!ownerExclude.isEmpty()) {
          final var q = "%%%s%%".formatted(ownerExclude);
          selection = selection.andNot(AUDIT.USER_ID.likeIgnoreCase(q));
        }

        final var typeInclude = type.include();
        if (!typeInclude.isEmpty()) {
          final var q = "%%%s%%".formatted(typeInclude);
          selection = selection.and(AUDIT.TYPE.likeIgnoreCase(q));
        }
        final var typeExclude = type.exclude();
        if (!typeExclude.isEmpty()) {
          final var q = "%%%s%%".formatted(typeExclude);
          selection = selection.andNot(AUDIT.TYPE.likeIgnoreCase(q));
        }

        final var messageInclude = message.include();
        if (!messageInclude.isEmpty()) {
          final var q = "%%%s%%".formatted(messageInclude);
          selection = selection.and(AUDIT.MESSAGE.likeIgnoreCase(q));
        }
        final var messageExclude = message.exclude();
        if (!messageExclude.isEmpty()) {
          final var q = "%%%s%%".formatted(messageExclude);
          selection = selection.andNot(AUDIT.MESSAGE.likeIgnoreCase(q));
        }

        return selection.stream()
          .map(IdDatabaseAuditQueries::toAuditEvent)
          .toList();
      } catch (final DataAccessException e) {
        throw handleDatabaseException(transaction, e);
      }
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

    final var context =
      this.transaction().createContext();

    try {
      context.insertInto(AUDIT)
        .set(AUDIT.TIME, time)
        .set(AUDIT.TYPE, type)
        .set(AUDIT.USER_ID, userId)
        .set(AUDIT.MESSAGE, message)
        .execute();
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    }
  }
}

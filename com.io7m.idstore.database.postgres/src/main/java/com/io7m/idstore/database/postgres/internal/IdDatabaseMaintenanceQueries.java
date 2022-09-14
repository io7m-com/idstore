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

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseMaintenanceQueriesType;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.io7m.idstore.database.postgres.internal.IdDatabaseExceptions.handleDatabaseException;
import static com.io7m.idstore.database.postgres.internal.Tables.BANS;
import static com.io7m.idstore.database.postgres.internal.Tables.EMAIL_VERIFICATIONS;
import static java.lang.Integer.valueOf;

/**
 * The maintenance queries.
 */

public final class IdDatabaseMaintenanceQueries
  extends IdBaseQueries
  implements IdDatabaseMaintenanceQueriesType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdDatabaseMaintenanceQueries.class);

  IdDatabaseMaintenanceQueries(
    final IdDatabaseTransaction inTransaction)
  {
    super(inTransaction);
  }

  @Override
  public void runMaintenance()
    throws IdDatabaseException
  {
    this.runExpireEmailVerifications();
    this.runExpireBans();
  }

  private void runExpireEmailVerifications()
    throws IdDatabaseException
  {
    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();

    try {
      final var deleted =
      context.deleteFrom(EMAIL_VERIFICATIONS)
        .where(EMAIL_VERIFICATIONS.EXPIRES.lt(this.currentTime()))
        .execute();

      LOG.debug("deleted {} expired email verifications", valueOf(deleted));
    } catch (final DataAccessException e) {
      throw handleDatabaseException(transaction, e);
    }
  }

  private void runExpireBans()
    throws IdDatabaseException
  {
    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();

    try {
      final var deleted =
        context.deleteFrom(BANS)
          .where(BANS.EXPIRES.lt(this.currentTime()))
          .execute();

      LOG.debug("deleted {} expired bans", valueOf(deleted));
    } catch (final DataAccessException e) {
      throw handleDatabaseException(transaction, e);
    }
  }
}

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

import com.io7m.idstore.database.api.IdDatabaseAdminsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseAuditQueriesType;
import com.io7m.idstore.database.api.IdDatabaseEmailsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseMaintenanceQueriesType;
import com.io7m.idstore.database.api.IdDatabaseQueriesType;
import com.io7m.idstore.database.api.IdDatabaseRole;
import com.io7m.idstore.database.api.IdDatabaseTransactionType;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.idstore.database.postgres.internal.Tables.ADMINS;
import static com.io7m.idstore.database.postgres.internal.Tables.USERS;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_NONEXISTENT;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_OR_USER_UNSET;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_UNSET;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SQL_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SQL_ERROR_UNSUPPORTED_QUERY_CLASS;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.USER_NONEXISTENT;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.USER_UNSET;
import static org.jooq.SQLDialect.POSTGRES;

final class IdDatabaseTransaction
  implements IdDatabaseTransactionType
{
  private final IdDatabaseConnection connection;
  private Instant timeStart;
  private UUID currentUserId;
  private UUID currentAdminId;

  IdDatabaseTransaction(
    final IdDatabaseConnection inConnection,
    final Instant inTimeStart)
  {
    this.connection =
      Objects.requireNonNull(inConnection, "connection");
    this.timeStart =
      Objects.requireNonNull(inTimeStart, "timeStart");
  }

  void setRole(
    final IdDatabaseRole role)
    throws SQLException
  {
    switch (role) {
      case ADMIN -> {

      }
      case IDSTORE -> {
        try (var st =
               this.connection.connection()
                 .prepareStatement("set role idstore")) {
          st.execute();
        }
      }
      case NONE -> {
        try (var st =
               this.connection.connection()
                 .prepareStatement("set role idstore_none")) {
          st.execute();
        }
      }
    }
  }

  @Override
  public <T extends IdDatabaseQueriesType> T queries(
    final Class<T> qClass)
    throws IdDatabaseException
  {
    if (Objects.equals(qClass, IdDatabaseAdminsQueriesType.class)) {
      return qClass.cast(new IdDatabaseAdminsQueries(this));
    }
    if (Objects.equals(qClass, IdDatabaseUsersQueriesType.class)) {
      return qClass.cast(new IdDatabaseUsersQueries(this));
    }
    if (Objects.equals(qClass, IdDatabaseAuditQueriesType.class)) {
      return qClass.cast(new IdDatabaseAuditQueries(this));
    }
    if (Objects.equals(qClass, IdDatabaseEmailsQueriesType.class)) {
      return qClass.cast(new IdDatabaseEmailsQueries(this));
    }
    if (Objects.equals(qClass, IdDatabaseMaintenanceQueriesType.class)) {
      return qClass.cast(new IdDatabaseMaintenanceQueries(this));
    }

    throw new IdDatabaseException(
      "Unsupported query type: %s".formatted(qClass),
      SQL_ERROR_UNSUPPORTED_QUERY_CLASS
    );
  }

  public DSLContext createContext()
  {
    final var sqlConnection =
      this.connection.connection();
    final var settings =
      this.connection.database().settings();
    return DSL.using(sqlConnection, POSTGRES, settings);
  }

  public Clock clock()
  {
    return this.connection.database().clock();
  }

  @Override
  public void rollback()
    throws IdDatabaseException
  {
    try {
      this.connection.connection().rollback();
      this.connection.database()
        .metrics()
        .addTransactionTimeRolledBack(this.updateTransactionTime());
    } catch (final SQLException e) {
      throw new IdDatabaseException(e.getMessage(), e, SQL_ERROR);
    }
  }

  @Override
  public void commit()
    throws IdDatabaseException
  {
    try {
      this.connection.connection().commit();
      this.connection.database()
        .metrics()
        .addTransactionTimeCommitted(this.updateTransactionTime());
    } catch (final SQLException e) {
      throw new IdDatabaseException(e.getMessage(), e, SQL_ERROR);
    }
  }

  private double updateTransactionTime()
  {
    final var timeNow =
      this.connection.database()
        .clock()
        .instant();
    final var diff =
      Duration.between(this.timeStart, timeNow);
    final var timeMs =
      (double) diff.toMillis() / 1000.0;

    this.timeStart = timeNow;
    return timeMs;
  }

  @Override
  public void close()
    throws IdDatabaseException
  {
    this.rollback();
  }

  @Override
  public void userIdSet(
    final UUID userId)
    throws IdDatabaseException
  {
    Objects.requireNonNull(userId, "userId");

    final var context = this.createContext();

    try {
      final var userOpt =
        context.select(USERS.ID)
          .from(USERS)
          .where(USERS.ID.eq(userId))
          .fetchOptional()
          .map(r -> r.getValue(USERS.ID));

      if (userOpt.isEmpty()) {
        throw new IdDatabaseException(
          "No such user: %s".formatted(userId),
          USER_NONEXISTENT
        );
      }

      this.currentUserId = userId;
      this.currentAdminId = null;
    } catch (final DataAccessException e) {
      throw new IdDatabaseException(e.getMessage(), e, SQL_ERROR);
    }
  }

  @Override
  public UUID userId()
    throws IdDatabaseException
  {
    return Optional.ofNullable(this.currentUserId).orElseThrow(() -> {
      return new IdDatabaseException(
        "A user must be set before calling this method.",
        USER_UNSET
      );
    });
  }

  @Override
  public void adminIdSet(
    final UUID adminId)
    throws IdDatabaseException
  {
    Objects.requireNonNull(adminId, "adminId");

    final var context = this.createContext();

    try {
      final var adminOpt =
        context.select(ADMINS.ID)
          .from(ADMINS)
          .where(ADMINS.ID.eq(adminId))
          .fetchOptional()
          .map(r -> r.getValue(ADMINS.ID));

      if (adminOpt.isEmpty()) {
        throw new IdDatabaseException(
          "No such admin: %s".formatted(adminId),
          ADMIN_NONEXISTENT
        );
      }

      this.currentAdminId = adminId;
      this.currentUserId = null;
    } catch (final DataAccessException e) {
      throw new IdDatabaseException(e.getMessage(), e, SQL_ERROR);
    }
  }

  @Override
  public UUID adminId()
    throws IdDatabaseException
  {
    return Optional.ofNullable(this.currentAdminId).orElseThrow(() -> {
      return new IdDatabaseException(
        "A admin must be set before calling this method.",
        ADMIN_UNSET
      );
    });
  }

  @Override
  public UUID executorId()
    throws IdDatabaseException
  {
    return Optional.ofNullable(this.currentAdminId)
      .or(() -> Optional.ofNullable(this.currentUserId))
      .orElseThrow(() -> {
        return new IdDatabaseException(
          "A user or admin must be set before calling this method.",
          ADMIN_OR_USER_UNSET
        );
      });
  }
}

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

import com.io7m.idstore.database.api.IdDatabaseEmailsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.postgres.internal.tables.records.EmailVerificationsRecord;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdEmailOwner;
import com.io7m.idstore.model.IdEmailVerification;
import com.io7m.idstore.model.IdEmailVerificationOperation;
import com.io7m.idstore.model.IdEmailVerificationResolution;
import com.io7m.idstore.model.IdToken;
import org.jooq.exception.DataAccessException;
import org.jooq.postgres.extensions.types.Hstore;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.idstore.database.postgres.internal.IdDatabaseAuditQueries.AU_DATA;
import static com.io7m.idstore.database.postgres.internal.IdDatabaseExceptions.handleDatabaseException;
import static com.io7m.idstore.database.postgres.internal.IdDatabaseUsersQueries.userDoesNotExist;
import static com.io7m.idstore.database.postgres.internal.Tables.AUDIT;
import static com.io7m.idstore.database.postgres.internal.Tables.EMAILS;
import static com.io7m.idstore.database.postgres.internal.Tables.EMAIL_VERIFICATIONS;
import static com.io7m.idstore.database.postgres.internal.Tables.USER_IDS;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.EMAIL_VERIFICATION_DUPLICATE;

final class IdDatabaseEmailsQueries
  extends IdBaseQueries
  implements IdDatabaseEmailsQueriesType
{
  IdDatabaseEmailsQueries(
    final IdDatabaseTransaction inTransaction)
  {
    super(inTransaction);
  }

  @Override
  public Optional<IdEmailOwner> emailExists(
    final IdEmail email)
    throws IdDatabaseException
  {
    Objects.requireNonNull(email, "email");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseEmailsQueries.emailExists");

    final var attributes =
      Map.ofEntries(
        Map.entry("Email", email.value())
      );

    try {
      final var emailRecordOpt =
        context.selectFrom(EMAILS)
          .where(EMAILS.EMAIL_ADDRESS.equalIgnoreCase(email.value()))
          .fetchOptional();

      if (emailRecordOpt.isEmpty()) {
        return Optional.empty();
      }

      final var emailRecord = emailRecordOpt.get();
      if (emailRecord.getAdminId() != null) {
        return Optional.of(
          new IdEmailOwner(
            true,
            emailRecord.getAdminId(),
            email
          )
        );
      }

      return Optional.of(
        new IdEmailOwner(
          false,
          emailRecord.getUserId(),
          email
        )
      );
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e, attributes);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public void emailVerificationCreate(
    final IdEmailVerification verification)
    throws IdDatabaseException
  {
    Objects.requireNonNull(verification, "verification");

    final var transaction = this.transaction();
    final var context = transaction.createContext();
    final var executor = transaction.userId();

    final var querySpan =
      transaction.createQuerySpan(
        "IdDatabaseEmailsQueries.emailVerificationCreate");

    final var attributes =
      Map.ofEntries(
        Map.entry("User ID", verification.user().toString()),
        Map.entry("Email", verification.email().value()),
        Map.entry("Expires", verification.expires().toString())
      );

    try {
      context.selectFrom(USER_IDS)
        .where(USER_IDS.ID.eq(verification.user()))
        .fetchOptional()
        .orElseThrow(() -> userDoesNotExist(attributes));

      {
        final var tokenValue =
          verification.tokenPermit().value();
        final var existing =
          context.selectFrom(EMAIL_VERIFICATIONS)
            .where(EMAIL_VERIFICATIONS.TOKEN_PERMIT.eq(tokenValue))
            .fetchOptional();

        if (existing.isPresent()) {
          throw new IdDatabaseException(
            "Email verification token already exists.",
            EMAIL_VERIFICATION_DUPLICATE,
            Map.of("Token", tokenValue),
            Optional.of("Use a different token.")
          );
        }
      }

      {
        final var tokenValue =
          verification.tokenDeny().value();
        final var existing =
          context.selectFrom(EMAIL_VERIFICATIONS)
            .where(EMAIL_VERIFICATIONS.TOKEN_DENY.eq(tokenValue))
            .fetchOptional();

        if (existing.isPresent()) {
          throw new IdDatabaseException(
            "Email verification token already exists.",
            EMAIL_VERIFICATION_DUPLICATE,
            Map.of("Token", tokenValue),
            Optional.of("Use a different token.")
          );
        }
      }

      context.insertInto(EMAIL_VERIFICATIONS)
        .set(EMAIL_VERIFICATIONS.EMAIL, verification.email().value())
        .set(EMAIL_VERIFICATIONS.EXPIRES, verification.expires())
        .set(EMAIL_VERIFICATIONS.OPERATION, verification.operation().name())
        .set(EMAIL_VERIFICATIONS.TOKEN_DENY, verification.tokenDeny().value())
        .set(
          EMAIL_VERIFICATIONS.TOKEN_PERMIT,
          verification.tokenPermit().value())
        .set(EMAIL_VERIFICATIONS.USER_ID, verification.user())
        .execute();

      final var auditData =
        Map.ofEntries(
          Map.entry("TokenPermit", verification.tokenPermit().value()),
          Map.entry("TokenDeny", verification.tokenDeny().value()),
          Map.entry("Email", verification.email().value())
        );

      context.insertInto(AUDIT)
        .set(AUDIT.USER_ID, executor)
        .set(AU_DATA, Hstore.hstore(auditData))
        .set(AUDIT.TYPE, "EMAIL_VERIFICATION_CREATED")
        .set(AUDIT.TIME, this.currentTime())
        .execute();

    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e, attributes);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public Optional<IdEmailVerification> emailVerificationGetPermit(
    final IdToken token)
    throws IdDatabaseException
  {
    Objects.requireNonNull(token, "token");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan(
        "IdDatabaseEmailsQueries.emailVerificationGetPermit");

    final var attributes =
      Map.ofEntries(
        Map.entry("Token", token.value())
      );

    try {
      return context.selectFrom(EMAIL_VERIFICATIONS)
        .where(EMAIL_VERIFICATIONS.TOKEN_PERMIT.eq(token.value()))
        .fetchOptional()
        .map(IdDatabaseEmailsQueries::mapVerification);
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e, attributes);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public Optional<IdEmailVerification> emailVerificationGetDeny(
    final IdToken token)
    throws IdDatabaseException
  {
    Objects.requireNonNull(token, "token");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan(
        "IdDatabaseEmailsQueries.emailVerificationGetDeny");

    final var attributes =
      Map.ofEntries(
        Map.entry("Token", token.value())
      );

    try {
      return context.selectFrom(EMAIL_VERIFICATIONS)
        .where(EMAIL_VERIFICATIONS.TOKEN_DENY.eq(token.value()))
        .fetchOptional()
        .map(IdDatabaseEmailsQueries::mapVerification);
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e, attributes);
    } finally {
      querySpan.end();
    }
  }

  private static IdEmailVerification mapVerification(
    final EmailVerificationsRecord record)
  {
    return new IdEmailVerification(
      record.getUserId(),
      new IdEmail(record.getEmail()),
      new IdToken(record.getTokenPermit()),
      new IdToken(record.getTokenDeny()),
      IdEmailVerificationOperation.valueOf(record.getOperation()),
      record.getExpires()
    );
  }

  @Override
  public void emailVerificationDelete(
    final IdToken token,
    final IdEmailVerificationResolution resolution)
    throws IdDatabaseException
  {
    Objects.requireNonNull(token, "token");
    Objects.requireNonNull(resolution, "resolution");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var executor =
      transaction.userId();
    final var querySpan =
      transaction.createQuerySpan(
        "IdDatabaseEmailsQueries.emailVerificationDelete");

    final var attributes =
      Map.ofEntries(
        Map.entry("Token", token.value()),
        Map.entry("Resolution", resolution.name())
      );

    try {
      final var condition =
        switch (resolution) {
          case PERMITTED -> {
            yield EMAIL_VERIFICATIONS.TOKEN_PERMIT.eq(token.value());
          }
          case DENIED -> {
            yield EMAIL_VERIFICATIONS.TOKEN_DENY.eq(token.value());
          }
          case EXPIRED -> {
            yield EMAIL_VERIFICATIONS.TOKEN_DENY.eq(token.value())
              .or(EMAIL_VERIFICATIONS.TOKEN_PERMIT.eq(token.value()));
          }
        };

      context.deleteFrom(EMAIL_VERIFICATIONS)
        .where(condition)
        .execute();

      final var auditData =
        Map.ofEntries(
          Map.entry("Token", token.value()),
          Map.entry("Resolution", resolution.name())
        );

      context.insertInto(AUDIT)
        .set(AUDIT.USER_ID, executor)
        .set(AU_DATA, Hstore.hstore(auditData))
        .set(AUDIT.TYPE, "EMAIL_VERIFICATION_DELETED")
        .set(AUDIT.TIME, this.currentTime())
        .execute();

    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e, attributes);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public long emailVerificationCount()
    throws IdDatabaseException
  {
    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var executor =
      transaction.userId();
    final var querySpan =
      transaction.createQuerySpan(
        "IdDatabaseEmailsQueries.emailVerificationCount");

    try {
      return Integer.toUnsignedLong(context.fetchCount(
        EMAIL_VERIFICATIONS, EMAIL_VERIFICATIONS.USER_ID.eq(executor)
      ));
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e, Map.of());
    } finally {
      querySpan.end();
    }
  }
}

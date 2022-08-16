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

import java.util.Objects;
import java.util.Optional;

import static com.io7m.idstore.database.postgres.internal.IdDatabaseExceptions.handleDatabaseException;
import static com.io7m.idstore.database.postgres.internal.IdDatabaseUsersQueries.USER_DOES_NOT_EXIST;
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

    final var context =
      this.transaction().createContext();

    try {
      final var emailRecordOpt =
        context.selectFrom(EMAILS)
          .where(EMAILS.EMAIL_ADDRESS.eq(email.value()))
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
      throw handleDatabaseException(this.transaction(), e);
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
    final var owner = transaction.userId();

    try {
      context.selectFrom(USER_IDS)
        .where(USER_IDS.ID.eq(verification.user()))
        .fetchOptional()
        .orElseThrow(USER_DOES_NOT_EXIST);

      {
        final var existing =
          context.selectFrom(EMAIL_VERIFICATIONS)
            .where(EMAIL_VERIFICATIONS.TOKEN.eq(verification.token().value()))
            .fetchOptional();
        if (existing.isPresent()) {
          throw new IdDatabaseException(
            "Email verification token already exists.",
            EMAIL_VERIFICATION_DUPLICATE
          );
        }
      }

      context.insertInto(EMAIL_VERIFICATIONS)
        .set(EMAIL_VERIFICATIONS.TOKEN, verification.token().value())
        .set(EMAIL_VERIFICATIONS.EMAIL, verification.email().value())
        .set(EMAIL_VERIFICATIONS.USER_ID, verification.user())
        .set(EMAIL_VERIFICATIONS.OPERATION, verification.operation().name())
        .set(EMAIL_VERIFICATIONS.EXPIRES, verification.expires())
        .execute();

      context.insertInto(AUDIT)
        .set(AUDIT.USER_ID, owner)
        .set(AUDIT.MESSAGE,
             "%s|%s".formatted(verification.token(), verification.email()))
        .set(AUDIT.TYPE, "EMAIL_VERIFICATION_CREATED")
        .set(AUDIT.TIME, this.currentTime())
        .execute();

    } catch (final DataAccessException e) {
      throw handleDatabaseException(transaction, e);
    }
  }

  @Override
  public Optional<IdEmailVerification> emailVerificationGet(
    final IdToken token)
    throws IdDatabaseException
  {
    Objects.requireNonNull(token, "token");

    final var context =
      this.transaction().createContext();

    try {
      return context.selectFrom(EMAIL_VERIFICATIONS)
        .where(EMAIL_VERIFICATIONS.TOKEN.eq(token.value()))
        .fetchOptional()
        .map(IdDatabaseEmailsQueries::mapVerification);
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    }
  }

  private static IdEmailVerification mapVerification(
    final EmailVerificationsRecord record)
  {
    return new IdEmailVerification(
      record.getUserId(),
      new IdEmail(record.getEmail()),
      new IdToken(record.getToken()),
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

    final var transaction = this.transaction();
    final var context = transaction.createContext();
    final var owner = transaction.userId();

    try {
      context.deleteFrom(EMAIL_VERIFICATIONS)
        .where(EMAIL_VERIFICATIONS.TOKEN.eq(token.value()))
        .execute();

      context.insertInto(AUDIT)
        .set(AUDIT.USER_ID, owner)
        .set(AUDIT.MESSAGE, "%s|%s".formatted(token, resolution))
        .set(AUDIT.TYPE, "EMAIL_VERIFICATION_DELETED")
        .set(AUDIT.TIME, this.currentTime())
        .execute();

    } catch (final DataAccessException e) {
      throw handleDatabaseException(transaction, e);
    }
  }
}

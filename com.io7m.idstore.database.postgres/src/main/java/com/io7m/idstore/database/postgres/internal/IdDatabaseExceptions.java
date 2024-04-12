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

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseTransactionType;
import org.jooq.exception.DataAccessException;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_NONEXISTENT;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.EMAIL_DUPLICATE;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.EMAIL_ONE_REQUIRED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.OPERATION_NOT_PERMITTED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SQL_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.USER_DUPLICATE_ID;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.USER_DUPLICATE_ID_NAME;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.USER_NONEXISTENT;

/**
 * Functions to handle database exceptions.
 */

public final class IdDatabaseExceptions
{
  private IdDatabaseExceptions()
  {

  }

  /**
   * Handle a data access exception.
   *
   * @param transaction The transaction
   * @param e           The exception
   * @param attributes  The extra exception attributes
   *
   * @return The resulting exception
   */

  public static IdDatabaseException handleDatabaseException(
    final IdDatabaseTransactionType transaction,
    final DataAccessException e,
    final Map<String, String> attributes)
  {
    final var m = e.getMessage();

    IdDatabaseException result =
      new IdDatabaseException(
        m,
        e,
        SQL_ERROR,
        attributes,
        Optional.empty()
      );

    if (e.getCause() instanceof final PSQLException psqlException) {
      final var serverError =
        Objects.requireNonNullElse(
          psqlException.getServerErrorMessage(),
          new ServerErrorMessage("")
        );

      /*
       * See https://www.postgresql.org/docs/current/errcodes-appendix.html
       * for all of these numeric codes.
       */

      result = switch (psqlException.getSQLState()) {

        /*
         * foreign_key_violation
         */

        case "23503" -> {
          final var constraint =
            Objects.requireNonNullElse(serverError.getConstraint(), "");

          yield switch (constraint) {
            case "emails_user_id_fkey" -> {
              yield new IdDatabaseException(
                "User does not exist.",
                USER_NONEXISTENT,
                attributes,
                Optional.empty()
              );
            }

            case "emails_admin_id_fkey" -> {
              yield new IdDatabaseException(
                "Admin does not exist.",
                ADMIN_NONEXISTENT,
                attributes,
                Optional.empty()
              );
            }

            default -> {
              yield new IdDatabaseException(
                m,
                e,
                SQL_ERROR,
                attributes,
                Optional.empty()
              );
            }
          };
        }

        /*
         * unique_violation
         */

        case "23505" -> {
          final var constraint =
            Objects.requireNonNullElse(serverError.getConstraint(), "");

          yield switch (constraint) {
            case "users_id_name_index" -> {
              yield new IdDatabaseException(
                "User ID name already exists",
                USER_DUPLICATE_ID_NAME,
                attributes,
                Optional.empty()
              );
            }

            case "user_ids_pkey" -> {
              yield new IdDatabaseException(
                "User ID already exists",
                USER_DUPLICATE_ID,
                attributes,
                Optional.empty()
              );
            }

            case "emails_unique_lower_email_idx" -> {
              yield new IdDatabaseException(
                "Email already exists",
                EMAIL_DUPLICATE,
                attributes,
                Optional.empty()
              );
            }

            default -> {
              yield new IdDatabaseException(
                m,
                e,
                SQL_ERROR,
                attributes,
                Optional.empty()
              );
            }
          };
        }

        case "22021" -> {

          /*
           * PostgreSQL: character_not_in_repertoire
           */

          yield new IdDatabaseException(
            Objects.requireNonNullElse(
              serverError.getMessage(),
              e.getMessage()),
            e,
            PROTOCOL_ERROR,
            attributes,
            Optional.empty()
          );
        }

        /*
         * insufficient_privilege
         */

        case "42501" -> {
          yield new IdDatabaseException(
            m,
            e,
            OPERATION_NOT_PERMITTED,
            attributes,
            Optional.empty()
          );
        }

        /*
         * Custom state code defined in a trigger.
         */

        case "ID001" -> {
          yield new IdDatabaseException(
            m,
            e,
            EMAIL_ONE_REQUIRED,
            attributes,
            Optional.empty()
          );
        }

        default -> {
          yield new IdDatabaseException(
            m,
            e,
            SQL_ERROR,
            attributes,
            Optional.empty()
          );
        }
      };
    }

    try {
      transaction.rollback();
    } catch (final IdDatabaseException ex) {
      result.addSuppressed(ex);
    }
    return result;
  }
}

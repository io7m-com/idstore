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

package com.io7m.idstore.error_codes;

/**
 * Standard error codes.
 */

public final class IdStandardErrorCodes
{
  private IdStandardErrorCodes()
  {

  }

  /**
   * A client sent a broken message of some kind.
   */

  public static final IdErrorCode PROTOCOL_ERROR =
    new IdErrorCode("error-protocol");

  /**
   * Authenticating a user or admin failed.
   */

  public static final IdErrorCode AUTHENTICATION_ERROR =
    new IdErrorCode("error-authentication");

  /**
   * An internal I/O error.
   */

  public static final IdErrorCode IO_ERROR =
    new IdErrorCode("error-io");

  /**
   * An internal serialization error.
   */

  public static final IdErrorCode SERIALIZATION_ERROR =
    new IdErrorCode("error-serialization");

  /**
   * An error raised by the Trasco database versioning library.
   */

  public static final IdErrorCode TRASCO_ERROR =
    new IdErrorCode("error-trasco");

  /**
   * An internal SQL database error.
   */

  public static final IdErrorCode SQL_ERROR =
    new IdErrorCode("error-sql");

  /**
   * An internal SQL database error relating to database revisioning.
   */

  public static final IdErrorCode SQL_REVISION_ERROR =
    new IdErrorCode("error-sql-revision");

  /**
   * A violation of an SQL foreign key integrity constraint.
   */

  public static final IdErrorCode SQL_ERROR_FOREIGN_KEY =
    new IdErrorCode("error-sql-foreign-key");

  /**
   * A violation of an SQL uniqueness constraint.
   */

  public static final IdErrorCode SQL_ERROR_UNIQUE =
    new IdErrorCode("error-sql-unique");

  /**
   * An attempt was made to use a query class that is unsupported.
   */

  public static final IdErrorCode SQL_ERROR_UNSUPPORTED_QUERY_CLASS =
    new IdErrorCode("error-sql-unsupported-query-class");

  /**
   * A generic "operation not permitted" error.
   */

  public static final IdErrorCode OPERATION_NOT_PERMITTED =
    new IdErrorCode("error-operation-not-permitted");

  /**
   * An action was denied by the security policy.
   */

  public static final IdErrorCode SECURITY_POLICY_DENIED =
    new IdErrorCode("error-security-policy-denied");

  /**
   * The wrong HTTP method was used.
   */

  public static final IdErrorCode HTTP_METHOD_ERROR =
    new IdErrorCode("error-http-method");

  /**
   * An HTTP parameter was required but missing.
   */

  public static final IdErrorCode HTTP_PARAMETER_NONEXISTENT =
    new IdErrorCode("error-http-parameter-nonexistent");

  /**
   * An HTTP parameter had an invalid value.
   */

  public static final IdErrorCode HTTP_PARAMETER_INVALID =
    new IdErrorCode("error-http-parameter-invalid");

  /**
   * An HTTP request exceeded the size limit.
   */

  public static final IdErrorCode HTTP_SIZE_LIMIT =
    new IdErrorCode("error-http-size-limit");


  /**
   * An attempt was made to create a user that already exists.
   */

  public static final IdErrorCode USER_DUPLICATE =
    new IdErrorCode("error-user-duplicate");

  /**
   * An attempt was made to create a user that already exists (ID conflict).
   */

  public static final IdErrorCode USER_DUPLICATE_ID =
    new IdErrorCode("error-user-duplicate-id");

  /**
   * An attempt was made to create a user that already exists (ID name
   * conflict).
   */

  public static final IdErrorCode USER_DUPLICATE_ID_NAME =
    new IdErrorCode("error-user-duplicate-id-name");

  /**
   * An attempt was made to create a user that already exists (Email conflict).
   */

  public static final IdErrorCode USER_DUPLICATE_EMAIL =
    new IdErrorCode("error-user-duplicate-email");

  /**
   * An attempt was made to reference a user that does not exist.
   */

  public static final IdErrorCode USER_NONEXISTENT =
    new IdErrorCode("error-user-nonexistent");

  /**
   * An attempt was made to perform an operation that requires a user.
   */

  public static final IdErrorCode USER_UNSET =
    new IdErrorCode("error-user-unset");

  /**
   * A problem occurred with the format of a password (such as an unsupported
   * password algorithm).
   */

  public static final IdErrorCode PASSWORD_ERROR =
    new IdErrorCode("error-password");

  /**
   * An attempt was made to create a admin that already exists.
   */

  public static final IdErrorCode ADMIN_DUPLICATE =
    new IdErrorCode("error-admin-duplicate");

  /**
   * An attempt was made to create a admin that already exists (ID conflict).
   */

  public static final IdErrorCode ADMIN_DUPLICATE_ID =
    new IdErrorCode("error-admin-duplicate-id");

  /**
   * An attempt was made to create a admin that already exists (ID name
   * conflict).
   */

  public static final IdErrorCode ADMIN_DUPLICATE_ID_NAME =
    new IdErrorCode("error-admin-duplicate-id-name");

  /**
   * An attempt was made to create a admin that already exists (Email
   * conflict).
   */

  public static final IdErrorCode ADMIN_DUPLICATE_EMAIL =
    new IdErrorCode("error-admin-duplicate-email");

  /**
   * An attempt was made to reference a admin that does not exist.
   */

  public static final IdErrorCode ADMIN_NONEXISTENT =
    new IdErrorCode("error-admin-nonexistent");

  /**
   * An attempt was made to perform an operation that requires a admin.
   */

  public static final IdErrorCode ADMIN_UNSET =
    new IdErrorCode("error-admin-unset");

  /**
   * An attempt was made to create an initial admin in a database, but a admin
   * already existed.
   */

  public static final IdErrorCode ADMIN_NOT_INITIAL =
    new IdErrorCode("error-admin-not-initial");

  /**
   * An attempt was made to reference a nonexistent email verification token.
   */

  public static final IdErrorCode EMAIL_VERIFICATION_NONEXISTENT =
    new IdErrorCode("error-email-verification-nonexistent");

  /**
   * An attempt was made to create a email verification token that already
   * exists.
   */

  public static final IdErrorCode EMAIL_VERIFICATION_DUPLICATE =
    new IdErrorCode("error-email-verification-duplicate");

  /**
   * An email verification failed for any reason.
   */

  public static final IdErrorCode EMAIL_VERIFICATION_FAILED =
    new IdErrorCode("error-email-verification-failed");

  /**
   * An attempt was made to create an email that already exists.
   */

  public static final IdErrorCode EMAIL_DUPLICATE =
    new IdErrorCode("error-email-duplicate");

  /**
   * An attempt was made to reference an email that does not exist.
   */

  public static final IdErrorCode EMAIL_NONEXISTENT =
    new IdErrorCode("error-email-nonexistent");

  /**
   * An attempt was made to remove the last email address from a user.
   */

  public static final IdErrorCode EMAIL_ONE_REQUIRED =
    new IdErrorCode("error-email-one-required");

  /**
   * The mail system failed.
   */

  public static final IdErrorCode MAIL_SYSTEM_FAILURE =
    new IdErrorCode("mail-system-failure");
}

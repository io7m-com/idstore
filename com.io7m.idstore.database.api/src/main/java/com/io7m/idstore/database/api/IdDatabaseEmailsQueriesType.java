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

package com.io7m.idstore.database.api;

import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdEmailOwner;
import com.io7m.idstore.model.IdEmailVerification;
import com.io7m.idstore.model.IdEmailVerificationResolution;
import com.io7m.idstore.model.IdToken;

import java.util.Optional;

/**
 * The database queries involving emails.
 */

public non-sealed interface IdDatabaseEmailsQueriesType
  extends IdDatabaseQueriesType
{
  /**
   * Determine if an email address already exists.
   *
   * @param email The email address
   *
   * @return The user ID that owns the email address, if any
   *
   * @throws IdDatabaseException On errors
   */

  Optional<IdEmailOwner> emailExists(
    IdEmail email)
    throws IdDatabaseException;

  /**
   * Create a verification.
   *
   * @param verification The verification
   *
   * @throws IdDatabaseException On errors
   */

  @IdDatabaseRequiresUser
  void emailVerificationCreate(
    IdEmailVerification verification)
    throws IdDatabaseException;

  /**
   * Retrieve a verification by the "permit" token.
   *
   * @param token The verification token
   *
   * @return The verification, if one exists
   *
   * @throws IdDatabaseException On errors
   */

  Optional<IdEmailVerification> emailVerificationGetPermit(
    IdToken token)
    throws IdDatabaseException;

  /**
   * Retrieve a verification by the "deny" token.
   *
   * @param token The verification token
   *
   * @return The verification, if one exists
   *
   * @throws IdDatabaseException On errors
   */

  Optional<IdEmailVerification> emailVerificationGetDeny(
    IdToken token)
    throws IdDatabaseException;

  /**
   * Delete a verification.
   *
   * @param token      The verification token
   * @param resolution The resolution
   *
   * @throws IdDatabaseException On errors
   */

  @IdDatabaseRequiresUser
  void emailVerificationDelete(
    IdToken token,
    IdEmailVerificationResolution resolution)
    throws IdDatabaseException;

  /**
   * Count the number of active email verifications for the current user.
   *
   * @return The number of active email verifications
   *
   * @throws IdDatabaseException On errors
   */

  @IdDatabaseRequiresUser
  long emailVerificationCount()
    throws IdDatabaseException;
}

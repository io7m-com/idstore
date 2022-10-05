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

package com.io7m.idstore.database.api;


import com.io7m.idstore.model.IdBan;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdLogin;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPassword;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdToken;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.model.IdUserPasswordReset;
import com.io7m.idstore.model.IdUserSearchByEmailParameters;
import com.io7m.idstore.model.IdUserSearchParameters;
import com.io7m.idstore.model.IdUserSummary;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The database queries involving users.
 */

public non-sealed interface IdDatabaseUsersQueriesType
  extends IdDatabaseQueriesType
{
  /**
   * Create a user.
   *
   * @param idName   The ID name
   * @param realName The real name
   * @param email    The user email
   * @param password The hashed password
   *
   * @return The created user
   *
   * @throws IdDatabaseException On errors
   */

  @IdDatabaseRequiresUser
  default IdUser userCreate(
    final IdName idName,
    final IdRealName realName,
    final IdEmail email,
    final IdPassword password)
    throws IdDatabaseException
  {
    return this.userCreate(
      UUID.randomUUID(),
      idName,
      realName,
      email,
      OffsetDateTime.now(),
      password
    );
  }

  /**
   * Create a user.
   *
   * @param id       The user ID
   * @param idName   The ID name
   * @param created  The creation time
   * @param realName The real name
   * @param email    The user email
   * @param password The hashed password
   *
   * @return The created user
   *
   * @throws IdDatabaseException On errors
   */

  @IdDatabaseRequiresAdmin
  IdUser userCreate(
    UUID id,
    IdName idName,
    IdRealName realName,
    IdEmail email,
    OffsetDateTime created,
    IdPassword password)
    throws IdDatabaseException;

  /**
   * @param id The user ID
   *
   * @return A user with the given ID
   *
   * @throws IdDatabaseException On errors
   */

  Optional<IdUser> userGet(UUID id)
    throws IdDatabaseException;

  /**
   * @param id The user ID
   *
   * @return A user with the given ID
   *
   * @throws IdDatabaseException On errors
   */

  IdUser userGetRequire(UUID id)
    throws IdDatabaseException;

  /**
   * @param name The ID name
   *
   * @return A user with the given name
   *
   * @throws IdDatabaseException On errors
   */

  Optional<IdUser> userGetForName(IdName name)
    throws IdDatabaseException;

  /**
   * @param name The ID name
   *
   * @return A user with the given name
   *
   * @throws IdDatabaseException On errors
   */

  IdUser userGetForNameRequire(IdName name)
    throws IdDatabaseException;

  /**
   * @param email The user email
   *
   * @return A user with the given email
   *
   * @throws IdDatabaseException On errors
   */

  Optional<IdUser> userGetForEmail(IdEmail email)
    throws IdDatabaseException;

  /**
   * @param email The user email
   *
   * @return A user with the given email
   *
   * @throws IdDatabaseException On errors
   */

  IdUser userGetForEmailRequire(IdEmail email)
    throws IdDatabaseException;

  /**
   * Record the fact that the given user has logged in.
   *
   * @param id           The user ID
   * @param metadata     The optional metadata included with the request
   * @param limitHistory The limit on the number of login records that will be
   *                     kept
   *
   * @throws IdDatabaseException On errors
   */

  void userLogin(
    UUID id,
    Map<String, String> metadata,
    int limitHistory)
    throws IdDatabaseException;

  /**
   * Update the given user.
   *
   * @param id           The user ID
   * @param withIdName   The new ID name, if desired
   * @param withRealName The new real name, if desired
   * @param withPassword The new password, if desired
   *
   * @throws IdDatabaseException On errors
   */

  @IdDatabaseRequiresUser
  void userUpdate(
    UUID id,
    Optional<IdName> withIdName,
    Optional<IdRealName> withRealName,
    Optional<IdPassword> withPassword)
    throws IdDatabaseException;

  /**
   * Update the given user as an admin.
   *
   * @param id           The user ID
   * @param withIdName   The new ID name, if desired
   * @param withRealName The new real name, if desired
   * @param withPassword The new password, if desired
   *
   * @throws IdDatabaseException On errors
   */

  @IdDatabaseRequiresAdmin
  void userUpdateAsAdmin(
    UUID id,
    Optional<IdName> withIdName,
    Optional<IdRealName> withRealName,
    Optional<IdPassword> withPassword)
    throws IdDatabaseException;

  /**
   * List users.
   *
   * @param parameters The search parameters
   * @param seek       The record to which to seek, if any
   *
   * @return The users
   *
   * @throws IdDatabaseException On errors
   */

  List<IdUserSummary> userSearch(
    IdUserSearchParameters parameters,
    Optional<List<Object>> seek)
    throws IdDatabaseException;

  /**
   * Determine approximate number of results that would be returned in total by
   * a given search.
   *
   * @param parameters The search parameters
   *
   * @return The users
   *
   * @throws IdDatabaseException On errors
   */

  long userSearchCount(
    IdUserSearchParameters parameters)
    throws IdDatabaseException;

  /**
   * List users.
   *
   * @param parameters The search parameters
   * @param seek       The record to which to seek, if any
   *
   * @return The users
   *
   * @throws IdDatabaseException On errors
   */

  List<IdUserSummary> userSearchByEmail(
    IdUserSearchByEmailParameters parameters,
    Optional<List<Object>> seek)
    throws IdDatabaseException;

  /**
   * Determine approximate number of results that would be returned in total by
   * a given search.
   *
   * @param parameters The search parameters
   *
   * @return The users
   *
   * @throws IdDatabaseException On errors
   */

  long userSearchByEmailCount(
    IdUserSearchByEmailParameters parameters)
    throws IdDatabaseException;

  /**
   * Add an email address to the given user.
   *
   * @param id    The user ID
   * @param email The new email
   *
   * @throws IdDatabaseException On errors
   */

  void userEmailAdd(
    UUID id,
    IdEmail email)
    throws IdDatabaseException;

  /**
   * Remove an email address from the given user.
   *
   * @param id    The user ID
   * @param email The email
   *
   * @throws IdDatabaseException On errors
   */

  void userEmailRemove(
    UUID id,
    IdEmail email)
    throws IdDatabaseException;

  /**
   * Obtain the login history of the given user.
   *
   * @param id    The user ID
   * @param limit The limit on the number of returned items
   *
   * @return The login history
   *
   * @throws IdDatabaseException On errors
   */

  List<IdLogin> userLoginHistory(
    UUID id,
    int limit)
    throws IdDatabaseException;

  /**
   * Delete the given user.
   *
   * @param id The user ID
   *
   * @throws IdDatabaseException On errors
   */

  @IdDatabaseRequiresAdmin
  void userDelete(
    UUID id)
    throws IdDatabaseException;

  /**
   * Create a ban on the given user account.
   *
   * @param ban The ban
   *
   * @throws IdDatabaseException On errors
   */

  @IdDatabaseRequiresAdmin
  void userBanCreate(IdBan ban)
    throws IdDatabaseException;

  /**
   * Get the ban for the given user, if one exists.
   *
   * @param id The user ID
   *
   * @return The ban, if any
   *
   * @throws IdDatabaseException On errors
   */

  Optional<IdBan> userBanGet(UUID id)
    throws IdDatabaseException;

  /**
   * Delete the given ban.
   *
   * @param ban The ban
   *
   * @throws IdDatabaseException On errors
   */

  @IdDatabaseRequiresAdmin
  void userBanDelete(IdBan ban)
    throws IdDatabaseException;

  /**
   * Create a password reset request.
   *
   * @param reset The request
   *
   * @throws IdDatabaseException On errors
   */

  void userPasswordResetCreate(
    IdUserPasswordReset reset)
    throws IdDatabaseException;

  /**
   * List the password reset requests for the given user.
   *
   * @param user The user
   *
   * @return A list of requests, if any
   *
   * @throws IdDatabaseException On errors
   */

  List<IdUserPasswordReset> userPasswordResetGet(
    UUID user)
    throws IdDatabaseException;

  /**
   * List the password reset request for the given token.
   *
   * @param token The token
   *
   * @return A request, if any
   *
   * @throws IdDatabaseException On errors
   */

  Optional<IdUserPasswordReset> userPasswordResetGetForToken(
    IdToken token)
    throws IdDatabaseException;

  /**
   * Delete a password reset request.
   *
   * @param reset The request
   *
   * @throws IdDatabaseException On errors
   */

  void userPasswordResetDelete(
    IdUserPasswordReset reset)
    throws IdDatabaseException;
}

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


package com.io7m.idstore.admin_client.api;

import com.io7m.idstore.model.IdBan;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdLogin;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPage;
import com.io7m.idstore.model.IdPassword;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.model.IdUserSearchByEmailParameters;
import com.io7m.idstore.model.IdUserSearchParameters;
import com.io7m.idstore.model.IdUserSummary;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Commands related to users.
 */

public interface IdAClientUsersType
{
  /**
   * Start searching/listing users. Calling this method will set the search
   * parameters and effectively reset searching back to page 1 of any results.
   *
   * @param parameters The search parameters
   *
   * @return The first page of results
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  IdPage<IdUserSummary> userSearchBegin(
    IdUserSearchParameters parameters)
    throws IdAClientException, InterruptedException;

  /**
   * Continue searching/listing users. This will return the next page of
   * results.
   *
   * @return The next page of results
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  IdPage<IdUserSummary> userSearchNext()
    throws IdAClientException, InterruptedException;

  /**
   * Continue searching/listing users. This will return the previous page of
   * results.
   *
   * @return The previous page of results
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  IdPage<IdUserSummary> userSearchPrevious()
    throws IdAClientException, InterruptedException;

  /**
   * Start searching/listing users. Calling this method will set the search
   * parameters and effectively reset searching back to page 1 of any results.
   *
   * @param parameters The search parameters
   *
   * @return The first page of results
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  IdPage<IdUserSummary> userSearchByEmailBegin(
    IdUserSearchByEmailParameters parameters)
    throws IdAClientException, InterruptedException;

  /**
   * Continue searching/listing users. This will return the next page of
   * results.
   *
   * @return The next page of results
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  IdPage<IdUserSummary> userSearchByEmailNext()
    throws IdAClientException, InterruptedException;

  /**
   * Continue searching/listing users. This will return the previous page of
   * results.
   *
   * @return The previous page of results
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  IdPage<IdUserSummary> userSearchByEmailPrevious()
    throws IdAClientException, InterruptedException;

  /**
   * Fetch the given user.
   *
   * @param id The user ID
   *
   * @return The user, if one exists
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  Optional<IdUser> userGet(UUID id)
    throws IdAClientException, InterruptedException;

  /**
   * Fetch the given user.
   *
   * @param email The user email
   *
   * @return The user, if one exists
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  Optional<IdUser> userGetByEmail(IdEmail email)
    throws IdAClientException, InterruptedException;

  /**
   * Update the given user.
   *
   * @param user    The user
   * @param idName   The new idname
   * @param realName The new realname
   * @param password The new password
   *
   * @return The updated user
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  IdUser userUpdate(
    UUID user,
    Optional<IdName> idName,
    Optional<IdRealName> realName,
    Optional<IdPassword> password)
    throws IdAClientException, InterruptedException;

  /**
   * Add an email to the given user.
   *
   * @param id    The user ID
   * @param email The email address
   *
   * @return The updated user
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  IdUser userEmailAdd(
    UUID id,
    IdEmail email)
    throws IdAClientException, InterruptedException;

  /**
   * Remove an email from the given user.
   *
   * @param id    The user ID
   * @param email The email address
   *
   * @return The updated user
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  IdUser userEmailRemove(
    UUID id,
    IdEmail email)
    throws IdAClientException, InterruptedException;
  
  /**
   * Create a user.
   *
   * @param id       The ID
   * @param password The password
   * @param realName The real name
   * @param email    The email
   * @param idName   The ID name
   *
   * @return The created user
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  IdUser userCreate(
    Optional<UUID> id,
    IdName idName,
    IdRealName realName,
    IdEmail email,
    IdPassword password)
    throws IdAClientException, InterruptedException;

  /**
   * Delete the given user.
   *
   * @param id The user ID
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  void userDelete(UUID id)
    throws IdAClientException, InterruptedException;

  /**
   * Create a ban on the given user account.
   *
   * @param ban The ban
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  void userBanCreate(IdBan ban)
    throws IdAClientException, InterruptedException;

  /**
   * Get the ban for the given user, if one exists.
   *
   * @param id The user ID
   *
   * @return The ban, if any
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  Optional<IdBan> userBanGet(UUID id)
    throws IdAClientException, InterruptedException;

  /**
   * Delete the given ban.
   *
   * @param ban The ban
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  void userBanDelete(IdBan ban)
    throws IdAClientException, InterruptedException;

  /**
   * List the login history for the given user.
   *
   * @param id The user ID
   *
   * @return The login history
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  List<IdLogin> userLoginHistory(
    UUID id)
    throws IdAClientException, InterruptedException;
}

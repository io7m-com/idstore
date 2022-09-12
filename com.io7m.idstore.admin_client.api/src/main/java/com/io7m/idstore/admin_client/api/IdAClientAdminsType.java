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

import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdAdminPermission;
import com.io7m.idstore.model.IdAdminSearchByEmailParameters;
import com.io7m.idstore.model.IdAdminSearchParameters;
import com.io7m.idstore.model.IdAdminSummary;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPage;
import com.io7m.idstore.model.IdPassword;
import com.io7m.idstore.model.IdRealName;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Commands related to admins.
 */

public interface IdAClientAdminsType
{
  /**
   * Retrieve the current admin profile.
   *
   * @return The current user
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  IdAdmin adminSelf()
    throws IdAClientException, InterruptedException;

  /**
   * Start searching/listing admins. Calling this method will set the search
   * parameters and effectively reset searching back to page 1 of any results.
   *
   * @param parameters The search parameters
   *
   * @return The first page of results
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  IdPage<IdAdminSummary> adminSearchBegin(
    IdAdminSearchParameters parameters)
    throws IdAClientException, InterruptedException;

  /**
   * Continue searching/listing admins. This will return the next page of
   * results.
   *
   * @return The next page of results
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  IdPage<IdAdminSummary> adminSearchNext()
    throws IdAClientException, InterruptedException;

  /**
   * Continue searching/listing admins. This will return the previous page of
   * results.
   *
   * @return The previous page of results
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  IdPage<IdAdminSummary> adminSearchPrevious()
    throws IdAClientException, InterruptedException;

  /**
   * Start searching/listing admins. Calling this method will set the search
   * parameters and effectively reset searching back to page 1 of any results.
   *
   * @param parameters The search parameters
   *
   * @return The first page of results
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  IdPage<IdAdminSummary> adminSearchByEmailBegin(
    IdAdminSearchByEmailParameters parameters)
    throws IdAClientException, InterruptedException;

  /**
   * Continue searching/listing admins. This will return the next page of
   * results.
   *
   * @return The next page of results
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  IdPage<IdAdminSummary> adminSearchByEmailNext()
    throws IdAClientException, InterruptedException;

  /**
   * Continue searching/listing admins. This will return the previous page of
   * results.
   *
   * @return The previous page of results
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  IdPage<IdAdminSummary> adminSearchByEmailPrevious()
    throws IdAClientException, InterruptedException;

  /**
   * Fetch the given admin.
   *
   * @param id The admin ID
   *
   * @return The admin, if one exists
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  Optional<IdAdmin> adminGet(UUID id)
    throws IdAClientException, InterruptedException;

  /**
   * Fetch the given admin.
   *
   * @param email The admin email
   *
   * @return The admin, if one exists
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  Optional<IdAdmin> adminGetByEmail(IdEmail email)
    throws IdAClientException, InterruptedException;

  /**
   * Update the given admin.
   *
   * @param admin The admin
   *
   * @return The updated admin
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  IdAdmin adminUpdate(IdAdmin admin)
    throws IdAClientException, InterruptedException;

  /**
   * Create a admin.
   *
   * @param id          The ID
   * @param password    The password
   * @param realName    The real name
   * @param email       The email
   * @param idName      The ID name
   * @param permissions The permissions
   *
   * @return The created admin
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  IdAdmin adminCreate(
    Optional<UUID> id,
    IdName idName,
    IdRealName realName,
    IdEmail email,
    IdPassword password,
    Set<IdAdminPermission> permissions)
    throws IdAClientException, InterruptedException;

}

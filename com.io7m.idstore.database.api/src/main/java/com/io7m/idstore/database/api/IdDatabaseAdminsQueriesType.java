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

import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdAdminPermission;
import com.io7m.idstore.model.IdAdminSearchByEmailParameters;
import com.io7m.idstore.model.IdAdminSearchParameters;
import com.io7m.idstore.model.IdAdminSummary;
import com.io7m.idstore.model.IdBan;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPassword;
import com.io7m.idstore.model.IdRealName;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The database queries involving admins.
 */

public non-sealed interface IdDatabaseAdminsQueriesType
  extends IdDatabaseQueriesType
{
  /**
   * Create an (initial) admin. This method behaves exactly the same as
   * {@link #adminCreate(IdName, IdRealName, IdEmail, IdPassword, Set)}  except
   * that it does not require an existing admin account and does not write to
   * the audit log. The method will fail if any admin account already exists,
   *
   * @param id       The admin ID
   * @param created  The creation time
   * @param idName   The admin ID name
   * @param realName The admin real name
   * @param email    The admin email
   * @param password The hashed password
   *
   * @return The created admin
   *
   * @throws IdDatabaseException On errors
   */

  IdAdmin adminCreateInitial(
    UUID id,
    IdName idName,
    IdRealName realName,
    IdEmail email,
    OffsetDateTime created,
    IdPassword password)
    throws IdDatabaseException;

  /**
   * Create an admin.
   *
   * @param idName      The admin ID name
   * @param realName    The admin real name
   * @param email       The admin email
   * @param password    The hashed password
   * @param permissions The permissions the created admin will have
   *
   * @return The created admin
   *
   * @throws IdDatabaseException On errors
   */

  @IdDatabaseRequiresAdmin
  default IdAdmin adminCreate(
    final IdName idName,
    final IdRealName realName,
    final IdEmail email,
    final IdPassword password,
    final Set<IdAdminPermission> permissions)
    throws IdDatabaseException
  {
    return this.adminCreate(
      UUID.randomUUID(),
      idName,
      realName,
      email,
      OffsetDateTime.now(),
      password,
      permissions
    );
  }

  /**
   * Create an admin.
   *
   * @param id          The admin ID
   * @param created     The creation time
   * @param idName      The admin ID name
   * @param realName    The admin real name
   * @param email       The admin email
   * @param password    The hashed password
   * @param permissions The permissions the created admin will have
   *
   * @return The created admin
   *
   * @throws IdDatabaseException On errors
   */

  @IdDatabaseRequiresAdmin
  IdAdmin adminCreate(
    UUID id,
    IdName idName,
    IdRealName realName,
    IdEmail email,
    OffsetDateTime created,
    IdPassword password,
    Set<IdAdminPermission> permissions)
    throws IdDatabaseException;

  /**
   * @param id The admin ID
   *
   * @return A admin with the given ID
   *
   * @throws IdDatabaseException On errors
   */

  Optional<IdAdmin> adminGet(UUID id)
    throws IdDatabaseException;

  /**
   * @param name The admin name
   *
   * @return A admin with the given name
   *
   * @throws IdDatabaseException On errors
   */

  Optional<IdAdmin> adminGetForName(IdName name)
    throws IdDatabaseException;

  /**
   * @param name The admin name
   *
   * @return A admin with the given name
   *
   * @throws IdDatabaseException On errors
   */

  IdAdmin adminGetForNameRequire(IdName name)
    throws IdDatabaseException;

  /**
   * @param email The admin email
   *
   * @return A admin with the given email
   *
   * @throws IdDatabaseException On errors
   */

  Optional<IdAdmin> adminGetForEmail(IdEmail email)
    throws IdDatabaseException;

  /**
   * Record the fact that the given admin has logged in.
   *
   * @param id        The admin ID
   * @param userAgent The user agent
   * @param host      The host from which the admin logged in
   *
   * @throws IdDatabaseException On errors
   */

  void adminLogin(
    UUID id,
    String userAgent,
    String host)
    throws IdDatabaseException;

  /**
   * List admins.
   *
   * @param parameters The search parameters
   * @param seek       The record to which to seek, if any
   *
   * @return The admins
   *
   * @throws IdDatabaseException On errors
   */

  List<IdAdminSummary> adminSearch(
    IdAdminSearchParameters parameters,
    Optional<List<Object>> seek)
    throws IdDatabaseException;

  /**
   * Determine approximate number of results that would be returned in total by
   * a given search.
   *
   * @param parameters The search parameters
   *
   * @return The admins
   *
   * @throws IdDatabaseException On errors
   */

  long adminSearchCount(
    IdAdminSearchParameters parameters)
    throws IdDatabaseException;

  /**
   * List admins.
   *
   * @param parameters The search parameters
   * @param seek       The record to which to seek, if any
   *
   * @return The admins
   *
   * @throws IdDatabaseException On errors
   */

  List<IdAdminSummary> adminSearchByEmail(
    IdAdminSearchByEmailParameters parameters,
    Optional<List<Object>> seek)
    throws IdDatabaseException;

  /**
   * Determine approximate number of results that would be returned in total by
   * a given search.
   *
   * @param parameters The search parameters
   *
   * @return The admins
   *
   * @throws IdDatabaseException On errors
   */

  long adminSearchByEmailCount(
    IdAdminSearchByEmailParameters parameters)
    throws IdDatabaseException;

  /**
   * @param id The admin ID
   *
   * @return A admin with the given ID
   *
   * @throws IdDatabaseException On errors
   */

  IdAdmin adminGetRequire(UUID id)
    throws IdDatabaseException;

  /**
   * Add an email address to the given admin.
   *
   * @param id    The admin ID
   * @param email The new email
   *
   * @throws IdDatabaseException On errors
   */

  void adminEmailAdd(
    UUID id,
    IdEmail email)
    throws IdDatabaseException;

  /**
   * Remove an email address from the given admin.
   *
   * @param id    The admin ID
   * @param email The email
   *
   * @throws IdDatabaseException On errors
   */

  void adminEmailRemove(
    UUID id,
    IdEmail email)
    throws IdDatabaseException;

  /**
   * Update the given admin.
   *
   * @param id              The admin ID
   * @param withIdName      The new ID name, if desired
   * @param withRealName    The new real name, if desired
   * @param withPassword    The new password, if desired
   * @param withPermissions The new permissions, if desired
   *
   * @throws IdDatabaseException On errors
   */

  @IdDatabaseRequiresAdmin
  void adminUpdate(
    UUID id,
    Optional<IdName> withIdName,
    Optional<IdRealName> withRealName,
    Optional<IdPassword> withPassword,
    Optional<Set<IdAdminPermission>> withPermissions)
    throws IdDatabaseException;

  /**
   * Delete the given admin.
   *
   * @param id The admin ID
   *
   * @throws IdDatabaseException On errors
   */

  @IdDatabaseRequiresAdmin
  void adminDelete(
    UUID id)
    throws IdDatabaseException;

  /**
   * Create a ban on the given admin account.
   *
   * @param ban The ban
   *
   * @throws IdDatabaseException On errors
   */

  @IdDatabaseRequiresAdmin
  void adminBanCreate(IdBan ban)
    throws IdDatabaseException;

  /**
   * Get the ban for the given admin, if one exists.
   *
   * @param id The admin ID
   *
   * @return The ban, if any
   *
   * @throws IdDatabaseException On errors
   */

  Optional<IdBan> adminBanGet(UUID id)
    throws IdDatabaseException;

  /**
   * Delete the given ban.
   *
   * @param ban The ban
   *
   * @throws IdDatabaseException On errors
   */

  @IdDatabaseRequiresAdmin
  void adminBanDelete(IdBan ban)
    throws IdDatabaseException;
}

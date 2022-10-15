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

package com.io7m.idstore.server.internal;

import com.io7m.idstore.database.api.IdDatabaseAdminSearchByEmailType;
import com.io7m.idstore.database.api.IdDatabaseAdminSearchType;
import com.io7m.idstore.database.api.IdDatabaseAuditEventsSearchType;
import com.io7m.idstore.database.api.IdDatabaseUserSearchByEmailType;
import com.io7m.idstore.database.api.IdDatabaseUserSearchType;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A controller for a single admin session.
 */

public final class IdAdminSession
{
  private final UUID adminId;
  private final String sessionId;
  private Optional<IdDatabaseAuditEventsSearchType> auditPaging;
  private Optional<IdDatabaseAdminSearchType> adminSearch;
  private Optional<IdDatabaseAdminSearchByEmailType> adminSearchByEmail;
  private Optional<IdDatabaseUserSearchType> userSearch;
  private Optional<IdDatabaseUserSearchByEmailType> userSearchByEmail;

  /**
   * A controller for a single admin session.
   *
   * @param inUserId    The admin ID
   * @param inSessionId The session ID
   */

  public IdAdminSession(
    final UUID inUserId,
    final String inSessionId)
  {
    this.adminId =
      Objects.requireNonNull(inUserId, "userId");
    this.sessionId =
      Objects.requireNonNull(inSessionId, "sessionId");

    this.auditPaging =
      Optional.empty();
    this.adminSearch =
      Optional.empty();
    this.adminSearchByEmail =
      Optional.empty();
    this.userSearch =
      Optional.empty();
    this.userSearchByEmail =
      Optional.empty();
  }

  /**
   * @return The audit record search
   */

  public Optional<IdDatabaseAuditEventsSearchType> auditSearch()
  {
    return this.auditPaging;
  }

  /**
   * Set the new audit record search.
   *
   * @param search The audit record search
   */

  public void setAuditSearch(
    final IdDatabaseAuditEventsSearchType search)
  {
    this.auditPaging =
      Optional.of(Objects.requireNonNull(search, "search"));
  }

  /**
   * @return The admin by email search
   */

  public Optional<IdDatabaseAdminSearchByEmailType> adminSearchByEmail()
  {
    return this.adminSearchByEmail;
  }

  /**
   * Set the new admin by email search.
   *
   * @param search The admin by email search
   */

  public void setAdminSearchByEmail(
    final IdDatabaseAdminSearchByEmailType search)
  {
    this.adminSearchByEmail =
      Optional.of(Objects.requireNonNull(search, "search"));
  }

  /**
   * @return The admin search
   */

  public Optional<IdDatabaseAdminSearchType> adminSearch()
  {
    return this.adminSearch;
  }

  /**
   * Set the new admin search.
   *
   * @param search The admin search
   */

  public void setAdminSearch(
    final IdDatabaseAdminSearchType search)
  {
    this.adminSearch =
      Optional.of(Objects.requireNonNull(search, "search"));
  }

  /**
   * @return The user by email search
   */

  public Optional<IdDatabaseUserSearchByEmailType> userSearchByEmail()
  {
    return this.userSearchByEmail;
  }

  /**
   * Set the new user by email search.
   *
   * @param search The user by email search
   */

  public void setUserSearchByEmail(
    final IdDatabaseUserSearchByEmailType search)
  {
    this.userSearchByEmail =
      Optional.of(Objects.requireNonNull(search, "search"));
  }

  /**
   * @return The user search
   */

  public Optional<IdDatabaseUserSearchType> userSearch()
  {
    return this.userSearch;
  }

  /**
   * Set the new user search.
   *
   * @param search The user search
   */

  public void setUserSearch(
    final IdDatabaseUserSearchType search)
  {
    this.userSearch =
      Optional.of(Objects.requireNonNull(search, "search"));
  }
}

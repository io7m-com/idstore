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

import com.io7m.idstore.database.api.IdDatabaseAdminSearchByEmailPaging;
import com.io7m.idstore.database.api.IdDatabaseAdminSearchByEmailPagingType;
import com.io7m.idstore.database.api.IdDatabaseAdminSearchPaging;
import com.io7m.idstore.database.api.IdDatabaseAdminSearchPagingType;
import com.io7m.idstore.database.api.IdDatabaseAuditListPaging;
import com.io7m.idstore.database.api.IdDatabaseAuditListPagingType;
import com.io7m.idstore.database.api.IdDatabaseUserSearchByEmailPaging;
import com.io7m.idstore.database.api.IdDatabaseUserSearchByEmailPagingType;
import com.io7m.idstore.database.api.IdDatabaseUserSearchPaging;
import com.io7m.idstore.database.api.IdDatabaseUserSearchPagingType;
import com.io7m.idstore.model.IdAdminSearchByEmailParameters;
import com.io7m.idstore.model.IdAdminSearchParameters;
import com.io7m.idstore.model.IdAuditListParameters;
import com.io7m.idstore.model.IdUserSearchByEmailParameters;
import com.io7m.idstore.model.IdUserSearchParameters;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A controller for a single user session.
 */

public final class IdUserSession
{
  private final UUID userId;
  private final String sessionId;
  private IdUserSearchParameters userSearchParameters;
  private IdDatabaseUserSearchPagingType userSearchPaging;
  private IdUserSearchByEmailParameters userSearchByEmailParameters;
  private IdDatabaseUserSearchByEmailPagingType userSearchByEmailPaging;
  private Optional<IdSessionMessage> message;
  private IdAuditListParameters auditListParameters;
  private IdDatabaseAuditListPagingType auditPaging;
  private IdAdminSearchParameters adminSearchParameters;
  private IdDatabaseAdminSearchPagingType adminSearchPaging;
  private IdAdminSearchByEmailParameters adminSearchByEmailParameters;
  private IdDatabaseAdminSearchByEmailPagingType adminSearchByEmailPaging;

  /**
   * A controller for a single user session.
   *
   * @param inUserId    The user ID
   * @param inSessionId The session ID
   */

  public IdUserSession(
    final UUID inUserId,
    final String inSessionId)
  {
    this.userId =
      Objects.requireNonNull(inUserId, "userId");
    this.sessionId =
      Objects.requireNonNull(inSessionId, "sessionId");

    this.userSearchParameters =
      IdUserSearchParameters.defaults();
    this.userSearchPaging =
      IdDatabaseUserSearchPaging.create(this.userSearchParameters);

    this.userSearchByEmailParameters =
      IdUserSearchByEmailParameters.defaults();
    this.userSearchByEmailPaging =
      IdDatabaseUserSearchByEmailPaging.create(this.userSearchByEmailParameters);

    this.auditListParameters =
      IdAuditListParameters.defaults();
    this.auditPaging =
      IdDatabaseAuditListPaging.create(this.auditListParameters);

    this.adminSearchParameters =
      IdAdminSearchParameters.defaults();
    this.adminSearchPaging =
      IdDatabaseAdminSearchPaging.create(this.adminSearchParameters);

    this.adminSearchByEmailParameters =
      IdAdminSearchByEmailParameters.defaults();
    this.adminSearchByEmailPaging =
      IdDatabaseAdminSearchByEmailPaging.create(this.adminSearchByEmailParameters);

    this.message =
      Optional.empty();
  }

  /**
   * @return The most recent user paging handler
   */

  public IdDatabaseUserSearchPagingType userPaging()
  {
    return this.userSearchPaging;
  }

  /**
   * @return The most recent user paging handler
   */

  public IdDatabaseUserSearchByEmailPagingType userByEmailPaging()
  {
    return this.userSearchByEmailPaging;
  }

  /**
   * Set the user listing parameters.
   *
   * @param userParameters The user parameters
   */

  public void setUserSearchParameters(
    final IdUserSearchParameters userParameters)
  {
    this.userSearchParameters =
      Objects.requireNonNull(userParameters, "userParameters");

    if (!Objects.equals(
      this.userSearchPaging.pageParameters(),
      userParameters)) {
      this.userSearchPaging =
        IdDatabaseUserSearchPaging.create(userParameters);
    }
  }

  /**
   * Set the user listing parameters.
   *
   * @param userParameters The user parameters
   */

  public void setUserSearchByEmailParameters(
    final IdUserSearchByEmailParameters userParameters)
  {
    this.userSearchByEmailParameters =
      Objects.requireNonNull(userParameters, "userParameters");

    if (!Objects.equals(
      this.userSearchByEmailPaging.pageParameters(),
      userParameters)) {
      this.userSearchByEmailPaging =
        IdDatabaseUserSearchByEmailPaging.create(userParameters);
    }
  }

  /**
   * @return The current message
   */

  public Optional<IdSessionMessage> messageCurrent()
  {
    return this.message;
  }

  /**
   * Discard the current message.
   */

  public void messageDiscard()
  {
    this.message = Optional.empty();
  }

  /**
   * Set the current message.
   *
   * @param inMessage The message
   */

  public void messageCurrentSet(
    final IdSessionMessage inMessage)
  {
    this.message = Optional.of(
      Objects.requireNonNull(inMessage, "message")
    );
  }

  /**
   * Set the audit listing parameters.
   *
   * @param auditParameters The audit parameters
   */

  public void setAuditListParameters(
    final IdAuditListParameters auditParameters)
  {
    this.auditListParameters =
      Objects.requireNonNull(auditParameters, "auditParameters");

    if (!Objects.equals(this.auditPaging.pageParameters(), auditParameters)) {
      this.auditPaging =
        IdDatabaseAuditListPaging.create(auditParameters);
    }
  }

  /**
   * @return The most recent audit list parameters
   */

  public IdAuditListParameters auditListParameters()
  {
    return this.auditListParameters;
  }

  /**
   * @return The most recent audit paging handler
   */

  public IdDatabaseAuditListPagingType auditPaging()
  {
    return this.auditPaging;
  }


  /**
   * @return The most recent admin paging handler
   */

  public IdDatabaseAdminSearchPagingType adminPaging()
  {
    return this.adminSearchPaging;
  }

  /**
   * @return The most recent admin paging handler
   */

  public IdDatabaseAdminSearchByEmailPagingType adminByEmailPaging()
  {
    return this.adminSearchByEmailPaging;
  }

  /**
   * Set the admin listing parameters.
   *
   * @param adminParameters The admin parameters
   */

  public void setAdminSearchParameters(
    final IdAdminSearchParameters adminParameters)
  {
    this.adminSearchParameters =
      Objects.requireNonNull(adminParameters, "adminParameters");

    if (!Objects.equals(
      this.adminSearchPaging.pageParameters(),
      adminParameters)) {
      this.adminSearchPaging =
        IdDatabaseAdminSearchPaging.create(adminParameters);
    }
  }

  /**
   * Set the admin listing parameters.
   *
   * @param adminParameters The admin parameters
   */

  public void setAdminSearchByEmailParameters(
    final IdAdminSearchByEmailParameters adminParameters)
  {
    this.adminSearchByEmailParameters =
      Objects.requireNonNull(adminParameters, "adminParameters");

    if (!Objects.equals(
      this.adminSearchByEmailPaging.pageParameters(),
      adminParameters)) {
      this.adminSearchByEmailPaging =
        IdDatabaseAdminSearchByEmailPaging.create(adminParameters);
    }
  }
}

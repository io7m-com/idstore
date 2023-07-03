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

package com.io7m.idstore.shell.admin.internal.formatting;

import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdAdminSummary;
import com.io7m.idstore.model.IdAuditEvent;
import com.io7m.idstore.model.IdLogin;
import com.io7m.idstore.model.IdPage;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.model.IdUserSummary;

import java.util.List;

/**
 * A shell formatter for data.
 */

public interface IdAFormatterType
{
  /**
   * Format an admin.
   *
   * @param admin The admin
   *
   * @throws Exception On errors
   */

  void formatAdmin(
    IdAdmin admin)
    throws Exception;

  /**
   * Format a page of admins.
   *
   * @param page The page
   *
   * @throws Exception On errors
   */

  void formatAdmins(
    IdPage<IdAdminSummary> page)
    throws Exception;

  /**
   * Format a page of audit events.
   *
   * @param page The page
   *
   * @throws Exception On errors
   */

  void formatAudits(
    IdPage<IdAuditEvent> page)
    throws Exception;

  /**
   * Format a page of users.
   *
   * @param page The page
   *
   * @throws Exception On errors
   */

  void formatUsers(
    IdPage<IdUserSummary> page)
    throws Exception;

  /**
   * Format a history.
   *
   * @param history The history
   *
   * @throws Exception On errors
   */

  void formatLoginHistory(
    List<IdLogin> history)
    throws Exception;

  /**
   * Format a user.
   *
   * @param user The user
   *
   * @throws Exception On errors
   */

  void formatUser(
    IdUser user)
    throws Exception;
}

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

package com.io7m.idstore.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Information for a single administrator.
 *
 * @param id          The admin's ID
 * @param idName      The admin's id name
 * @param realName    The admin's real name
 * @param emails      The admin's emails
 * @param password    The admin's password
 * @param timeCreated The date the admin was created
 * @param timeUpdated The date the admin was last updated
 * @param permissions The set of permissions belonging to the admin
 */

public record IdAdmin(
  UUID id,
  IdName idName,
  IdRealName realName,
  IdNonEmptyList<IdEmail> emails,
  OffsetDateTime timeCreated,
  OffsetDateTime timeUpdated,
  IdPassword password,
  IdAdminPermissionSet permissions)
{
  /**
   * Information for a single administrator.
   *
   * @param id          The admin's ID
   * @param idName      The admin's id name
   * @param realName    The admin's real name
   * @param emails      The admin's emails
   * @param password    The admin's password
   * @param timeCreated The date the admin was created
   * @param timeUpdated The date the admin was last updated
   * @param permissions The set of permissions belonging to the admin
   */

  public IdAdmin
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(idName, "idName");
    Objects.requireNonNull(realName, "realName");
    Objects.requireNonNull(emails, "emails");
    Objects.requireNonNull(timeCreated, "timeCreated");
    Objects.requireNonNull(timeUpdated, "timeUpdated");
    Objects.requireNonNull(password, "password");
    Objects.requireNonNull(permissions, "permissions");
  }
}

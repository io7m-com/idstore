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

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Information required to create a admin.
 *
 * @param id          The admin ID (if an explicit ID is desired)
 * @param idName      The id name
 * @param realName    The real name
 * @param email       The email
 * @param password    The password
 * @param permissions The permissions
 */

public record IdAdminCreate(
  Optional<UUID> id,
  IdName idName,
  IdRealName realName,
  IdEmail email,
  IdPassword password,
  IdAdminPermissionSet permissions)
{
  /**
   * Information required to create a admin.
   *
   * @param id          The admin ID (if an explicit ID is desired)
   * @param idName      The id name
   * @param realName    The real name
   * @param email       The email
   * @param password    The password
   * @param permissions The permissions
   */

  public IdAdminCreate
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(idName, "idName");
    Objects.requireNonNull(realName, "realName");
    Objects.requireNonNull(email, "email");
    Objects.requireNonNull(password, "password");
    Objects.requireNonNull(permissions, "permissions");
  }
}

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

package com.io7m.idstore.protocol.admin_v1;

import com.io7m.idstore.model.IdAdminPermission;
import com.io7m.idstore.protocol.api.IdProtocolFromModel;
import com.io7m.idstore.protocol.api.IdProtocolToModel;

import java.util.Objects;

/**
 * The type of admin permissions.
 */

public enum IdA1AdminPermission
{
  /**
   * A permission that allows for creating/editing admins.
   */

  ADMIN_CREATE,

  /**
   * A permission that allows reading admins.
   */

  ADMIN_READ,

  /**
   * A permission that allows reading the audit log.
   */

  AUDIT_READ,

  /**
   * A permission that allows creating/editing users.
   */

  USER_WRITE,

  /**
   * A permission that allows reading users.
   */

  USER_READ;

  /**
   * Create a v1 permission from the given model permission.
   *
   * @param permission The model permission
   *
   * @return A v1 permission
   *
   * @see #toPermission()
   */

  @IdProtocolFromModel
  public static IdA1AdminPermission ofPermission(
    final IdAdminPermission permission)
  {
    Objects.requireNonNull(permission, "permission");

    return switch (permission) {
      case ADMIN_CREATE -> ADMIN_CREATE;
      case ADMIN_READ -> ADMIN_READ;
      case AUDIT_READ -> AUDIT_READ;
      case USER_READ -> USER_READ;
      case USER_WRITE -> USER_WRITE;
    };
  }

  /**
   * Convert this to a model permission.
   *
   * @return This as a model permission
   *
   * @see #ofPermission(IdAdminPermission)
   */

  @IdProtocolToModel
  public IdAdminPermission toPermission()
  {
    return switch (this) {
      case ADMIN_CREATE -> IdAdminPermission.ADMIN_CREATE;
      case ADMIN_READ -> IdAdminPermission.ADMIN_READ;
      case AUDIT_READ -> IdAdminPermission.AUDIT_READ;
      case USER_READ -> IdAdminPermission.USER_READ;
      case USER_WRITE -> IdAdminPermission.USER_WRITE;
    };
  }
}

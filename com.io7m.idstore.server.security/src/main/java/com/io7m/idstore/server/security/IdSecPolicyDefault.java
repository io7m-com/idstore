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

package com.io7m.idstore.server.security;

import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdAdminPermission;

import java.util.HashSet;
import java.util.Objects;

import static com.io7m.idstore.model.IdAdminPermission.ADMIN_CREATE;
import static com.io7m.idstore.model.IdAdminPermission.ADMIN_DELETE;
import static com.io7m.idstore.model.IdAdminPermission.ADMIN_READ;
import static com.io7m.idstore.model.IdAdminPermission.ADMIN_WRITE;
import static com.io7m.idstore.model.IdAdminPermission.ADMIN_WRITE_SELF;
import static com.io7m.idstore.model.IdAdminPermission.AUDIT_READ;
import static com.io7m.idstore.model.IdAdminPermission.USER_CREATE;
import static com.io7m.idstore.model.IdAdminPermission.USER_DELETE;
import static com.io7m.idstore.model.IdAdminPermission.USER_READ;
import static com.io7m.idstore.model.IdAdminPermission.USER_WRITE;

/**
 * The default security policy.
 */

public final class IdSecPolicyDefault implements IdSecPolicyType
{
  private static final IdSecPolicyDefault INSTANCE =
    new IdSecPolicyDefault();

  private IdSecPolicyDefault()
  {

  }

  /**
   * @return A reference to this policy
   */

  public static IdSecPolicyType get()
  {
    return INSTANCE;
  }

  private static IdSecPolicyResultType checkUserAction(
    final IdSecUserActionType action)
  {
    if (action instanceof IdSecUserActionEmailAddBegin e) {
      return checkUserActionEmailAddBegin(e);
    }
    if (action instanceof IdSecUserActionEmailAddPermit e) {
      return checkUserActionEmailAddPermit(e);
    }
    if (action instanceof IdSecUserActionEmailAddDeny e) {
      return checkUserActionEmailAddDeny(e);
    }
    if (action instanceof IdSecUserActionEmailRemoveBegin e) {
      return checkUserActionEmailRemoveBegin(e);
    }
    if (action instanceof IdSecUserActionEmailRemovePermit e) {
      return checkUserActionEmailRemovePermit(e);
    }
    if (action instanceof IdSecUserActionEmailRemoveDeny e) {
      return checkUserActionEmailRemoveDeny(e);
    }
    if (action instanceof IdSecUserActionRealnameUpdate e) {
      return checkUserActionRealnameUpdate(e);
    }

    return new IdSecPolicyResultDenied("Operation not permitted.");
  }

  private static IdSecPolicyResultType checkUserActionRealnameUpdate(
    final IdSecUserActionRealnameUpdate e)
  {
    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkUserActionEmailAddBegin(
    final IdSecUserActionEmailAddBegin e)
  {
    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkUserActionEmailAddPermit(
    final IdSecUserActionEmailAddPermit e)
  {
    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkUserActionEmailAddDeny(
    final IdSecUserActionEmailAddDeny e)
  {
    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkUserActionEmailRemoveBegin(
    final IdSecUserActionEmailRemoveBegin e)
  {
    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkUserActionEmailRemovePermit(
    final IdSecUserActionEmailRemovePermit e)
  {
    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkUserActionEmailRemoveDeny(
    final IdSecUserActionEmailRemoveDeny e)
  {
    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkAdminAction(
    final IdSecAdminActionType action)
  {
    if (action instanceof IdSecAdminActionUserRead e) {
      return checkAdminActionUserRead(e);
    }
    if (action instanceof IdSecAdminActionUserCreate e) {
      return checkAdminActionUserCreate(e);
    }
    if (action instanceof IdSecAdminActionUserUpdate e) {
      return checkAdminActionUserUpdate(e);
    }
    if (action instanceof IdSecAdminActionUserDelete e) {
      return checkAdminActionUserDelete(e);
    }

    if (action instanceof IdSecAdminActionAdminRead e) {
      return checkAdminActionAdminRead(e);
    }
    if (action instanceof IdSecAdminActionAdminCreate e) {
      return checkAdminActionAdminCreate(e);
    }
    if (action instanceof IdSecAdminActionAdminUpdate e) {
      return checkAdminActionAdminUpdate(e);
    }
    if (action instanceof IdSecAdminActionAdminDelete e) {
      return checkAdminActionAdminDelete(e);
    }

    if (action instanceof IdSecAdminActionAdminEmailAdd e) {
      return checkAdminActionAdminEmailAdd(e);
    }
    if (action instanceof IdSecAdminActionAdminEmailRemove e) {
      return checkAdminActionAdminEmailRemove(e);
    }
    if (action instanceof IdSecAdminActionAdminPermissionGrant e) {
      return checkAdminActionAdminPermissionGrant(e);
    }
    if (action instanceof IdSecAdminActionAdminPermissionRevoke e) {
      return checkAdminActionAdminPermissionRevoke(e);
    }

    if (action instanceof IdSecAdminActionAuditRead e) {
      return checkAdminActionAuditRead(e);
    }

    return new IdSecPolicyResultDenied("Operation not permitted.");
  }

  private static IdSecPolicyResultType checkAdminActionAdminPermissionRevoke(
    final IdSecAdminActionAdminPermissionRevoke e)
  {
    final var admin = e.admin();
    if (Objects.equals(admin.id(), e.targetAdmin())) {
      if (!admin.permissions().implies(ADMIN_WRITE_SELF)) {
        return new IdSecPolicyResultDenied(
          "Modifying admins requires the %s permission.".formatted(ADMIN_WRITE_SELF)
        );
      }
    } else {
      if (!admin.permissions().implies(ADMIN_WRITE)) {
        return new IdSecPolicyResultDenied(
          "Modifying admins requires the %s permission.".formatted(ADMIN_WRITE)
        );
      }
    }

    final var permission = e.permission();
    if (!admin.permissions().implies(permission)) {
      return new IdSecPolicyResultDenied(
        "The %s permission cannot be revoked by an admin that does not have it."
          .formatted(permission)
      );
    }

    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkAdminActionAdminPermissionGrant(
    final IdSecAdminActionAdminPermissionGrant e)
  {
    final var admin = e.admin();
    if (Objects.equals(admin.id(), e.targetAdmin())) {
      if (!admin.permissions().implies(ADMIN_WRITE_SELF)) {
        return new IdSecPolicyResultDenied(
          "Modifying admins requires the %s permission.".formatted(ADMIN_WRITE_SELF)
        );
      }
    } else {
      if (!admin.permissions().implies(ADMIN_WRITE)) {
        return new IdSecPolicyResultDenied(
          "Modifying admins requires the %s permission.".formatted(ADMIN_WRITE)
        );
      }
    }

    final var permission = e.permission();
    if (!admin.permissions().implies(permission)) {
      return new IdSecPolicyResultDenied(
        "The %s permission cannot be granted by an admin that does not have it."
          .formatted(permission)
      );
    }

    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkAdminActionAdminEmailRemove(
    final IdSecAdminActionAdminEmailRemove e)
  {
    final var admin = e.admin();
    if (Objects.equals(admin.id(), e.targetAdmin())) {
      if (!admin.permissions().implies(ADMIN_WRITE_SELF)) {
        return new IdSecPolicyResultDenied(
          "Modifying admins requires the %s permission.".formatted(ADMIN_WRITE_SELF)
        );
      }
    } else {
      if (!admin.permissions().implies(ADMIN_WRITE)) {
        return new IdSecPolicyResultDenied(
          "Modifying admins requires the %s permission.".formatted(ADMIN_WRITE)
        );
      }
    }

    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkAdminActionAdminEmailAdd(
    final IdSecAdminActionAdminEmailAdd e)
  {
    final var admin = e.admin();
    if (Objects.equals(admin.id(), e.targetAdmin())) {
      if (!admin.permissions().implies(ADMIN_WRITE_SELF)) {
        return new IdSecPolicyResultDenied(
          "Modifying admins requires the %s permission.".formatted(ADMIN_WRITE_SELF)
        );
      }
    } else {
      if (!admin.permissions().implies(ADMIN_WRITE)) {
        return new IdSecPolicyResultDenied(
          "Modifying admins requires the %s permission.".formatted(ADMIN_WRITE)
        );
      }
    }

    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkAdminActionAuditRead(
    final IdSecAdminActionAuditRead e)
  {
    final var permissions = e.admin().permissions();
    if (permissions.implies(AUDIT_READ)) {
      return new IdSecPolicyResultPermitted();
    }

    return new IdSecPolicyResultDenied(
      "Reading audit records requires the %s permission.".formatted(AUDIT_READ)
    );
  }

  private static IdSecPolicyResultType checkAdminActionUserRead(
    final IdSecAdminActionUserRead e)
  {
    final var permissions = e.admin().permissions();
    if (permissions.implies(USER_READ)) {
      return new IdSecPolicyResultPermitted();
    }

    return new IdSecPolicyResultDenied(
      "Reading users requires the %s permission.".formatted(USER_READ)
    );
  }

  private static IdSecPolicyResultType checkAdminActionUserDelete(
    final IdSecAdminActionUserDelete e)
  {
    final var permissions = e.admin().permissions();
    if (permissions.implies(USER_DELETE)) {
      return new IdSecPolicyResultPermitted();
    }

    return new IdSecPolicyResultDenied(
      "Deleting users requires the %s permission.".formatted(USER_DELETE)
    );
  }

  private static IdSecPolicyResultType checkAdminActionUserCreate(
    final IdSecAdminActionUserCreate e)
  {
    final var permissions = e.admin().permissions();
    if (permissions.implies(USER_CREATE)) {
      return new IdSecPolicyResultPermitted();
    }

    return new IdSecPolicyResultDenied(
      "Creating users requires the %s permission.".formatted(USER_CREATE)
    );
  }

  private static IdSecPolicyResultType checkAdminActionUserUpdate(
    final IdSecAdminActionUserUpdate e)
  {
    final var permissions = e.admin().permissions();
    if (permissions.implies(USER_WRITE)) {
      return new IdSecPolicyResultPermitted();
    }

    return new IdSecPolicyResultDenied(
      "Updating users requires the %s permission.".formatted(USER_WRITE)
    );
  }

  private static IdSecPolicyResultType checkAdminActionAdminRead(
    final IdSecAdminActionAdminRead e)
  {
    final var permissions = e.admin().permissions();
    if (permissions.implies(ADMIN_READ)) {
      return new IdSecPolicyResultPermitted();
    }

    return new IdSecPolicyResultDenied(
      "Reading admins requires the %s permission.".formatted(ADMIN_READ)
    );
  }

  private static IdSecPolicyResultType checkAdminActionAdminCreate(
    final IdSecAdminActionAdminCreate e)
  {
    final var permissionsHeld =
      e.admin().permissions();
    final var permissionsWanted =
      e.targetPermissions();

    if (!permissionsHeld.implies(ADMIN_CREATE)) {
      return new IdSecPolicyResultDenied(
        "Creating admins requires the %s permission.".formatted(ADMIN_CREATE)
      );
    }

    if (permissionsHeld.impliesAll(permissionsWanted)) {
      return new IdSecPolicyResultPermitted();
    }

    final var missing = new HashSet<>(permissionsWanted);
    missing.removeAll(permissionsHeld.impliedPermissions());

    return new IdSecPolicyResultDenied(
      "The current admin cannot grant the following permissions: %s"
        .formatted(missing)
    );
  }

  private static IdSecPolicyResultType checkAdminActionAdminUpdate(
    final IdSecAdminActionAdminUpdate e)
  {
    final var admin = e.admin();
    final var permissionsHeld = admin.permissions();
    if (Objects.equals(admin.id(), e.targetAdmin())) {
      if (!permissionsHeld.implies(ADMIN_WRITE_SELF)) {
        return new IdSecPolicyResultDenied(
          "Modifying admins requires the %s permission.".formatted(ADMIN_WRITE_SELF)
        );
      }
    } else {
      if (!permissionsHeld.implies(ADMIN_WRITE)) {
        return new IdSecPolicyResultDenied(
          "Modifying admins requires the %s permission.".formatted(ADMIN_WRITE)
        );
      }
    }

    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkAdminActionAdminDelete(
    final IdSecAdminActionAdminDelete e)
  {
    final var permissionsHeld = e.admin().permissions();
    if (permissionsHeld.implies(ADMIN_DELETE)) {
      return new IdSecPolicyResultPermitted();
    }

    return new IdSecPolicyResultDenied(
      "Deleting admins requires the %s permission.".formatted(ADMIN_DELETE)
    );
  }

  @Override
  public IdSecPolicyResultType check(
    final IdSecActionType action)
  {
    Objects.requireNonNull(action, "action");

    if (action instanceof IdSecAdminActionType admin) {
      return checkAdminAction(admin);
    }
    if (action instanceof IdSecUserActionType user) {
      return checkUserAction(user);
    }
    return new IdSecPolicyResultDenied("Operation not permitted.");
  }
}

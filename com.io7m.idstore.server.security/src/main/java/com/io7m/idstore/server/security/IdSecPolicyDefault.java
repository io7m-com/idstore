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

import java.util.Objects;

import static com.io7m.idstore.model.IdAdminPermission.AUDIT_READ;
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
    if (action instanceof IdSecAdminActionAuditRead e) {
      return checkAdminActionAuditRead(e);
    }

    return new IdSecPolicyResultDenied("Operation not permitted.");
  }

  private static IdSecPolicyResultType checkAdminActionAuditRead(
    final IdSecAdminActionAuditRead e)
  {
    final var permissions = e.admin().permissions();
    if (permissions.contains(AUDIT_READ)) {
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
    if (permissions.contains(USER_READ)) {
      return new IdSecPolicyResultPermitted();
    }

    return new IdSecPolicyResultDenied(
      "Reading users requires the %s permission.".formatted(USER_READ)
    );
  }

  private static IdSecPolicyResultType checkAdminActionUserCreate(
    final IdSecAdminActionUserCreate e)
  {
    final var permissions = e.admin().permissions();
    if (permissions.contains(USER_WRITE)) {
      return new IdSecPolicyResultPermitted();
    }

    return new IdSecPolicyResultDenied(
      "Updating users requires the %s permission.".formatted(USER_WRITE)
    );
  }

  private static IdSecPolicyResultType checkAdminActionUserUpdate(
    final IdSecAdminActionUserUpdate e)
  {
    final var permissions = e.admin().permissions();
    if (permissions.contains(USER_WRITE)) {
      return new IdSecPolicyResultPermitted();
    }

    return new IdSecPolicyResultDenied(
      "Updating users requires the %s permission.".formatted(USER_WRITE)
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

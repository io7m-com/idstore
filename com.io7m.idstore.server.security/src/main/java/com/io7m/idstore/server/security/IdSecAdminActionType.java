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

package com.io7m.idstore.server.security;

import com.io7m.idstore.model.IdAdmin;

/**
 * A view of an action within the security policy. An <i>action</i> may (or may
 * not) be performed by an <i>admin</i> according to the security policy.
 */

public sealed interface IdSecAdminActionType
  extends IdSecActionType permits IdSecAdminActionAdminBanCreate,
  IdSecAdminActionAdminBanDelete,
  IdSecAdminActionAdminBanGet,
  IdSecAdminActionAdminCreate,
  IdSecAdminActionAdminDelete,
  IdSecAdminActionAdminEmailAdd,
  IdSecAdminActionAdminEmailRemove,
  IdSecAdminActionAdminPermissionGrant,
  IdSecAdminActionAdminPermissionRevoke,
  IdSecAdminActionAdminRead,
  IdSecAdminActionAdminUpdate,
  IdSecAdminActionAuditRead,
  IdSecAdminActionUserBanCreate,
  IdSecAdminActionUserBanDelete,
  IdSecAdminActionUserBanGet,
  IdSecAdminActionUserCreate,
  IdSecAdminActionUserDelete,
  IdSecAdminActionUserRead,
  IdSecAdminActionUserUpdateCredentials,
  IdSecAdminActionUserUpdateEmail
{
  /**
   * @return The admin performing the action
   */

  IdAdmin admin();
}

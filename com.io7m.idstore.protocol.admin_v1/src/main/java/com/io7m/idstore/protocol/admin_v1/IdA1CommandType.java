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

/**
 * The type of Public API v1 commands.
 *
 * @param <R> The expected type of the response
 */

public sealed interface IdA1CommandType<R extends IdA1ResponseType>
  extends IdA1MessageType
  permits IdA1CommandAdminBanCreate,
  IdA1CommandAdminBanDelete,
  IdA1CommandAdminBanGet,
  IdA1CommandAdminCreate,
  IdA1CommandAdminDelete,
  IdA1CommandAdminEmailAdd,
  IdA1CommandAdminEmailRemove,
  IdA1CommandAdminGet,
  IdA1CommandAdminGetByEmail,
  IdA1CommandAdminPermissionGrant,
  IdA1CommandAdminPermissionRevoke,
  IdA1CommandAdminSearchBegin,
  IdA1CommandAdminSearchByEmailBegin,
  IdA1CommandAdminSearchByEmailNext,
  IdA1CommandAdminSearchByEmailPrevious,
  IdA1CommandAdminSearchNext,
  IdA1CommandAdminSearchPrevious,
  IdA1CommandAdminSelf,
  IdA1CommandAdminUpdate,
  IdA1CommandAuditSearchBegin,
  IdA1CommandAuditSearchNext,
  IdA1CommandAuditSearchPrevious,
  IdA1CommandLogin,
  IdA1CommandUserBanCreate,
  IdA1CommandUserBanDelete,
  IdA1CommandUserBanGet,
  IdA1CommandUserCreate,
  IdA1CommandUserDelete,
  IdA1CommandUserEmailAdd,
  IdA1CommandUserEmailRemove,
  IdA1CommandUserGet,
  IdA1CommandUserGetByEmail,
  IdA1CommandUserLoginHistory,
  IdA1CommandUserSearchBegin,
  IdA1CommandUserSearchByEmailBegin,
  IdA1CommandUserSearchByEmailNext,
  IdA1CommandUserSearchByEmailPrevious,
  IdA1CommandUserSearchNext,
  IdA1CommandUserSearchPrevious,
  IdA1CommandUserUpdate
{

}

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


package com.io7m.idstore.protocol.admin;

/**
 * The type of commands in the Admin protocol.
 *
 * @param <R> The associated response type
 */

public sealed interface IdACommandType<R extends IdAResponseType>
  extends IdAMessageType
  permits IdACommandAdminBanCreate,
  IdACommandAdminBanDelete,
  IdACommandAdminBanGet,
  IdACommandAdminCreate,
  IdACommandAdminDelete,
  IdACommandAdminEmailAdd,
  IdACommandAdminEmailRemove,
  IdACommandAdminGet,
  IdACommandAdminGetByEmail,
  IdACommandAdminPermissionGrant,
  IdACommandAdminPermissionRevoke,
  IdACommandAdminSearchBegin,
  IdACommandAdminSearchByEmailBegin,
  IdACommandAdminSearchByEmailNext,
  IdACommandAdminSearchByEmailPrevious,
  IdACommandAdminSearchNext,
  IdACommandAdminSearchPrevious,
  IdACommandAdminSelf,
  IdACommandAdminUpdateCredentials,
  IdACommandAdminUpdatePasswordExpiration,
  IdACommandAuditSearchBegin,
  IdACommandAuditSearchNext,
  IdACommandAuditSearchPrevious,
  IdACommandLogin,
  IdACommandMailTest,
  IdACommandMaintenanceModeSet,
  IdACommandUserBanCreate,
  IdACommandUserBanDelete,
  IdACommandUserBanGet,
  IdACommandUserCreate,
  IdACommandUserDelete,
  IdACommandUserEmailAdd,
  IdACommandUserEmailRemove,
  IdACommandUserGet,
  IdACommandUserGetByEmail,
  IdACommandUserLoginHistory,
  IdACommandUserSearchBegin,
  IdACommandUserSearchByEmailBegin,
  IdACommandUserSearchByEmailNext,
  IdACommandUserSearchByEmailPrevious,
  IdACommandUserSearchNext,
  IdACommandUserSearchPrevious,
  IdACommandUserUpdateCredentials,
  IdACommandUserUpdatePasswordExpiration
{
  /**
   * @return The response type associated with this command
   */

  Class<R> responseClass();
}

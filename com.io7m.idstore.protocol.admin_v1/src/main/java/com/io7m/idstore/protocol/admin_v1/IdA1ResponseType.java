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

import java.util.UUID;

/**
 * The type of API v1 responses.
 */

public sealed interface IdA1ResponseType
  extends IdA1MessageType
  permits IdA1ResponseAdminBanCreate,
  IdA1ResponseAdminBanDelete,
  IdA1ResponseAdminBanGet,
  IdA1ResponseAdminCreate,
  IdA1ResponseAdminDelete,
  IdA1ResponseAdminGet,
  IdA1ResponseAdminSearchBegin,
  IdA1ResponseAdminSearchByEmailBegin,
  IdA1ResponseAdminSearchByEmailNext,
  IdA1ResponseAdminSearchByEmailPrevious,
  IdA1ResponseAdminSearchNext,
  IdA1ResponseAdminSearchPrevious,
  IdA1ResponseAdminSelf,
  IdA1ResponseAdminUpdate,
  IdA1ResponseAuditSearchBegin,
  IdA1ResponseAuditSearchNext,
  IdA1ResponseAuditSearchPrevious,
  IdA1ResponseError,
  IdA1ResponseLogin,
  IdA1ResponseUserBanCreate,
  IdA1ResponseUserBanDelete,
  IdA1ResponseUserBanGet,
  IdA1ResponseUserCreate,
  IdA1ResponseUserDelete,
  IdA1ResponseUserGet,
  IdA1ResponseUserSearchBegin,
  IdA1ResponseUserSearchByEmailBegin,
  IdA1ResponseUserSearchByEmailNext,
  IdA1ResponseUserSearchByEmailPrevious,
  IdA1ResponseUserSearchNext,
  IdA1ResponseUserSearchPrevious,
  IdA1ResponseUserUpdate
{
  /**
   * @return The server-assigned request ID
   */

  UUID requestId();
}

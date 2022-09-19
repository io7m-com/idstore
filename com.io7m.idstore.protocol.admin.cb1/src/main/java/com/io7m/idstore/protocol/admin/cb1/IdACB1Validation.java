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

package com.io7m.idstore.protocol.admin.cb1;

import com.io7m.cedarbridge.runtime.api.CBIntegerUnsigned16;
import com.io7m.cedarbridge.runtime.api.CBList;
import com.io7m.cedarbridge.runtime.api.CBString;
import com.io7m.idstore.model.IdAuditSearchParameters;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.protocol.admin.IdACommandAdminBanCreate;
import com.io7m.idstore.protocol.admin.IdACommandAdminBanDelete;
import com.io7m.idstore.protocol.admin.IdACommandAdminBanGet;
import com.io7m.idstore.protocol.admin.IdACommandAdminCreate;
import com.io7m.idstore.protocol.admin.IdACommandAdminDelete;
import com.io7m.idstore.protocol.admin.IdACommandAdminEmailAdd;
import com.io7m.idstore.protocol.admin.IdACommandAdminEmailRemove;
import com.io7m.idstore.protocol.admin.IdACommandAdminGet;
import com.io7m.idstore.protocol.admin.IdACommandAdminGetByEmail;
import com.io7m.idstore.protocol.admin.IdACommandAdminPermissionGrant;
import com.io7m.idstore.protocol.admin.IdACommandAdminPermissionRevoke;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchBegin;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchByEmailBegin;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchByEmailNext;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchNext;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchPrevious;
import com.io7m.idstore.protocol.admin.IdACommandAdminSelf;
import com.io7m.idstore.protocol.admin.IdACommandAdminUpdate;
import com.io7m.idstore.protocol.admin.IdACommandAuditSearchBegin;
import com.io7m.idstore.protocol.admin.IdACommandAuditSearchNext;
import com.io7m.idstore.protocol.admin.IdACommandAuditSearchPrevious;
import com.io7m.idstore.protocol.admin.IdACommandLogin;
import com.io7m.idstore.protocol.admin.IdACommandType;
import com.io7m.idstore.protocol.admin.IdACommandUserBanCreate;
import com.io7m.idstore.protocol.admin.IdACommandUserBanDelete;
import com.io7m.idstore.protocol.admin.IdACommandUserBanGet;
import com.io7m.idstore.protocol.admin.IdACommandUserCreate;
import com.io7m.idstore.protocol.admin.IdACommandUserDelete;
import com.io7m.idstore.protocol.admin.IdACommandUserEmailAdd;
import com.io7m.idstore.protocol.admin.IdACommandUserEmailRemove;
import com.io7m.idstore.protocol.admin.IdACommandUserGet;
import com.io7m.idstore.protocol.admin.IdACommandUserGetByEmail;
import com.io7m.idstore.protocol.admin.IdACommandUserLoginHistory;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchBegin;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchByEmailBegin;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchByEmailNext;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchNext;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchPrevious;
import com.io7m.idstore.protocol.admin.IdACommandUserUpdate;
import com.io7m.idstore.protocol.admin.IdAMessageType;
import com.io7m.idstore.protocol.admin.IdAResponseAdminBanCreate;
import com.io7m.idstore.protocol.admin.IdAResponseAdminBanDelete;
import com.io7m.idstore.protocol.admin.IdAResponseAdminBanGet;
import com.io7m.idstore.protocol.admin.IdAResponseAdminCreate;
import com.io7m.idstore.protocol.admin.IdAResponseAdminDelete;
import com.io7m.idstore.protocol.admin.IdAResponseAdminEmailAdd;
import com.io7m.idstore.protocol.admin.IdAResponseAdminEmailRemove;
import com.io7m.idstore.protocol.admin.IdAResponseAdminGet;
import com.io7m.idstore.protocol.admin.IdAResponseAdminPermissionGrant;
import com.io7m.idstore.protocol.admin.IdAResponseAdminPermissionRevoke;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSearchBegin;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSearchByEmailBegin;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSearchByEmailNext;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSearchNext;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSearchPrevious;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSelf;
import com.io7m.idstore.protocol.admin.IdAResponseAdminUpdate;
import com.io7m.idstore.protocol.admin.IdAResponseAuditSearchBegin;
import com.io7m.idstore.protocol.admin.IdAResponseAuditSearchNext;
import com.io7m.idstore.protocol.admin.IdAResponseAuditSearchPrevious;
import com.io7m.idstore.protocol.admin.IdAResponseError;
import com.io7m.idstore.protocol.admin.IdAResponseLogin;
import com.io7m.idstore.protocol.admin.IdAResponseType;
import com.io7m.idstore.protocol.admin.IdAResponseUserBanCreate;
import com.io7m.idstore.protocol.admin.IdAResponseUserBanDelete;
import com.io7m.idstore.protocol.admin.IdAResponseUserBanGet;
import com.io7m.idstore.protocol.admin.IdAResponseUserCreate;
import com.io7m.idstore.protocol.admin.IdAResponseUserDelete;
import com.io7m.idstore.protocol.admin.IdAResponseUserEmailAdd;
import com.io7m.idstore.protocol.admin.IdAResponseUserEmailRemove;
import com.io7m.idstore.protocol.admin.IdAResponseUserGet;
import com.io7m.idstore.protocol.admin.IdAResponseUserLoginHistory;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchBegin;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchByEmailBegin;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchByEmailNext;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchNext;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchPrevious;
import com.io7m.idstore.protocol.admin.IdAResponseUserUpdate;
import com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationGeneral;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.api.IdProtocolMessageValidatorType;

import static com.io7m.cedarbridge.runtime.api.CBOptionType.fromOptional;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireCommandAdminBanCreate;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireCommandAdminBanDelete;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireCommandAdminBanGet;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireCommandAdminCreate;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireCommandAdminDelete;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireCommandAdminEmailAdd;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireCommandAdminEmailRemove;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireCommandAdminGet;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireCommandAdminGetByEmail;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireCommandAdminPermissionGrant;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireCommandAdminPermissionRevoke;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireCommandAdminSearchBegin;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireCommandAdminSearchByEmailBegin;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireCommandAdminSearchByEmailNext;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireCommandAdminSearchByEmailPrevious;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireCommandAdminSearchNext;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireCommandAdminSearchPrevious;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireCommandAdminSelf;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireCommandAdminUpdate;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireResponseAdminBanCreate;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireResponseAdminBanDelete;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireResponseAdminBanGet;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireResponseAdminCreate;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireResponseAdminDelete;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireResponseAdminEmailAdd;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireResponseAdminEmailRemove;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireResponseAdminGet;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireResponseAdminPermissionGrant;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireResponseAdminPermissionRevoke;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireResponseAdminSearchBegin;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireResponseAdminSearchByEmailBegin;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireResponseAdminSearchByEmailNext;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireResponseAdminSearchByEmailPrevious;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireResponseAdminSearchNext;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireResponseAdminSearchPrevious;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireResponseAdminSelf;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.fromWireResponseAdminUpdate;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireCommandAdminBanCreate;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireCommandAdminBanDelete;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireCommandAdminBanGet;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireCommandAdminCreate;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireCommandAdminDelete;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireCommandAdminEmailAdd;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireCommandAdminEmailRemove;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireCommandAdminGet;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireCommandAdminGetByEmail;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireCommandAdminPermissionGrant;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireCommandAdminPermissionRevoke;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireCommandAdminSearchBegin;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireCommandAdminSearchByEmailBegin;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireCommandAdminSearchByEmailNext;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireCommandAdminSearchByEmailPrevious;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireCommandAdminSearchNext;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireCommandAdminSearchPrevious;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireCommandAdminSelf;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireCommandAdminUpdate;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireResponseAdminBanCreate;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireResponseAdminBanDelete;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireResponseAdminBanGet;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireResponseAdminCreate;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireResponseAdminDelete;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireResponseAdminEmailAdd;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireResponseAdminEmailRemove;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireResponseAdminGet;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireResponseAdminPermissionGrant;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireResponseAdminPermissionRevoke;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireResponseAdminSearchBegin;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireResponseAdminSearchByEmailBegin;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireResponseAdminSearchByEmailNext;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireResponseAdminSearchByEmailPrevious;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireResponseAdminSearchNext;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireResponseAdminSearchPrevious;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireResponseAdminSelf;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationAdmin.toWireResponseAdminUpdate;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationGeneral.fromWirePage;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationGeneral.fromWireTimeRange;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationGeneral.fromWireUUID;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationGeneral.toWirePage;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationGeneral.toWireTimeRange;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationGeneral.toWireUUID;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.fromWireCommandUserBanCreate;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.fromWireCommandUserBanDelete;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.fromWireCommandUserBanGet;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.fromWireCommandUserCreate;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.fromWireCommandUserDelete;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.fromWireCommandUserEmailAdd;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.fromWireCommandUserEmailRemove;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.fromWireCommandUserGet;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.fromWireCommandUserGetByEmail;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.fromWireCommandUserSearchBegin;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.fromWireCommandUserSearchByEmailBegin;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.fromWireCommandUserSearchByEmailNext;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.fromWireCommandUserSearchByEmailPrevious;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.fromWireCommandUserSearchNext;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.fromWireCommandUserSearchPrevious;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.fromWireCommandUserUpdate;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.fromWireResponseUserBanCreate;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.fromWireResponseUserBanDelete;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.fromWireResponseUserBanGet;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.fromWireResponseUserCreate;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.fromWireResponseUserDelete;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.fromWireResponseUserEmailAdd;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.fromWireResponseUserEmailRemove;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.fromWireResponseUserGet;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.fromWireResponseUserSearchBegin;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.fromWireResponseUserSearchByEmailBegin;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.fromWireResponseUserSearchByEmailNext;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.fromWireResponseUserSearchByEmailPrevious;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.fromWireResponseUserSearchNext;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.fromWireResponseUserSearchPrevious;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.fromWireResponseUserUpdate;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.toWireCommandUserBanCreate;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.toWireCommandUserBanDelete;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.toWireCommandUserBanGet;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.toWireCommandUserCreate;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.toWireCommandUserDelete;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.toWireCommandUserEmailAdd;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.toWireCommandUserEmailRemove;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.toWireCommandUserGet;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.toWireCommandUserGetByEmail;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.toWireCommandUserSearchBegin;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.toWireCommandUserSearchByEmailBegin;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.toWireCommandUserSearchByEmailNext;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.toWireCommandUserSearchByEmailPrevious;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.toWireCommandUserSearchNext;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.toWireCommandUserSearchPrevious;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.toWireCommandUserUpdate;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.toWireResponseUserBanCreate;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.toWireResponseUserBanDelete;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.toWireResponseUserBanGet;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.toWireResponseUserCreate;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.toWireResponseUserDelete;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.toWireResponseUserEmailAdd;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.toWireResponseUserEmailRemove;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.toWireResponseUserGet;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.toWireResponseUserSearchBegin;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.toWireResponseUserSearchByEmailBegin;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.toWireResponseUserSearchByEmailNext;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.toWireResponseUserSearchByEmailPrevious;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.toWireResponseUserSearchNext;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.toWireResponseUserSearchPrevious;
import static com.io7m.idstore.protocol.admin.cb1.internal.IdACB1ValidationUser.toWireResponseUserUpdate;

/**
 * Functions to translate between the core command set and the Admin v1
 * Cedarbridge encoding command set.
 */

public final class IdACB1Validation
  implements IdProtocolMessageValidatorType<IdAMessageType, ProtocolIdA1v1Type>
{
  /**
   * Functions to translate between the core command set and the Admin v1
   * Cedarbridge encoding command set.
   */

  public IdACB1Validation()
  {

  }

  private static ProtocolIdA1v1Type toWireResponse(
    final IdAResponseType response)
    throws IdProtocolException
  {
    if (response instanceof IdAResponseError c) {
      return toWireResponseError(c);
    } else if (response instanceof IdAResponseLogin c) {
      return toWireResponseLogin(c);

      /*
       * Admin commands
       */

    } else if (response instanceof IdAResponseAdminBanCreate c) {
      return toWireResponseAdminBanCreate(c);
    } else if (response instanceof IdAResponseAdminBanDelete c) {
      return toWireResponseAdminBanDelete(c);
    } else if (response instanceof IdAResponseAdminBanGet c) {
      return toWireResponseAdminBanGet(c);
    } else if (response instanceof IdAResponseAdminCreate c) {
      return toWireResponseAdminCreate(c);
    } else if (response instanceof IdAResponseAdminDelete c) {
      return toWireResponseAdminDelete(c);
    } else if (response instanceof IdAResponseAdminEmailAdd c) {
      return toWireResponseAdminEmailAdd(c);
    } else if (response instanceof IdAResponseAdminEmailRemove c) {
      return toWireResponseAdminEmailRemove(c);
    } else if (response instanceof IdAResponseAdminGet c) {
      return toWireResponseAdminGet(c);
    } else if (response instanceof IdAResponseAdminPermissionGrant c) {
      return toWireResponseAdminPermissionGrant(c);
    } else if (response instanceof IdAResponseAdminPermissionRevoke c) {
      return toWireResponseAdminPermissionRevoke(c);
    } else if (response instanceof IdAResponseAdminSearchBegin c) {
      return toWireResponseAdminSearchBegin(c);
    } else if (response instanceof IdAResponseAdminSearchByEmailBegin c) {
      return toWireResponseAdminSearchByEmailBegin(c);
    } else if (response instanceof IdAResponseAdminSearchByEmailNext c) {
      return toWireResponseAdminSearchByEmailNext(c);
    } else if (response instanceof IdAResponseAdminSearchByEmailPrevious c) {
      return toWireResponseAdminSearchByEmailPrevious(c);
    } else if (response instanceof IdAResponseAdminSearchNext c) {
      return toWireResponseAdminSearchNext(c);
    } else if (response instanceof IdAResponseAdminSearchPrevious c) {
      return toWireResponseAdminSearchPrevious(c);
    } else if (response instanceof IdAResponseAdminSelf c) {
      return toWireResponseAdminSelf(c);
    } else if (response instanceof IdAResponseAdminUpdate c) {
      return toWireResponseAdminUpdate(c);
    } else if (response instanceof IdAResponseAuditSearchBegin c) {
      return toWireResponseAuditSearchBegin(c);
    } else if (response instanceof IdAResponseAuditSearchNext c) {
      return toWireResponseAuditSearchNext(c);
    } else if (response instanceof IdAResponseAuditSearchPrevious c) {
      return toWireResponseAuditSearchPrevious(c);

      /*
       * User responses.
       */

    } else if (response instanceof IdAResponseUserBanCreate c) {
      return toWireResponseUserBanCreate(c);
    } else if (response instanceof IdAResponseUserBanDelete c) {
      return toWireResponseUserBanDelete(c);
    } else if (response instanceof IdAResponseUserBanGet c) {
      return toWireResponseUserBanGet(c);
    } else if (response instanceof IdAResponseUserCreate c) {
      return toWireResponseUserCreate(c);
    } else if (response instanceof IdAResponseUserDelete c) {
      return toWireResponseUserDelete(c);
    } else if (response instanceof IdAResponseUserEmailAdd c) {
      return toWireResponseUserEmailAdd(c);
    } else if (response instanceof IdAResponseUserEmailRemove c) {
      return toWireResponseUserEmailRemove(c);
    } else if (response instanceof IdAResponseUserGet c) {
      return toWireResponseUserGet(c);
    } else if (response instanceof IdAResponseUserSearchBegin c) {
      return toWireResponseUserSearchBegin(c);
    } else if (response instanceof IdAResponseUserSearchByEmailBegin c) {
      return toWireResponseUserSearchByEmailBegin(c);
    } else if (response instanceof IdAResponseUserSearchByEmailNext c) {
      return toWireResponseUserSearchByEmailNext(c);
    } else if (response instanceof IdAResponseUserSearchByEmailPrevious c) {
      return toWireResponseUserSearchByEmailPrevious(c);
    } else if (response instanceof IdAResponseUserSearchNext c) {
      return toWireResponseUserSearchNext(c);
    } else if (response instanceof IdAResponseUserSearchPrevious c) {
      return toWireResponseUserSearchPrevious(c);
    } else if (response instanceof IdAResponseUserUpdate c) {
      return toWireResponseUserUpdate(c);
    } else if (response instanceof IdAResponseUserLoginHistory c) {
      return toWireResponseUserLoginHistory(c);
    }

    throw new IdProtocolException(
      PROTOCOL_ERROR,
      "Unrecognized message: %s".formatted(response)
    );
  }

  private static IdA1ResponseUserLoginHistory toWireResponseUserLoginHistory(
    final IdAResponseUserLoginHistory c)
  {
    return new IdA1ResponseUserLoginHistory(
      toWireUUID(c.requestId()),
      new CBList<>(
        c.history()
          .stream()
          .map(IdACB1ValidationGeneral::toWireLogin)
          .toList())
    );
  }

  private static IdA1ResponseAuditSearchBegin toWireResponseAuditSearchBegin(
    final IdAResponseAuditSearchBegin r)
  {
    return new IdA1ResponseAuditSearchBegin(
      toWireUUID(r.requestId()),
      toWirePage(r.page(), IdACB1ValidationGeneral::toWireAuditEvent)
    );
  }

  private static IdA1ResponseAuditSearchNext toWireResponseAuditSearchNext(
    final IdAResponseAuditSearchNext r)
  {
    return new IdA1ResponseAuditSearchNext(
      toWireUUID(r.requestId()),
      toWirePage(r.page(), IdACB1ValidationGeneral::toWireAuditEvent)
    );
  }

  private static IdA1ResponseAuditSearchPrevious toWireResponseAuditSearchPrevious(
    final IdAResponseAuditSearchPrevious r)
  {
    return new IdA1ResponseAuditSearchPrevious(
      toWireUUID(r.requestId()),
      toWirePage(r.page(), IdACB1ValidationGeneral::toWireAuditEvent)
    );
  }

  private static IdA1ResponseError toWireResponseError(
    final IdAResponseError error)
  {
    return new IdA1ResponseError(
      toWireUUID(error.requestId()),
      new CBString(error.errorCode()),
      new CBString(error.message())
    );
  }

  private static IdA1ResponseLogin toWireResponseLogin(
    final IdAResponseLogin login)
  {
    return new IdA1ResponseLogin(toWireUUID(login.requestId()));
  }

  private static ProtocolIdA1v1Type convertToWireCommand(
    final IdACommandType<?> command)
    throws IdProtocolException
  {
    if (command instanceof IdACommandLogin c) {
      return toWireCommandLogin(c);

      /*
       * Admin commands.
       */

    } else if (command instanceof IdACommandAdminBanCreate c) {
      return toWireCommandAdminBanCreate(c);
    } else if (command instanceof IdACommandAdminBanDelete c) {
      return toWireCommandAdminBanDelete(c);
    } else if (command instanceof IdACommandAdminBanGet c) {
      return toWireCommandAdminBanGet(c);
    } else if (command instanceof IdACommandAdminCreate c) {
      return toWireCommandAdminCreate(c);
    } else if (command instanceof IdACommandAdminDelete c) {
      return toWireCommandAdminDelete(c);
    } else if (command instanceof IdACommandAdminEmailAdd c) {
      return toWireCommandAdminEmailAdd(c);
    } else if (command instanceof IdACommandAdminEmailRemove c) {
      return toWireCommandAdminEmailRemove(c);
    } else if (command instanceof IdACommandAdminGet c) {
      return toWireCommandAdminGet(c);
    } else if (command instanceof IdACommandAdminGetByEmail c) {
      return toWireCommandAdminGetByEmail(c);
    } else if (command instanceof IdACommandAdminPermissionGrant c) {
      return toWireCommandAdminPermissionGrant(c);
    } else if (command instanceof IdACommandAdminPermissionRevoke c) {
      return toWireCommandAdminPermissionRevoke(c);
    } else if (command instanceof IdACommandAdminSearchBegin c) {
      return toWireCommandAdminSearchBegin(c);
    } else if (command instanceof IdACommandAdminSearchByEmailBegin c) {
      return toWireCommandAdminSearchByEmailBegin(c);
    } else if (command instanceof IdACommandAdminSearchByEmailNext c) {
      return toWireCommandAdminSearchByEmailNext(c);
    } else if (command instanceof IdACommandAdminSearchByEmailPrevious c) {
      return toWireCommandAdminSearchByEmailPrevious(c);
    } else if (command instanceof IdACommandAdminSearchNext c) {
      return toWireCommandAdminSearchNext(c);
    } else if (command instanceof IdACommandAdminSearchPrevious c) {
      return toWireCommandAdminSearchPrevious(c);
    } else if (command instanceof IdACommandAdminSelf c) {
      return toWireCommandAdminSelf(c);
    } else if (command instanceof IdACommandAdminUpdate c) {
      return toWireCommandAdminUpdate(c);
    } else if (command instanceof IdACommandAuditSearchBegin c) {
      return toWireCommandAuditSearchBegin(c);
    } else if (command instanceof IdACommandAuditSearchNext c) {
      return toWireCommandAuditSearchNext(c);
    } else if (command instanceof IdACommandAuditSearchPrevious c) {
      return toWireCommandAuditSearchPrevious(c);

      /*
       * User commands.
       */

    } else if (command instanceof IdACommandUserBanCreate c) {
      return toWireCommandUserBanCreate(c);
    } else if (command instanceof IdACommandUserBanDelete c) {
      return toWireCommandUserBanDelete(c);
    } else if (command instanceof IdACommandUserBanGet c) {
      return toWireCommandUserBanGet(c);
    } else if (command instanceof IdACommandUserCreate c) {
      return toWireCommandUserCreate(c);
    } else if (command instanceof IdACommandUserDelete c) {
      return toWireCommandUserDelete(c);
    } else if (command instanceof IdACommandUserEmailAdd c) {
      return toWireCommandUserEmailAdd(c);
    } else if (command instanceof IdACommandUserEmailRemove c) {
      return toWireCommandUserEmailRemove(c);
    } else if (command instanceof IdACommandUserGet c) {
      return toWireCommandUserGet(c);
    } else if (command instanceof IdACommandUserGetByEmail c) {
      return toWireCommandUserGetByEmail(c);
    } else if (command instanceof IdACommandUserSearchBegin c) {
      return toWireCommandUserSearchBegin(c);
    } else if (command instanceof IdACommandUserSearchByEmailBegin c) {
      return toWireCommandUserSearchByEmailBegin(c);
    } else if (command instanceof IdACommandUserSearchByEmailNext c) {
      return toWireCommandUserSearchByEmailNext(c);
    } else if (command instanceof IdACommandUserSearchByEmailPrevious c) {
      return toWireCommandUserSearchByEmailPrevious(c);
    } else if (command instanceof IdACommandUserSearchNext c) {
      return toWireCommandUserSearchNext(c);
    } else if (command instanceof IdACommandUserSearchPrevious c) {
      return toWireCommandUserSearchPrevious(c);
    } else if (command instanceof IdACommandUserUpdate c) {
      return toWireCommandUserUpdate(c);
    } else if (command instanceof IdACommandUserLoginHistory c) {
      return toWireCommandUserLoginHistory(c);
    }

    throw new IdProtocolException(
      PROTOCOL_ERROR,
      "Unrecognized message: %s".formatted(command)
    );
  }

  private static IdA1CommandUserLoginHistory toWireCommandUserLoginHistory(
    final IdACommandUserLoginHistory c)
  {
    return new IdA1CommandUserLoginHistory(
      toWireUUID(c.user())
    );
  }

  private static IdA1CommandAuditSearchNext toWireCommandAuditSearchNext(
    final IdACommandAuditSearchNext c)
  {
    return new IdA1CommandAuditSearchNext();
  }

  private static IdA1CommandAuditSearchPrevious toWireCommandAuditSearchPrevious(
    final IdACommandAuditSearchPrevious c)
  {
    return new IdA1CommandAuditSearchPrevious();
  }

  private static IdA1CommandAuditSearchBegin toWireCommandAuditSearchBegin(
    final IdACommandAuditSearchBegin c)
  {
    return new IdA1CommandAuditSearchBegin(
      toWireAuditSearchParameters(c.parameters())
    );
  }

  private static IdA1AuditSearchParameters toWireAuditSearchParameters(
    final IdAuditSearchParameters parameters)
  {
    return new IdA1AuditSearchParameters(
      toWireTimeRange(parameters.timeRange()),
      fromOptional(parameters.owner().map(CBString::new)),
      fromOptional(parameters.message().map(CBString::new)),
      fromOptional(parameters.type().map(CBString::new)),
      new CBIntegerUnsigned16(parameters.limit())
    );
  }

  private static IdA1CommandLogin toWireCommandLogin(
    final IdACommandLogin login)
  {
    return new IdA1CommandLogin(
      new CBString(login.userName().value()),
      new CBString(login.password())
    );
  }

  private static IdAResponseError fromWireResponseError(
    final IdA1ResponseError error)
  {
    return new IdAResponseError(
      fromWireUUID(error.fieldRequestId()),
      error.fieldErrorCode().value(),
      error.fieldMessage().value()
    );
  }

  private static IdAResponseLogin fromWireResponseLogin(
    final IdA1ResponseLogin login)
  {
    return new IdAResponseLogin(fromWireUUID(login.fieldRequestId()));
  }

  private static IdACommandLogin fromWireCommandLogin(
    final IdA1CommandLogin login)
  {
    return new IdACommandLogin(
      new IdName(login.fieldUserName().value()),
      login.fieldPassword().value()
    );
  }

  @Override
  public ProtocolIdA1v1Type convertToWire(
    final IdAMessageType message)
    throws IdProtocolException
  {
    if (message instanceof IdACommandType<?> command) {
      return convertToWireCommand(command);
    } else if (message instanceof IdAResponseType response) {
      return toWireResponse(response);
    } else {
      throw new IdProtocolException(
        PROTOCOL_ERROR,
        "Unrecognized message: %s".formatted(message)
      );
    }
  }

  @Override
  public IdAMessageType convertFromWire(
    final ProtocolIdA1v1Type message)
    throws IdProtocolException
  {
    try {
      if (message instanceof IdA1CommandLogin c) {
        return fromWireCommandLogin(c);
      } else if (message instanceof IdA1ResponseLogin c) {
        return fromWireResponseLogin(c);
      } else if (message instanceof IdA1ResponseError c) {
        return fromWireResponseError(c);

        /*
         * Admin commands.
         */

      } else if (message instanceof IdA1CommandAdminBanCreate c) {
        return fromWireCommandAdminBanCreate(c);
      } else if (message instanceof IdA1CommandAdminBanDelete c) {
        return fromWireCommandAdminBanDelete(c);
      } else if (message instanceof IdA1CommandAdminBanGet c) {
        return fromWireCommandAdminBanGet(c);
      } else if (message instanceof IdA1CommandAdminCreate c) {
        return fromWireCommandAdminCreate(c);
      } else if (message instanceof IdA1CommandAdminDelete c) {
        return fromWireCommandAdminDelete(c);
      } else if (message instanceof IdA1CommandAdminEmailAdd c) {
        return fromWireCommandAdminEmailAdd(c);
      } else if (message instanceof IdA1CommandAdminEmailRemove c) {
        return fromWireCommandAdminEmailRemove(c);
      } else if (message instanceof IdA1CommandAdminGet c) {
        return fromWireCommandAdminGet(c);
      } else if (message instanceof IdA1CommandAdminGetByEmail c) {
        return fromWireCommandAdminGetByEmail(c);
      } else if (message instanceof IdA1CommandAdminPermissionGrant c) {
        return fromWireCommandAdminPermissionGrant(c);
      } else if (message instanceof IdA1CommandAdminPermissionRevoke c) {
        return fromWireCommandAdminPermissionRevoke(c);
      } else if (message instanceof IdA1CommandAdminSearchBegin c) {
        return fromWireCommandAdminSearchBegin(c);
      } else if (message instanceof IdA1CommandAdminSearchByEmailBegin c) {
        return fromWireCommandAdminSearchByEmailBegin(c);
      } else if (message instanceof IdA1CommandAdminSearchByEmailNext c) {
        return fromWireCommandAdminSearchByEmailNext(c);
      } else if (message instanceof IdA1CommandAdminSearchByEmailPrevious c) {
        return fromWireCommandAdminSearchByEmailPrevious(c);
      } else if (message instanceof IdA1CommandAdminSearchNext c) {
        return fromWireCommandAdminSearchNext(c);
      } else if (message instanceof IdA1CommandAdminSearchPrevious c) {
        return fromWireCommandAdminSearchPrevious(c);
      } else if (message instanceof IdA1CommandAdminSelf c) {
        return fromWireCommandAdminSelf(c);
      } else if (message instanceof IdA1CommandAdminUpdate c) {
        return fromWireCommandAdminUpdate(c);
      } else if (message instanceof IdA1CommandAuditSearchBegin c) {
        return fromWireCommandAuditSearchBegin(c);
      } else if (message instanceof IdA1CommandAuditSearchNext c) {
        return fromWireCommandAuditSearchNext(c);
      } else if (message instanceof IdA1CommandAuditSearchPrevious c) {
        return fromWireCommandAuditSearchPrevious(c);

        /*
         * Admin responses.
         */

      } else if (message instanceof IdA1ResponseAdminBanCreate c) {
        return fromWireResponseAdminBanCreate(c);
      } else if (message instanceof IdA1ResponseAdminBanDelete c) {
        return fromWireResponseAdminBanDelete(c);
      } else if (message instanceof IdA1ResponseAdminBanGet c) {
        return fromWireResponseAdminBanGet(c);
      } else if (message instanceof IdA1ResponseAdminCreate c) {
        return fromWireResponseAdminCreate(c);
      } else if (message instanceof IdA1ResponseAdminDelete c) {
        return fromWireResponseAdminDelete(c);
      } else if (message instanceof IdA1ResponseAdminEmailAdd c) {
        return fromWireResponseAdminEmailAdd(c);
      } else if (message instanceof IdA1ResponseAdminEmailRemove c) {
        return fromWireResponseAdminEmailRemove(c);
      } else if (message instanceof IdA1ResponseAdminGet c) {
        return fromWireResponseAdminGet(c);
      } else if (message instanceof IdA1ResponseAdminPermissionGrant c) {
        return fromWireResponseAdminPermissionGrant(c);
      } else if (message instanceof IdA1ResponseAdminPermissionRevoke c) {
        return fromWireResponseAdminPermissionRevoke(c);
      } else if (message instanceof IdA1ResponseAdminSearchBegin c) {
        return fromWireResponseAdminSearchBegin(c);
      } else if (message instanceof IdA1ResponseAdminSearchByEmailBegin c) {
        return fromWireResponseAdminSearchByEmailBegin(c);
      } else if (message instanceof IdA1ResponseAdminSearchByEmailNext c) {
        return fromWireResponseAdminSearchByEmailNext(c);
      } else if (message instanceof IdA1ResponseAdminSearchByEmailPrevious c) {
        return fromWireResponseAdminSearchByEmailPrevious(c);
      } else if (message instanceof IdA1ResponseAdminSearchNext c) {
        return fromWireResponseAdminSearchNext(c);
      } else if (message instanceof IdA1ResponseAdminSearchPrevious c) {
        return fromWireResponseAdminSearchPrevious(c);
      } else if (message instanceof IdA1ResponseAdminSelf c) {
        return fromWireResponseAdminSelf(c);
      } else if (message instanceof IdA1ResponseAdminUpdate c) {
        return fromWireResponseAdminUpdate(c);

        /*
         * Audit responses.
         */

      } else if (message instanceof IdA1ResponseAuditSearchBegin c) {
        return fromWireResponseAuditSearchBegin(c);
      } else if (message instanceof IdA1ResponseAuditSearchNext c) {
        return fromWireResponseAuditSearchNext(c);
      } else if (message instanceof IdA1ResponseAuditSearchPrevious c) {
        return fromWireResponseAuditSearchPrevious(c);

        /*
         * User commands.
         */

      } else if (message instanceof IdA1CommandUserBanCreate c) {
        return fromWireCommandUserBanCreate(c);
      } else if (message instanceof IdA1CommandUserBanDelete c) {
        return fromWireCommandUserBanDelete(c);
      } else if (message instanceof IdA1CommandUserBanGet c) {
        return fromWireCommandUserBanGet(c);
      } else if (message instanceof IdA1CommandUserCreate c) {
        return fromWireCommandUserCreate(c);
      } else if (message instanceof IdA1CommandUserDelete c) {
        return fromWireCommandUserDelete(c);
      } else if (message instanceof IdA1CommandUserEmailAdd c) {
        return fromWireCommandUserEmailAdd(c);
      } else if (message instanceof IdA1CommandUserEmailRemove c) {
        return fromWireCommandUserEmailRemove(c);
      } else if (message instanceof IdA1CommandUserGet c) {
        return fromWireCommandUserGet(c);
      } else if (message instanceof IdA1CommandUserGetByEmail c) {
        return fromWireCommandUserGetByEmail(c);
      } else if (message instanceof IdA1CommandUserSearchBegin c) {
        return fromWireCommandUserSearchBegin(c);
      } else if (message instanceof IdA1CommandUserSearchByEmailBegin c) {
        return fromWireCommandUserSearchByEmailBegin(c);
      } else if (message instanceof IdA1CommandUserSearchByEmailNext c) {
        return fromWireCommandUserSearchByEmailNext(c);
      } else if (message instanceof IdA1CommandUserSearchByEmailPrevious c) {
        return fromWireCommandUserSearchByEmailPrevious(c);
      } else if (message instanceof IdA1CommandUserSearchNext c) {
        return fromWireCommandUserSearchNext(c);
      } else if (message instanceof IdA1CommandUserSearchPrevious c) {
        return fromWireCommandUserSearchPrevious(c);
      } else if (message instanceof IdA1CommandUserUpdate c) {
        return fromWireCommandUserUpdate(c);
      } else if (message instanceof IdA1CommandUserLoginHistory c) {
        return fromWireCommandUserLoginHistory(c);

        /*
         * User responses.
         */

      } else if (message instanceof IdA1ResponseUserBanCreate c) {
        return fromWireResponseUserBanCreate(c);
      } else if (message instanceof IdA1ResponseUserBanDelete c) {
        return fromWireResponseUserBanDelete(c);
      } else if (message instanceof IdA1ResponseUserBanGet c) {
        return fromWireResponseUserBanGet(c);
      } else if (message instanceof IdA1ResponseUserCreate c) {
        return fromWireResponseUserCreate(c);
      } else if (message instanceof IdA1ResponseUserDelete c) {
        return fromWireResponseUserDelete(c);
      } else if (message instanceof IdA1ResponseUserEmailAdd c) {
        return fromWireResponseUserEmailAdd(c);
      } else if (message instanceof IdA1ResponseUserEmailRemove c) {
        return fromWireResponseUserEmailRemove(c);
      } else if (message instanceof IdA1ResponseUserGet c) {
        return fromWireResponseUserGet(c);
      } else if (message instanceof IdA1ResponseUserSearchBegin c) {
        return fromWireResponseUserSearchBegin(c);
      } else if (message instanceof IdA1ResponseUserSearchByEmailBegin c) {
        return fromWireResponseUserSearchByEmailBegin(c);
      } else if (message instanceof IdA1ResponseUserSearchByEmailNext c) {
        return fromWireResponseUserSearchByEmailNext(c);
      } else if (message instanceof IdA1ResponseUserSearchByEmailPrevious c) {
        return fromWireResponseUserSearchByEmailPrevious(c);
      } else if (message instanceof IdA1ResponseUserSearchNext c) {
        return fromWireResponseUserSearchNext(c);
      } else if (message instanceof IdA1ResponseUserSearchPrevious c) {
        return fromWireResponseUserSearchPrevious(c);
      } else if (message instanceof IdA1ResponseUserUpdate c) {
        return fromWireResponseUserUpdate(c);
      } else if (message instanceof IdA1ResponseUserLoginHistory c) {
        return fromWireResponseUserLoginHistory(c);
      }

    } catch (final Exception e) {
      throw new IdProtocolException(PROTOCOL_ERROR, e.getMessage(), e);
    }

    throw new IdProtocolException(
      PROTOCOL_ERROR,
      "Unrecognized message: %s".formatted(message)
    );
  }

  private static IdACommandUserLoginHistory fromWireCommandUserLoginHistory(
    final IdA1CommandUserLoginHistory c)
  {
    return new IdACommandUserLoginHistory(
      fromWireUUID(c.fieldUserId())
    );
  }

  private static IdAResponseUserLoginHistory fromWireResponseUserLoginHistory(
    final IdA1ResponseUserLoginHistory c)
  {
    return new IdAResponseUserLoginHistory(
      fromWireUUID(c.fieldRequestId()),
      c.fieldHistory()
        .values()
        .stream()
        .map(IdACB1ValidationGeneral::fromWireLogin)
        .toList()
    );
  }

  private static IdAResponseAuditSearchBegin fromWireResponseAuditSearchBegin(
    final IdA1ResponseAuditSearchBegin c)
  {
    return new IdAResponseAuditSearchBegin(
      fromWireUUID(c.fieldRequestId()),
      fromWirePage(c.fieldPage(), IdACB1ValidationGeneral::fromWireAuditEvent)
    );
  }

  private static IdAResponseAuditSearchNext fromWireResponseAuditSearchNext(
    final IdA1ResponseAuditSearchNext c)
  {
    return new IdAResponseAuditSearchNext(
      fromWireUUID(c.fieldRequestId()),
      fromWirePage(c.fieldPage(), IdACB1ValidationGeneral::fromWireAuditEvent)
    );
  }

  private static IdAResponseAuditSearchPrevious fromWireResponseAuditSearchPrevious(
    final IdA1ResponseAuditSearchPrevious c)
  {
    return new IdAResponseAuditSearchPrevious(
      fromWireUUID(c.fieldRequestId()),
      fromWirePage(c.fieldPage(), IdACB1ValidationGeneral::fromWireAuditEvent)
    );
  }

  private static IdACommandAuditSearchBegin fromWireCommandAuditSearchBegin(
    final IdA1CommandAuditSearchBegin c)
  {
    return new IdACommandAuditSearchBegin(
      fromWireAuditSearchParameters(c.fieldParameters())
    );
  }

  private static IdACommandAuditSearchNext fromWireCommandAuditSearchNext(
    final IdA1CommandAuditSearchNext c)
  {
    return new IdACommandAuditSearchNext();
  }

  private static IdACommandAuditSearchPrevious fromWireCommandAuditSearchPrevious(
    final IdA1CommandAuditSearchPrevious c)
  {
    return new IdACommandAuditSearchPrevious();
  }

  private static IdAuditSearchParameters fromWireAuditSearchParameters(
    final IdA1AuditSearchParameters p)
  {
    return new IdAuditSearchParameters(
      fromWireTimeRange(p.fieldTimeRange()),
      p.fieldOwner().asOptional().map(CBString::value),
      p.fieldType().asOptional().map(CBString::value),
      p.fieldMessage().asOptional().map(CBString::value),
      p.fieldLimit().value()
    );
  }
}

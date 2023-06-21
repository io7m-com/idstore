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

package com.io7m.idstore.protocol.admin.cb;

import com.io7m.cedarbridge.runtime.api.CBCore;
import com.io7m.cedarbridge.runtime.api.CBIntegerUnsigned16;
import com.io7m.cedarbridge.runtime.api.CBMap;
import com.io7m.cedarbridge.runtime.api.CBString;
import com.io7m.cedarbridge.runtime.api.CBUUID;
import com.io7m.cedarbridge.runtime.convenience.CBLists;
import com.io7m.cedarbridge.runtime.convenience.CBMaps;
import com.io7m.cedarbridge.runtime.time.CBOffsetDateTime;
import com.io7m.idstore.error_codes.IdErrorCode;
import com.io7m.idstore.model.IdAuditSearchParameters;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdShortHumanToken;
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
import com.io7m.idstore.protocol.admin.IdACommandAdminUpdateCredentials;
import com.io7m.idstore.protocol.admin.IdACommandAdminUpdatePasswordExpiration;
import com.io7m.idstore.protocol.admin.IdACommandAuditSearchBegin;
import com.io7m.idstore.protocol.admin.IdACommandAuditSearchNext;
import com.io7m.idstore.protocol.admin.IdACommandAuditSearchPrevious;
import com.io7m.idstore.protocol.admin.IdACommandLogin;
import com.io7m.idstore.protocol.admin.IdACommandMailTest;
import com.io7m.idstore.protocol.admin.IdACommandMaintenanceModeSet;
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
import com.io7m.idstore.protocol.admin.IdACommandUserUpdateCredentials;
import com.io7m.idstore.protocol.admin.IdACommandUserUpdatePasswordExpiration;
import com.io7m.idstore.protocol.admin.IdAMessageType;
import com.io7m.idstore.protocol.admin.IdAPasswordExpirationSetNever;
import com.io7m.idstore.protocol.admin.IdAPasswordExpirationSetRefresh;
import com.io7m.idstore.protocol.admin.IdAPasswordExpirationSetSpecific;
import com.io7m.idstore.protocol.admin.IdAPasswordExpirationSetType;
import com.io7m.idstore.protocol.admin.IdAResponseAdminBanCreate;
import com.io7m.idstore.protocol.admin.IdAResponseAdminBanDelete;
import com.io7m.idstore.protocol.admin.IdAResponseAdminBanGet;
import com.io7m.idstore.protocol.admin.IdAResponseAdminCreate;
import com.io7m.idstore.protocol.admin.IdAResponseAdminDelete;
import com.io7m.idstore.protocol.admin.IdAResponseAdminGet;
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
import com.io7m.idstore.protocol.admin.IdAResponseBlame;
import com.io7m.idstore.protocol.admin.IdAResponseError;
import com.io7m.idstore.protocol.admin.IdAResponseLogin;
import com.io7m.idstore.protocol.admin.IdAResponseMailTest;
import com.io7m.idstore.protocol.admin.IdAResponseMaintenanceModeSet;
import com.io7m.idstore.protocol.admin.IdAResponseType;
import com.io7m.idstore.protocol.admin.IdAResponseUserBanCreate;
import com.io7m.idstore.protocol.admin.IdAResponseUserBanDelete;
import com.io7m.idstore.protocol.admin.IdAResponseUserBanGet;
import com.io7m.idstore.protocol.admin.IdAResponseUserCreate;
import com.io7m.idstore.protocol.admin.IdAResponseUserDelete;
import com.io7m.idstore.protocol.admin.IdAResponseUserGet;
import com.io7m.idstore.protocol.admin.IdAResponseUserLoginHistory;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchBegin;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchByEmailBegin;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchByEmailNext;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchNext;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchPrevious;
import com.io7m.idstore.protocol.admin.IdAResponseUserUpdate;
import com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationGeneral;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.api.IdProtocolMessageValidatorType;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.io7m.cedarbridge.runtime.api.CBCore.string;
import static com.io7m.cedarbridge.runtime.api.CBOptionType.fromOptional;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireAdmin;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireCommandAdminBanCreate;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireCommandAdminBanDelete;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireCommandAdminBanGet;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireCommandAdminCreate;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireCommandAdminDelete;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireCommandAdminEmailAdd;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireCommandAdminEmailRemove;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireCommandAdminGet;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireCommandAdminGetByEmail;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireCommandAdminPermissionGrant;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireCommandAdminPermissionRevoke;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireCommandAdminSearchBegin;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireCommandAdminSearchByEmailBegin;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireCommandAdminSearchByEmailNext;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireCommandAdminSearchByEmailPrevious;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireCommandAdminSearchNext;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireCommandAdminSearchPrevious;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireCommandAdminSelf;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireCommandAdminUpdateCredentials;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireResponseAdminBanCreate;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireResponseAdminBanDelete;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireResponseAdminBanGet;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireResponseAdminCreate;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireResponseAdminDelete;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireResponseAdminGet;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireResponseAdminSearchBegin;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireResponseAdminSearchByEmailBegin;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireResponseAdminSearchByEmailNext;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireResponseAdminSearchByEmailPrevious;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireResponseAdminSearchNext;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireResponseAdminSearchPrevious;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireResponseAdminSelf;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.fromWireResponseAdminUpdate;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireAdmin;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireCommandAdminBanCreate;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireCommandAdminBanDelete;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireCommandAdminBanGet;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireCommandAdminCreate;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireCommandAdminDelete;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireCommandAdminEmailAdd;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireCommandAdminEmailRemove;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireCommandAdminGet;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireCommandAdminGetByEmail;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireCommandAdminPermissionGrant;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireCommandAdminPermissionRevoke;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireCommandAdminSearchBegin;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireCommandAdminSearchByEmailBegin;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireCommandAdminSearchByEmailNext;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireCommandAdminSearchByEmailPrevious;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireCommandAdminSearchNext;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireCommandAdminSearchPrevious;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireCommandAdminSelf;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireCommandAdminUpdateCredentials;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireResponseAdminBanCreate;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireResponseAdminBanDelete;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireResponseAdminBanGet;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireResponseAdminCreate;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireResponseAdminDelete;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireResponseAdminGet;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireResponseAdminSearchBegin;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireResponseAdminSearchByEmailBegin;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireResponseAdminSearchByEmailNext;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireResponseAdminSearchByEmailPrevious;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireResponseAdminSearchNext;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireResponseAdminSearchPrevious;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireResponseAdminSelf;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationAdmin.toWireResponseAdminUpdate;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationGeneral.fromWirePage;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationGeneral.fromWireTimeRange;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationGeneral.toWirePage;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationGeneral.toWireTimeRange;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.fromWireCommandUserBanCreate;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.fromWireCommandUserBanDelete;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.fromWireCommandUserBanGet;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.fromWireCommandUserCreate;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.fromWireCommandUserDelete;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.fromWireCommandUserEmailAdd;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.fromWireCommandUserEmailRemove;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.fromWireCommandUserGet;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.fromWireCommandUserGetByEmail;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.fromWireCommandUserSearchBegin;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.fromWireCommandUserSearchByEmailBegin;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.fromWireCommandUserSearchByEmailNext;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.fromWireCommandUserSearchByEmailPrevious;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.fromWireCommandUserSearchNext;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.fromWireCommandUserSearchPrevious;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.fromWireCommandUserUpdateCredentials;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.fromWireResponseUserBanCreate;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.fromWireResponseUserBanDelete;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.fromWireResponseUserBanGet;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.fromWireResponseUserCreate;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.fromWireResponseUserDelete;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.fromWireResponseUserGet;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.fromWireResponseUserSearchBegin;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.fromWireResponseUserSearchByEmailBegin;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.fromWireResponseUserSearchByEmailNext;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.fromWireResponseUserSearchByEmailPrevious;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.fromWireResponseUserSearchNext;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.fromWireResponseUserSearchPrevious;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.fromWireResponseUserUpdate;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.toWireCommandUserBanCreate;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.toWireCommandUserBanDelete;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.toWireCommandUserBanGet;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.toWireCommandUserCreate;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.toWireCommandUserDelete;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.toWireCommandUserEmailAdd;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.toWireCommandUserEmailRemove;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.toWireCommandUserGet;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.toWireCommandUserGetByEmail;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.toWireCommandUserSearchBegin;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.toWireCommandUserSearchByEmailBegin;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.toWireCommandUserSearchByEmailNext;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.toWireCommandUserSearchByEmailPrevious;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.toWireCommandUserSearchNext;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.toWireCommandUserSearchPrevious;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.toWireCommandUserUpdateCredentials;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.toWireResponseUserBanCreate;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.toWireResponseUserBanDelete;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.toWireResponseUserBanGet;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.toWireResponseUserCreate;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.toWireResponseUserDelete;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.toWireResponseUserGet;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.toWireResponseUserSearchBegin;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.toWireResponseUserSearchByEmailBegin;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.toWireResponseUserSearchByEmailNext;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.toWireResponseUserSearchByEmailPrevious;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.toWireResponseUserSearchNext;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.toWireResponseUserSearchPrevious;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationUser.toWireResponseUserUpdate;

/**
 * Functions to translate between the core command set and the Admin v1
 * Cedarbridge encoding command set.
 */

public final class IdACB1Validation
  implements IdProtocolMessageValidatorType<IdAMessageType, ProtocolIdAv1Type>
{
  /**
   * Functions to translate between the core command set and the Admin v1
   * Cedarbridge encoding command set.
   */

  public IdACB1Validation()
  {

  }

  private static ProtocolIdAv1Type toWireResponse(
    final IdAResponseType response)
    throws IdProtocolException
  {
    if (response instanceof final IdAResponseError c) {
      return toWireResponseError(c);
    } else if (response instanceof final IdAResponseLogin c) {
      return toWireResponseLogin(c);

      /*
       * Admin commands
       */

    } else if (response instanceof final IdAResponseAdminBanCreate c) {
      return toWireResponseAdminBanCreate(c);
    } else if (response instanceof final IdAResponseAdminBanDelete c) {
      return toWireResponseAdminBanDelete(c);
    } else if (response instanceof final IdAResponseAdminBanGet c) {
      return toWireResponseAdminBanGet(c);
    } else if (response instanceof final IdAResponseAdminCreate c) {
      return toWireResponseAdminCreate(c);
    } else if (response instanceof final IdAResponseAdminDelete c) {
      return toWireResponseAdminDelete(c);
    } else if (response instanceof final IdAResponseAdminGet c) {
      return toWireResponseAdminGet(c);
    } else if (response instanceof final IdAResponseAdminSearchBegin c) {
      return toWireResponseAdminSearchBegin(c);
    } else if (response instanceof final IdAResponseAdminSearchByEmailBegin c) {
      return toWireResponseAdminSearchByEmailBegin(c);
    } else if (response instanceof final IdAResponseAdminSearchByEmailNext c) {
      return toWireResponseAdminSearchByEmailNext(c);
    } else if (response instanceof final IdAResponseAdminSearchByEmailPrevious c) {
      return toWireResponseAdminSearchByEmailPrevious(c);
    } else if (response instanceof final IdAResponseAdminSearchNext c) {
      return toWireResponseAdminSearchNext(c);
    } else if (response instanceof final IdAResponseAdminSearchPrevious c) {
      return toWireResponseAdminSearchPrevious(c);
    } else if (response instanceof final IdAResponseAdminSelf c) {
      return toWireResponseAdminSelf(c);
    } else if (response instanceof final IdAResponseAdminUpdate c) {
      return toWireResponseAdminUpdate(c);
    } else if (response instanceof final IdAResponseAuditSearchBegin c) {
      return toWireResponseAuditSearchBegin(c);
    } else if (response instanceof final IdAResponseAuditSearchNext c) {
      return toWireResponseAuditSearchNext(c);
    } else if (response instanceof final IdAResponseAuditSearchPrevious c) {
      return toWireResponseAuditSearchPrevious(c);

      /*
       * User responses.
       */

    } else if (response instanceof final IdAResponseUserBanCreate c) {
      return toWireResponseUserBanCreate(c);
    } else if (response instanceof final IdAResponseUserBanDelete c) {
      return toWireResponseUserBanDelete(c);
    } else if (response instanceof final IdAResponseUserBanGet c) {
      return toWireResponseUserBanGet(c);
    } else if (response instanceof final IdAResponseUserCreate c) {
      return toWireResponseUserCreate(c);
    } else if (response instanceof final IdAResponseUserDelete c) {
      return toWireResponseUserDelete(c);
    } else if (response instanceof final IdAResponseUserGet c) {
      return toWireResponseUserGet(c);
    } else if (response instanceof final IdAResponseUserSearchBegin c) {
      return toWireResponseUserSearchBegin(c);
    } else if (response instanceof final IdAResponseUserSearchByEmailBegin c) {
      return toWireResponseUserSearchByEmailBegin(c);
    } else if (response instanceof final IdAResponseUserSearchByEmailNext c) {
      return toWireResponseUserSearchByEmailNext(c);
    } else if (response instanceof final IdAResponseUserSearchByEmailPrevious c) {
      return toWireResponseUserSearchByEmailPrevious(c);
    } else if (response instanceof final IdAResponseUserSearchNext c) {
      return toWireResponseUserSearchNext(c);
    } else if (response instanceof final IdAResponseUserSearchPrevious c) {
      return toWireResponseUserSearchPrevious(c);
    } else if (response instanceof final IdAResponseUserUpdate c) {
      return toWireResponseUserUpdate(c);
    } else if (response instanceof final IdAResponseUserLoginHistory c) {
      return toWireResponseUserLoginHistory(c);

      /*
       * General.
       */

    } else if (response instanceof final IdAResponseMailTest c) {
      return toWireResponseMailTest(c);
    } else if (response instanceof final IdAResponseMaintenanceModeSet c) {
      return toWireResponseMaintenanceModeSet(c);
    }

    throw new IdProtocolException(
      "Unrecognized message: %s".formatted(response),
      PROTOCOL_ERROR,
      Map.of(),
      Optional.empty()
    );
  }

  private static IdA1ResponseMailTest toWireResponseMailTest(
    final IdAResponseMailTest c)
  {
    return new IdA1ResponseMailTest(
      new CBUUID(c.requestId()),
      string(c.token().value())
    );
  }

  private static IdA1ResponseMaintenanceModeSet toWireResponseMaintenanceModeSet(
    final IdAResponseMaintenanceModeSet c)
  {
    return new IdA1ResponseMaintenanceModeSet(
      new CBUUID(c.requestId()),
      string(c.message())
    );
  }

  private static IdA1ResponseUserLoginHistory toWireResponseUserLoginHistory(
    final IdAResponseUserLoginHistory c)
  {
    return new IdA1ResponseUserLoginHistory(
      new CBUUID(c.requestId()),
      CBLists.ofCollection(c.history(), IdACB1ValidationGeneral::toWireLogin)
    );
  }

  private static IdA1ResponseAuditSearchBegin toWireResponseAuditSearchBegin(
    final IdAResponseAuditSearchBegin r)
  {
    return new IdA1ResponseAuditSearchBegin(
      new CBUUID(r.requestId()),
      toWirePage(r.page(), IdACB1ValidationGeneral::toWireAuditEvent)
    );
  }

  private static IdA1ResponseAuditSearchNext toWireResponseAuditSearchNext(
    final IdAResponseAuditSearchNext r)
  {
    return new IdA1ResponseAuditSearchNext(
      new CBUUID(r.requestId()),
      toWirePage(r.page(), IdACB1ValidationGeneral::toWireAuditEvent)
    );
  }

  private static IdA1ResponseAuditSearchPrevious toWireResponseAuditSearchPrevious(
    final IdAResponseAuditSearchPrevious r)
  {
    return new IdA1ResponseAuditSearchPrevious(
      new CBUUID(r.requestId()),
      toWirePage(r.page(), IdACB1ValidationGeneral::toWireAuditEvent)
    );
  }

  private static IdA1ResponseError toWireResponseError(
    final IdAResponseError error)
  {
    return new IdA1ResponseError(
      new CBUUID(error.requestId()),
      new CBString(error.errorCode().id()),
      new CBString(error.message()),
      CBMaps.ofMapString(error.attributes()),
      fromOptional(error.remediatingAction().map(CBString::new)),
      fromBlame(error.blame())
    );
  }

  private static IdA1ResponseBlame fromBlame(
    final IdAResponseBlame blame)
  {
    return switch (blame) {
      case BLAME_CLIENT -> new IdA1ResponseBlame.BlameClient();
      case BLAME_SERVER -> new IdA1ResponseBlame.BlameServer();
    };
  }

  private static IdA1ResponseLogin toWireResponseLogin(
    final IdAResponseLogin login)
  {
    return new IdA1ResponseLogin(
      new CBUUID(login.requestId()),
      toWireAdmin(login.admin())
    );
  }

  private static ProtocolIdAv1Type convertToWireCommand(
    final IdACommandType<?> command)
    throws IdProtocolException
  {
    if (command instanceof final IdACommandLogin c) {
      return toWireCommandLogin(c);

      /*
       * Admin commands.
       */

    } else if (command instanceof final IdACommandAdminBanCreate c) {
      return toWireCommandAdminBanCreate(c);
    } else if (command instanceof final IdACommandAdminBanDelete c) {
      return toWireCommandAdminBanDelete(c);
    } else if (command instanceof final IdACommandAdminBanGet c) {
      return toWireCommandAdminBanGet(c);
    } else if (command instanceof final IdACommandAdminCreate c) {
      return toWireCommandAdminCreate(c);
    } else if (command instanceof final IdACommandAdminDelete c) {
      return toWireCommandAdminDelete(c);
    } else if (command instanceof final IdACommandAdminEmailAdd c) {
      return toWireCommandAdminEmailAdd(c);
    } else if (command instanceof final IdACommandAdminEmailRemove c) {
      return toWireCommandAdminEmailRemove(c);
    } else if (command instanceof final IdACommandAdminGet c) {
      return toWireCommandAdminGet(c);
    } else if (command instanceof final IdACommandAdminGetByEmail c) {
      return toWireCommandAdminGetByEmail(c);
    } else if (command instanceof final IdACommandAdminPermissionGrant c) {
      return toWireCommandAdminPermissionGrant(c);
    } else if (command instanceof final IdACommandAdminPermissionRevoke c) {
      return toWireCommandAdminPermissionRevoke(c);
    } else if (command instanceof final IdACommandAdminSearchBegin c) {
      return toWireCommandAdminSearchBegin(c);
    } else if (command instanceof final IdACommandAdminSearchByEmailBegin c) {
      return toWireCommandAdminSearchByEmailBegin(c);
    } else if (command instanceof final IdACommandAdminSearchByEmailNext c) {
      return toWireCommandAdminSearchByEmailNext();
    } else if (command instanceof final IdACommandAdminSearchByEmailPrevious c) {
      return toWireCommandAdminSearchByEmailPrevious();
    } else if (command instanceof final IdACommandAdminSearchNext c) {
      return toWireCommandAdminSearchNext();
    } else if (command instanceof final IdACommandAdminSearchPrevious c) {
      return toWireCommandAdminSearchPrevious();
    } else if (command instanceof final IdACommandAdminSelf c) {
      return toWireCommandAdminSelf();
    } else if (command instanceof final IdACommandAdminUpdateCredentials c) {
      return toWireCommandAdminUpdateCredentials(c);
    } else if (command instanceof final IdACommandAdminUpdatePasswordExpiration c) {
      return toWireCommandAdminUpdatePasswordExpiration(c);
    } else if (command instanceof final IdACommandAuditSearchBegin c) {
      return toWireCommandAuditSearchBegin(c);
    } else if (command instanceof final IdACommandAuditSearchNext c) {
      return toWireCommandAuditSearchNext();
    } else if (command instanceof final IdACommandAuditSearchPrevious c) {
      return toWireCommandAuditSearchPrevious();

      /*
       * User commands.
       */

    } else if (command instanceof final IdACommandUserBanCreate c) {
      return toWireCommandUserBanCreate(c);
    } else if (command instanceof final IdACommandUserBanDelete c) {
      return toWireCommandUserBanDelete(c);
    } else if (command instanceof final IdACommandUserBanGet c) {
      return toWireCommandUserBanGet(c);
    } else if (command instanceof final IdACommandUserCreate c) {
      return toWireCommandUserCreate(c);
    } else if (command instanceof final IdACommandUserDelete c) {
      return toWireCommandUserDelete(c);
    } else if (command instanceof final IdACommandUserEmailAdd c) {
      return toWireCommandUserEmailAdd(c);
    } else if (command instanceof final IdACommandUserEmailRemove c) {
      return toWireCommandUserEmailRemove(c);
    } else if (command instanceof final IdACommandUserGet c) {
      return toWireCommandUserGet(c);
    } else if (command instanceof final IdACommandUserGetByEmail c) {
      return toWireCommandUserGetByEmail(c);
    } else if (command instanceof final IdACommandUserSearchBegin c) {
      return toWireCommandUserSearchBegin(c);
    } else if (command instanceof final IdACommandUserSearchByEmailBegin c) {
      return toWireCommandUserSearchByEmailBegin(c);
    } else if (command instanceof final IdACommandUserSearchByEmailNext c) {
      return toWireCommandUserSearchByEmailNext();
    } else if (command instanceof final IdACommandUserSearchByEmailPrevious c) {
      return toWireCommandUserSearchByEmailPrevious();
    } else if (command instanceof final IdACommandUserSearchNext c) {
      return toWireCommandUserSearchNext();
    } else if (command instanceof final IdACommandUserSearchPrevious c) {
      return toWireCommandUserSearchPrevious();
    } else if (command instanceof final IdACommandUserUpdateCredentials c) {
      return toWireCommandUserUpdateCredentials(c);
    } else if (command instanceof final IdACommandUserLoginHistory c) {
      return toWireCommandUserLoginHistory(c);
    } else if (command instanceof final IdACommandUserUpdatePasswordExpiration c) {
      return toWireCommandUserUpdatePasswordExpiration(c);

      /*
       * Other commands.
       */

    } else if (command instanceof final IdACommandMailTest c) {
      return toWireCommandMailTest(c);
    } else if (command instanceof final IdACommandMaintenanceModeSet c) {
      return toWireCommandMaintenanceModeSet(c);
    }

    throw new IdProtocolException(
      "Unrecognized message: %s".formatted(command),
      PROTOCOL_ERROR,
      Map.of(),
      Optional.empty()
    );
  }

  private static ProtocolIdAv1Type toWireCommandMaintenanceModeSet(
    final IdACommandMaintenanceModeSet c)
  {
    return new IdA1CommandMaintenanceModeSet(
      fromOptional(c.message().map(CBCore::string))
    );
  }

  private static ProtocolIdAv1Type toWireCommandMailTest(
    final IdACommandMailTest c)
  {
    return new IdA1CommandMailTest(
      string(c.address().value()),
      string(c.token().value())
    );
  }

  private static IdA1CommandUserUpdatePasswordExpiration
  toWireCommandUserUpdatePasswordExpiration(
    final IdACommandUserUpdatePasswordExpiration c)
  {
    return new IdA1CommandUserUpdatePasswordExpiration(
      new CBUUID(c.user()),
      toWirePasswordExpirationSet(c.set())
    );
  }

  private static IdA1CommandAdminUpdatePasswordExpiration
  toWireCommandAdminUpdatePasswordExpiration(
    final IdACommandAdminUpdatePasswordExpiration c)
  {
    return new IdA1CommandAdminUpdatePasswordExpiration(
      new CBUUID(c.user()),
      toWirePasswordExpirationSet(c.set())
    );
  }

  private static IdA1PasswordExpirationSet toWirePasswordExpirationSet(
    final IdAPasswordExpirationSetType set)
  {
    if (set instanceof IdAPasswordExpirationSetNever) {
      return new IdA1PasswordExpirationSet.Never();
    }
    if (set instanceof IdAPasswordExpirationSetRefresh) {
      return new IdA1PasswordExpirationSet.Refresh();
    }
    if (set instanceof final IdAPasswordExpirationSetSpecific s) {
      return new IdA1PasswordExpirationSet.Specific(
        new CBOffsetDateTime(s.time())
      );
    }
    throw new IllegalStateException("Unrecognized set type: " + set);
  }

  private static IdA1CommandUserLoginHistory toWireCommandUserLoginHistory(
    final IdACommandUserLoginHistory c)
  {
    return new IdA1CommandUserLoginHistory(
      new CBUUID(c.user())
    );
  }

  private static IdA1CommandAuditSearchNext toWireCommandAuditSearchNext()
  {
    return new IdA1CommandAuditSearchNext();
  }

  private static IdA1CommandAuditSearchPrevious toWireCommandAuditSearchPrevious()
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
      new CBString(login.password()),
      CBMaps.ofMapString(login.metadata())
    );
  }

  private static IdAResponseError fromWireResponseError(
    final IdA1ResponseError error)
    throws IdProtocolException
  {
    return new IdAResponseError(
      error.fieldRequestId().value(),
      error.fieldMessage().value(),
      new IdErrorCode(error.fieldErrorCode().value()),
      CBMaps.toMapString(error.fieldAttributes()),
      error.fieldRemediatingAction()
        .asOptional()
        .map(CBString::value),
      fromWireBlame(error.fieldBlame())
    );
  }

  private static IdAResponseBlame fromWireBlame(
    final IdA1ResponseBlame blame)
    throws IdProtocolException
  {
    if (blame instanceof IdA1ResponseBlame.BlameClient) {
      return IdAResponseBlame.BLAME_CLIENT;
    }
    if (blame instanceof IdA1ResponseBlame.BlameServer) {
      return IdAResponseBlame.BLAME_SERVER;
    }
    throw new IdProtocolException(
      "Unrecognized blame: %s".formatted(blame),
      PROTOCOL_ERROR,
      Map.of(),
      Optional.empty()
    );
  }

  private static IdAResponseLogin fromWireResponseLogin(
    final IdA1ResponseLogin login)
    throws IdProtocolException, IdPasswordException
  {
    return new IdAResponseLogin(
      login.fieldRequestId().value(),
      fromWireAdmin(login.fieldAdmin())
    );
  }

  private static IdACommandLogin fromWireCommandLogin(
    final IdA1CommandLogin login)
  {
    return new IdACommandLogin(
      new IdName(login.fieldUserName().value()),
      login.fieldPassword().value(),
      fromWireCommandLoginMetadata(login.fieldMetadata())
    );
  }

  private static Map<String, String> fromWireCommandLoginMetadata(
    final CBMap<CBString, CBString> map)
  {
    return map.values()
      .entrySet()
      .stream()
      .map(e -> Map.entry(e.getKey().value(), e.getValue().value()))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  public ProtocolIdAv1Type convertToWire(
    final IdAMessageType message)
    throws IdProtocolException
  {
    if (message instanceof final IdACommandType<?> command) {
      return convertToWireCommand(command);
    } else if (message instanceof final IdAResponseType response) {
      return toWireResponse(response);
    } else {
      throw new IdProtocolException(
        "Unrecognized message: %s".formatted(message),
        PROTOCOL_ERROR,
        Map.of(),
        Optional.empty()
      );
    }
  }

  @Override
  public IdAMessageType convertFromWire(
    final ProtocolIdAv1Type message)
    throws IdProtocolException
  {
    try {
      if (message instanceof final IdA1CommandLogin c) {
        return fromWireCommandLogin(c);
      } else if (message instanceof final IdA1ResponseLogin c) {
        return fromWireResponseLogin(c);
      } else if (message instanceof final IdA1ResponseError c) {
        return fromWireResponseError(c);

        /*
         * Admin commands.
         */

      } else if (message instanceof final IdA1CommandAdminBanCreate c) {
        return fromWireCommandAdminBanCreate(c);
      } else if (message instanceof final IdA1CommandAdminBanDelete c) {
        return fromWireCommandAdminBanDelete(c);
      } else if (message instanceof final IdA1CommandAdminBanGet c) {
        return fromWireCommandAdminBanGet(c);
      } else if (message instanceof final IdA1CommandAdminCreate c) {
        return fromWireCommandAdminCreate(c);
      } else if (message instanceof final IdA1CommandAdminDelete c) {
        return fromWireCommandAdminDelete(c);
      } else if (message instanceof final IdA1CommandAdminEmailAdd c) {
        return fromWireCommandAdminEmailAdd(c);
      } else if (message instanceof final IdA1CommandAdminEmailRemove c) {
        return fromWireCommandAdminEmailRemove(c);
      } else if (message instanceof final IdA1CommandAdminGet c) {
        return fromWireCommandAdminGet(c);
      } else if (message instanceof final IdA1CommandAdminGetByEmail c) {
        return fromWireCommandAdminGetByEmail(c);
      } else if (message instanceof final IdA1CommandAdminPermissionGrant c) {
        return fromWireCommandAdminPermissionGrant(c);
      } else if (message instanceof final IdA1CommandAdminPermissionRevoke c) {
        return fromWireCommandAdminPermissionRevoke(c);
      } else if (message instanceof final IdA1CommandAdminSearchBegin c) {
        return fromWireCommandAdminSearchBegin(c);
      } else if (message instanceof final IdA1CommandAdminSearchByEmailBegin c) {
        return fromWireCommandAdminSearchByEmailBegin(c);
      } else if (message instanceof final IdA1CommandAdminSearchByEmailNext c) {
        return fromWireCommandAdminSearchByEmailNext();
      } else if (message instanceof final IdA1CommandAdminSearchByEmailPrevious c) {
        return fromWireCommandAdminSearchByEmailPrevious();
      } else if (message instanceof final IdA1CommandAdminSearchNext c) {
        return fromWireCommandAdminSearchNext();
      } else if (message instanceof final IdA1CommandAdminSearchPrevious c) {
        return fromWireCommandAdminSearchPrevious();
      } else if (message instanceof final IdA1CommandAdminSelf c) {
        return fromWireCommandAdminSelf();
      } else if (message instanceof final IdA1CommandAdminUpdateCredentials c) {
        return fromWireCommandAdminUpdateCredentials(c);
      } else if (message instanceof final IdA1CommandAdminUpdatePasswordExpiration c) {
        return fromWireCommandAdminUpdatePasswordExpiration(c);
      } else if (message instanceof final IdA1CommandAuditSearchBegin c) {
        return fromWireCommandAuditSearchBegin(c);
      } else if (message instanceof final IdA1CommandAuditSearchNext c) {
        return fromWireCommandAuditSearchNext();
      } else if (message instanceof final IdA1CommandAuditSearchPrevious c) {
        return fromWireCommandAuditSearchPrevious();

        /*
         * Admin responses.
         */

      } else if (message instanceof final IdA1ResponseAdminBanCreate c) {
        return fromWireResponseAdminBanCreate(c);
      } else if (message instanceof final IdA1ResponseAdminBanDelete c) {
        return fromWireResponseAdminBanDelete(c);
      } else if (message instanceof final IdA1ResponseAdminBanGet c) {
        return fromWireResponseAdminBanGet(c);
      } else if (message instanceof final IdA1ResponseAdminCreate c) {
        return fromWireResponseAdminCreate(c);
      } else if (message instanceof final IdA1ResponseAdminDelete c) {
        return fromWireResponseAdminDelete(c);
      } else if (message instanceof final IdA1ResponseAdminGet c) {
        return fromWireResponseAdminGet(c);
      } else if (message instanceof final IdA1ResponseAdminSearchBegin c) {
        return fromWireResponseAdminSearchBegin(c);
      } else if (message instanceof final IdA1ResponseAdminSearchByEmailBegin c) {
        return fromWireResponseAdminSearchByEmailBegin(c);
      } else if (message instanceof final IdA1ResponseAdminSearchByEmailNext c) {
        return fromWireResponseAdminSearchByEmailNext(c);
      } else if (message instanceof final IdA1ResponseAdminSearchByEmailPrevious c) {
        return fromWireResponseAdminSearchByEmailPrevious(c);
      } else if (message instanceof final IdA1ResponseAdminSearchNext c) {
        return fromWireResponseAdminSearchNext(c);
      } else if (message instanceof final IdA1ResponseAdminSearchPrevious c) {
        return fromWireResponseAdminSearchPrevious(c);
      } else if (message instanceof final IdA1ResponseAdminSelf c) {
        return fromWireResponseAdminSelf(c);
      } else if (message instanceof final IdA1ResponseAdminUpdate c) {
        return fromWireResponseAdminUpdate(c);

        /*
         * Audit responses.
         */

      } else if (message instanceof final IdA1ResponseAuditSearchBegin c) {
        return fromWireResponseAuditSearchBegin(c);
      } else if (message instanceof final IdA1ResponseAuditSearchNext c) {
        return fromWireResponseAuditSearchNext(c);
      } else if (message instanceof final IdA1ResponseAuditSearchPrevious c) {
        return fromWireResponseAuditSearchPrevious(c);

        /*
         * User commands.
         */

      } else if (message instanceof final IdA1CommandUserBanCreate c) {
        return fromWireCommandUserBanCreate(c);
      } else if (message instanceof final IdA1CommandUserBanDelete c) {
        return fromWireCommandUserBanDelete(c);
      } else if (message instanceof final IdA1CommandUserBanGet c) {
        return fromWireCommandUserBanGet(c);
      } else if (message instanceof final IdA1CommandUserCreate c) {
        return fromWireCommandUserCreate(c);
      } else if (message instanceof final IdA1CommandUserDelete c) {
        return fromWireCommandUserDelete(c);
      } else if (message instanceof final IdA1CommandUserEmailAdd c) {
        return fromWireCommandUserEmailAdd(c);
      } else if (message instanceof final IdA1CommandUserEmailRemove c) {
        return fromWireCommandUserEmailRemove(c);
      } else if (message instanceof final IdA1CommandUserGet c) {
        return fromWireCommandUserGet(c);
      } else if (message instanceof final IdA1CommandUserGetByEmail c) {
        return fromWireCommandUserGetByEmail(c);
      } else if (message instanceof final IdA1CommandUserSearchBegin c) {
        return fromWireCommandUserSearchBegin(c);
      } else if (message instanceof final IdA1CommandUserSearchByEmailBegin c) {
        return fromWireCommandUserSearchByEmailBegin(c);
      } else if (message instanceof final IdA1CommandUserSearchByEmailNext c) {
        return fromWireCommandUserSearchByEmailNext();
      } else if (message instanceof final IdA1CommandUserSearchByEmailPrevious c) {
        return fromWireCommandUserSearchByEmailPrevious();
      } else if (message instanceof final IdA1CommandUserSearchNext c) {
        return fromWireCommandUserSearchNext();
      } else if (message instanceof final IdA1CommandUserSearchPrevious c) {
        return fromWireCommandUserSearchPrevious();
      } else if (message instanceof final IdA1CommandUserUpdateCredentials c) {
        return fromWireCommandUserUpdateCredentials(c);
      } else if (message instanceof final IdA1CommandUserUpdatePasswordExpiration c) {
        return fromWireCommandUserUpdatePasswordExpiration(c);
      } else if (message instanceof final IdA1CommandUserLoginHistory c) {
        return fromWireCommandUserLoginHistory(c);

        /*
         * User responses.
         */

      } else if (message instanceof final IdA1ResponseUserBanCreate c) {
        return fromWireResponseUserBanCreate(c);
      } else if (message instanceof final IdA1ResponseUserBanDelete c) {
        return fromWireResponseUserBanDelete(c);
      } else if (message instanceof final IdA1ResponseUserBanGet c) {
        return fromWireResponseUserBanGet(c);
      } else if (message instanceof final IdA1ResponseUserCreate c) {
        return fromWireResponseUserCreate(c);
      } else if (message instanceof final IdA1ResponseUserDelete c) {
        return fromWireResponseUserDelete(c);
      } else if (message instanceof final IdA1ResponseUserGet c) {
        return fromWireResponseUserGet(c);
      } else if (message instanceof final IdA1ResponseUserSearchBegin c) {
        return fromWireResponseUserSearchBegin(c);
      } else if (message instanceof final IdA1ResponseUserSearchByEmailBegin c) {
        return fromWireResponseUserSearchByEmailBegin(c);
      } else if (message instanceof final IdA1ResponseUserSearchByEmailNext c) {
        return fromWireResponseUserSearchByEmailNext(c);
      } else if (message instanceof final IdA1ResponseUserSearchByEmailPrevious c) {
        return fromWireResponseUserSearchByEmailPrevious(c);
      } else if (message instanceof final IdA1ResponseUserSearchNext c) {
        return fromWireResponseUserSearchNext(c);
      } else if (message instanceof final IdA1ResponseUserSearchPrevious c) {
        return fromWireResponseUserSearchPrevious(c);
      } else if (message instanceof final IdA1ResponseUserUpdate c) {
        return fromWireResponseUserUpdate(c);
      } else if (message instanceof final IdA1ResponseUserLoginHistory c) {
        return fromWireResponseUserLoginHistory(c);

        /*
         * Other commands/responses.
         */

      } else if (message instanceof final IdA1ResponseMailTest c) {
        return fromWireResponseMailTest(c);
      } else if (message instanceof final IdA1CommandMailTest c) {
        return fromWireCommandMailTest(c);
      } else if (message instanceof final IdA1ResponseMaintenanceModeSet c) {
        return fromWireResponseMaintenanceModeSet(c);
      } else if (message instanceof final IdA1CommandMaintenanceModeSet c) {
        return fromWireCommandMaintenanceModeSet(c);
      }

    } catch (final Exception e) {
      throw new IdProtocolException(
        Objects.requireNonNullElse(
          e.getMessage(),
          e.getClass().getSimpleName()),
        e,
        PROTOCOL_ERROR,
        Map.of(),
        Optional.empty()
      );
    }

    throw new IdProtocolException(
      "Unrecognized message: %s".formatted(message),
      PROTOCOL_ERROR,
      Map.of(),
      Optional.empty()
    );
  }

  private static IdAMessageType fromWireCommandMaintenanceModeSet(
    final IdA1CommandMaintenanceModeSet c)
  {
    return new IdACommandMaintenanceModeSet(
      c.fieldMessage()
        .asOptional().map(CBString::value)
    );
  }

  private static IdAMessageType fromWireResponseMaintenanceModeSet(
    final IdA1ResponseMaintenanceModeSet c)
  {
    return new IdAResponseMaintenanceModeSet(
      c.fieldRequestId().value(),
      c.fieldMessage().value()
    );
  }

  private static IdACommandMailTest fromWireCommandMailTest(
    final IdA1CommandMailTest c)
  {
    return new IdACommandMailTest(
      new IdEmail(c.fieldAddress().value()),
      new IdShortHumanToken(c.fieldToken().value())
    );
  }

  private static IdAResponseMailTest fromWireResponseMailTest(
    final IdA1ResponseMailTest c)
  {
    return new IdAResponseMailTest(
      c.fieldRequestId().value(),
      new IdShortHumanToken(c.fieldToken().value())
    );
  }

  private static IdAMessageType fromWireCommandAdminUpdatePasswordExpiration(
    final IdA1CommandAdminUpdatePasswordExpiration c)
  {
    return new IdACommandAdminUpdatePasswordExpiration(
      c.fieldUserId().value(),
      fromWirePasswordExpirationSet(c.fieldSet())
    );
  }

  private static IdAPasswordExpirationSetType fromWirePasswordExpirationSet(
    final IdA1PasswordExpirationSet set)
  {
    if (set instanceof IdA1PasswordExpirationSet.Never) {
      return new IdAPasswordExpirationSetNever();
    }
    if (set instanceof IdA1PasswordExpirationSet.Refresh) {
      return new IdAPasswordExpirationSetRefresh();
    }
    if (set instanceof final IdA1PasswordExpirationSet.Specific s) {
      return new IdAPasswordExpirationSetSpecific(s.fieldTime().value());
    }
    throw new IllegalStateException("Unrecognized set type: " + set);
  }

  private static IdAMessageType fromWireCommandUserUpdatePasswordExpiration(
    final IdA1CommandUserUpdatePasswordExpiration c)
  {
    return new IdACommandUserUpdatePasswordExpiration(
      c.fieldUserId().value(),
      fromWirePasswordExpirationSet(c.fieldSet())
    );
  }

  private static IdACommandUserLoginHistory fromWireCommandUserLoginHistory(
    final IdA1CommandUserLoginHistory c)
  {
    return new IdACommandUserLoginHistory(
      c.fieldUserId().value()
    );
  }

  private static IdAResponseUserLoginHistory fromWireResponseUserLoginHistory(
    final IdA1ResponseUserLoginHistory c)
  {
    return new IdAResponseUserLoginHistory(
      c.fieldRequestId().value(),
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
      c.fieldRequestId().value(),
      fromWirePage(c.fieldPage(), IdACB1ValidationGeneral::fromWireAuditEvent)
    );
  }

  private static IdAResponseAuditSearchNext fromWireResponseAuditSearchNext(
    final IdA1ResponseAuditSearchNext c)
  {
    return new IdAResponseAuditSearchNext(
      c.fieldRequestId().value(),
      fromWirePage(c.fieldPage(), IdACB1ValidationGeneral::fromWireAuditEvent)
    );
  }

  private static IdAResponseAuditSearchPrevious fromWireResponseAuditSearchPrevious(
    final IdA1ResponseAuditSearchPrevious c)
  {
    return new IdAResponseAuditSearchPrevious(
      c.fieldRequestId().value(),
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

  private static IdACommandAuditSearchNext fromWireCommandAuditSearchNext()
  {
    return new IdACommandAuditSearchNext();
  }

  private static IdACommandAuditSearchPrevious fromWireCommandAuditSearchPrevious()
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

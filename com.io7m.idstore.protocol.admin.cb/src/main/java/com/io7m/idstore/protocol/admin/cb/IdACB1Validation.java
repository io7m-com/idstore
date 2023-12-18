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
import static com.io7m.idstore.protocol.admin.IdAResponseBlame.BLAME_CLIENT;
import static com.io7m.idstore.protocol.admin.IdAResponseBlame.BLAME_SERVER;
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
    return switch (response) {
      case final IdAResponseError c -> toWireResponseError(c);
      case final IdAResponseLogin c -> toWireResponseLogin(c);

      /*
       * Admin commands
       */

      case final IdAResponseAdminBanCreate c -> toWireResponseAdminBanCreate(c);
      case final IdAResponseAdminBanDelete c -> toWireResponseAdminBanDelete(c);
      case final IdAResponseAdminBanGet c -> toWireResponseAdminBanGet(c);
      case final IdAResponseAdminCreate c -> toWireResponseAdminCreate(c);
      case final IdAResponseAdminDelete c -> toWireResponseAdminDelete(c);
      case final IdAResponseAdminGet c -> toWireResponseAdminGet(c);
      case final IdAResponseAdminSearchBegin c ->
        toWireResponseAdminSearchBegin(c);
      case final IdAResponseAdminSearchByEmailBegin c ->
        toWireResponseAdminSearchByEmailBegin(c);
      case final IdAResponseAdminSearchByEmailNext c ->
        toWireResponseAdminSearchByEmailNext(c);
      case final IdAResponseAdminSearchByEmailPrevious c ->
        toWireResponseAdminSearchByEmailPrevious(c);
      case final IdAResponseAdminSearchNext c ->
        toWireResponseAdminSearchNext(c);
      case final IdAResponseAdminSearchPrevious c ->
        toWireResponseAdminSearchPrevious(c);
      case final IdAResponseAdminSelf c -> toWireResponseAdminSelf(c);
      case final IdAResponseAdminUpdate c -> toWireResponseAdminUpdate(c);
      case final IdAResponseAuditSearchBegin c ->
        toWireResponseAuditSearchBegin(c);
      case final IdAResponseAuditSearchNext c ->
        toWireResponseAuditSearchNext(c);
      case final IdAResponseAuditSearchPrevious c ->
        toWireResponseAuditSearchPrevious(c);

      /*
       * User responses.
       */

      case final IdAResponseUserBanCreate c -> toWireResponseUserBanCreate(c);
      case final IdAResponseUserBanDelete c -> toWireResponseUserBanDelete(c);
      case final IdAResponseUserBanGet c -> toWireResponseUserBanGet(c);
      case final IdAResponseUserCreate c -> toWireResponseUserCreate(c);
      case final IdAResponseUserDelete c -> toWireResponseUserDelete(c);
      case final IdAResponseUserGet c -> toWireResponseUserGet(c);
      case final IdAResponseUserSearchBegin c ->
        toWireResponseUserSearchBegin(c);
      case final IdAResponseUserSearchByEmailBegin c ->
        toWireResponseUserSearchByEmailBegin(c);
      case final IdAResponseUserSearchByEmailNext c ->
        toWireResponseUserSearchByEmailNext(c);
      case final IdAResponseUserSearchByEmailPrevious c ->
        toWireResponseUserSearchByEmailPrevious(c);
      case final IdAResponseUserSearchNext c -> toWireResponseUserSearchNext(c);
      case final IdAResponseUserSearchPrevious c ->
        toWireResponseUserSearchPrevious(c);
      case final IdAResponseUserUpdate c -> toWireResponseUserUpdate(c);
      case final IdAResponseUserLoginHistory c ->
        toWireResponseUserLoginHistory(c);

      /*
       * General.
       */

      case final IdAResponseMailTest c -> toWireResponseMailTest(c);
      case final IdAResponseMaintenanceModeSet c ->
        toWireResponseMaintenanceModeSet(c);
    };
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
    return switch (command) {
      case final IdACommandLogin c -> toWireCommandLogin(c);

      /*
       * Admin commands.
       */

      case final IdACommandAdminBanCreate c -> toWireCommandAdminBanCreate(c);
      case final IdACommandAdminBanDelete c -> toWireCommandAdminBanDelete(c);
      case final IdACommandAdminBanGet c -> toWireCommandAdminBanGet(c);
      case final IdACommandAdminCreate c -> toWireCommandAdminCreate(c);
      case final IdACommandAdminDelete c -> toWireCommandAdminDelete(c);
      case final IdACommandAdminEmailAdd c -> toWireCommandAdminEmailAdd(c);
      case final IdACommandAdminEmailRemove c ->
        toWireCommandAdminEmailRemove(c);
      case final IdACommandAdminGet c -> toWireCommandAdminGet(c);
      case final IdACommandAdminGetByEmail c -> toWireCommandAdminGetByEmail(c);
      case final IdACommandAdminPermissionGrant c ->
        toWireCommandAdminPermissionGrant(c);
      case final IdACommandAdminPermissionRevoke c ->
        toWireCommandAdminPermissionRevoke(c);
      case final IdACommandAdminSearchBegin c ->
        toWireCommandAdminSearchBegin(c);
      case final IdACommandAdminSearchByEmailBegin c ->
        toWireCommandAdminSearchByEmailBegin(c);
      case final IdACommandAdminSearchByEmailNext c ->
        toWireCommandAdminSearchByEmailNext();
      case final IdACommandAdminSearchByEmailPrevious c ->
        toWireCommandAdminSearchByEmailPrevious();
      case final IdACommandAdminSearchNext c -> toWireCommandAdminSearchNext();
      case final IdACommandAdminSearchPrevious c ->
        toWireCommandAdminSearchPrevious();
      case final IdACommandAdminSelf c -> toWireCommandAdminSelf();
      case final IdACommandAdminUpdateCredentials c ->
        toWireCommandAdminUpdateCredentials(c);
      case final IdACommandAdminUpdatePasswordExpiration c ->
        toWireCommandAdminUpdatePasswordExpiration(c);
      case final IdACommandAuditSearchBegin c ->
        toWireCommandAuditSearchBegin(c);
      case final IdACommandAuditSearchNext c -> toWireCommandAuditSearchNext();
      case final IdACommandAuditSearchPrevious c ->
        toWireCommandAuditSearchPrevious();

      /*
       * User commands.
       */

      case final IdACommandUserBanCreate c -> toWireCommandUserBanCreate(c);
      case final IdACommandUserBanDelete c -> toWireCommandUserBanDelete(c);
      case final IdACommandUserBanGet c -> toWireCommandUserBanGet(c);
      case final IdACommandUserCreate c -> toWireCommandUserCreate(c);
      case final IdACommandUserDelete c -> toWireCommandUserDelete(c);
      case final IdACommandUserEmailAdd c -> toWireCommandUserEmailAdd(c);
      case final IdACommandUserEmailRemove c -> toWireCommandUserEmailRemove(c);
      case final IdACommandUserGet c -> toWireCommandUserGet(c);
      case final IdACommandUserGetByEmail c -> toWireCommandUserGetByEmail(c);
      case final IdACommandUserSearchBegin c -> toWireCommandUserSearchBegin(c);
      case final IdACommandUserSearchByEmailBegin c ->
        toWireCommandUserSearchByEmailBegin(c);
      case final IdACommandUserSearchByEmailNext c ->
        toWireCommandUserSearchByEmailNext();
      case final IdACommandUserSearchByEmailPrevious c ->
        toWireCommandUserSearchByEmailPrevious();
      case final IdACommandUserSearchNext c -> toWireCommandUserSearchNext();
      case final IdACommandUserSearchPrevious c ->
        toWireCommandUserSearchPrevious();
      case final IdACommandUserUpdateCredentials c ->
        toWireCommandUserUpdateCredentials(c);
      case final IdACommandUserLoginHistory c ->
        toWireCommandUserLoginHistory(c);
      case final IdACommandUserUpdatePasswordExpiration c ->
        toWireCommandUserUpdatePasswordExpiration(c);

      /*
       * Other commands.
       */

      case final IdACommandMailTest c -> toWireCommandMailTest(c);
      case final IdACommandMaintenanceModeSet c ->
        toWireCommandMaintenanceModeSet(c);
    };
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
    return switch (set) {
      case final IdAPasswordExpirationSetNever m ->
        new IdA1PasswordExpirationSet.Never();
      case final IdAPasswordExpirationSetRefresh m ->
        new IdA1PasswordExpirationSet.Refresh();
      case final IdAPasswordExpirationSetSpecific s ->
        new IdA1PasswordExpirationSet.Specific(new CBOffsetDateTime(s.time()));
    };
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
  {
    return switch (blame) {
      case final IdA1ResponseBlame.BlameClient m -> BLAME_CLIENT;
      case final IdA1ResponseBlame.BlameServer m -> BLAME_SERVER;
    };
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
    return switch (message) {
      case final IdACommandType<?> command -> convertToWireCommand(command);
      case final IdAResponseType response -> toWireResponse(response);
    };
  }

  @Override
  public IdAMessageType convertFromWire(
    final ProtocolIdAv1Type message)
    throws IdProtocolException
  {
    try {
      return switch (message) {
        case final IdA1CommandLogin c -> fromWireCommandLogin(c);
        case final IdA1ResponseLogin c -> fromWireResponseLogin(c);
        case final IdA1ResponseError c -> fromWireResponseError(c);

        /*
         * Admin commands.
         */

        case final IdA1CommandAdminBanCreate c ->
          fromWireCommandAdminBanCreate(c);
        case final IdA1CommandAdminBanDelete c ->
          fromWireCommandAdminBanDelete(c);
        case final IdA1CommandAdminBanGet c -> fromWireCommandAdminBanGet(c);
        case final IdA1CommandAdminCreate c -> fromWireCommandAdminCreate(c);
        case final IdA1CommandAdminDelete c -> fromWireCommandAdminDelete(c);
        case final IdA1CommandAdminEmailAdd c ->
          fromWireCommandAdminEmailAdd(c);
        case final IdA1CommandAdminEmailRemove c ->
          fromWireCommandAdminEmailRemove(c);
        case final IdA1CommandAdminGet c -> fromWireCommandAdminGet(c);
        case final IdA1CommandAdminGetByEmail c ->
          fromWireCommandAdminGetByEmail(c);
        case final IdA1CommandAdminPermissionGrant c ->
          fromWireCommandAdminPermissionGrant(c);
        case final IdA1CommandAdminPermissionRevoke c ->
          fromWireCommandAdminPermissionRevoke(c);
        case final IdA1CommandAdminSearchBegin c ->
          fromWireCommandAdminSearchBegin(c);
        case final IdA1CommandAdminSearchByEmailBegin c ->
          fromWireCommandAdminSearchByEmailBegin(c);
        case final IdA1CommandAdminSearchByEmailNext c ->
          fromWireCommandAdminSearchByEmailNext();
        case final IdA1CommandAdminSearchByEmailPrevious c ->
          fromWireCommandAdminSearchByEmailPrevious();
        case final IdA1CommandAdminSearchNext c ->
          fromWireCommandAdminSearchNext();
        case final IdA1CommandAdminSearchPrevious c ->
          fromWireCommandAdminSearchPrevious();
        case final IdA1CommandAdminSelf c -> fromWireCommandAdminSelf();
        case final IdA1CommandAdminUpdateCredentials c ->
          fromWireCommandAdminUpdateCredentials(c);
        case final IdA1CommandAdminUpdatePasswordExpiration c ->
          fromWireCommandAdminUpdatePasswordExpiration(c);
        case final IdA1CommandAuditSearchBegin c ->
          fromWireCommandAuditSearchBegin(c);
        case final IdA1CommandAuditSearchNext c ->
          fromWireCommandAuditSearchNext();
        case final IdA1CommandAuditSearchPrevious c ->
          fromWireCommandAuditSearchPrevious();

        /*
         * Admin responses.
         */

        case final IdA1ResponseAdminBanCreate c ->
          fromWireResponseAdminBanCreate(c);
        case final IdA1ResponseAdminBanDelete c ->
          fromWireResponseAdminBanDelete(c);
        case final IdA1ResponseAdminBanGet c -> fromWireResponseAdminBanGet(c);
        case final IdA1ResponseAdminCreate c -> fromWireResponseAdminCreate(c);
        case final IdA1ResponseAdminDelete c -> fromWireResponseAdminDelete(c);
        case final IdA1ResponseAdminGet c -> fromWireResponseAdminGet(c);
        case final IdA1ResponseAdminSearchBegin c ->
          fromWireResponseAdminSearchBegin(c);
        case final IdA1ResponseAdminSearchByEmailBegin c ->
          fromWireResponseAdminSearchByEmailBegin(c);
        case final IdA1ResponseAdminSearchByEmailNext c ->
          fromWireResponseAdminSearchByEmailNext(c);
        case final IdA1ResponseAdminSearchByEmailPrevious c ->
          fromWireResponseAdminSearchByEmailPrevious(c);
        case final IdA1ResponseAdminSearchNext c ->
          fromWireResponseAdminSearchNext(c);
        case final IdA1ResponseAdminSearchPrevious c ->
          fromWireResponseAdminSearchPrevious(c);
        case final IdA1ResponseAdminSelf c -> fromWireResponseAdminSelf(c);
        case final IdA1ResponseAdminUpdate c -> fromWireResponseAdminUpdate(c);

        /*
         * Audit responses.
         */

        case final IdA1ResponseAuditSearchBegin c ->
          fromWireResponseAuditSearchBegin(c);
        case final IdA1ResponseAuditSearchNext c ->
          fromWireResponseAuditSearchNext(c);
        case final IdA1ResponseAuditSearchPrevious c ->
          fromWireResponseAuditSearchPrevious(c);

        /*
         * User commands.
         */

        case final IdA1CommandUserBanCreate c ->
          fromWireCommandUserBanCreate(c);
        case final IdA1CommandUserBanDelete c ->
          fromWireCommandUserBanDelete(c);
        case final IdA1CommandUserBanGet c -> fromWireCommandUserBanGet(c);
        case final IdA1CommandUserCreate c -> fromWireCommandUserCreate(c);
        case final IdA1CommandUserDelete c -> fromWireCommandUserDelete(c);
        case final IdA1CommandUserEmailAdd c -> fromWireCommandUserEmailAdd(c);
        case final IdA1CommandUserEmailRemove c ->
          fromWireCommandUserEmailRemove(c);
        case final IdA1CommandUserGet c -> fromWireCommandUserGet(c);
        case final IdA1CommandUserGetByEmail c ->
          fromWireCommandUserGetByEmail(c);
        case final IdA1CommandUserSearchBegin c ->
          fromWireCommandUserSearchBegin(c);
        case final IdA1CommandUserSearchByEmailBegin c ->
          fromWireCommandUserSearchByEmailBegin(c);
        case final IdA1CommandUserSearchByEmailNext c ->
          fromWireCommandUserSearchByEmailNext();
        case final IdA1CommandUserSearchByEmailPrevious c ->
          fromWireCommandUserSearchByEmailPrevious();
        case final IdA1CommandUserSearchNext c ->
          fromWireCommandUserSearchNext();
        case final IdA1CommandUserSearchPrevious c ->
          fromWireCommandUserSearchPrevious();
        case final IdA1CommandUserUpdateCredentials c ->
          fromWireCommandUserUpdateCredentials(c);
        case final IdA1CommandUserUpdatePasswordExpiration c ->
          fromWireCommandUserUpdatePasswordExpiration(c);
        case final IdA1CommandUserLoginHistory c ->
          fromWireCommandUserLoginHistory(c);

        /*
         * User responses.
         */

        case final IdA1ResponseUserBanCreate c ->
          fromWireResponseUserBanCreate(c);
        case final IdA1ResponseUserBanDelete c ->
          fromWireResponseUserBanDelete(c);
        case final IdA1ResponseUserBanGet c -> fromWireResponseUserBanGet(c);
        case final IdA1ResponseUserCreate c -> fromWireResponseUserCreate(c);
        case final IdA1ResponseUserDelete c -> fromWireResponseUserDelete(c);
        case final IdA1ResponseUserGet c -> fromWireResponseUserGet(c);
        case final IdA1ResponseUserSearchBegin c ->
          fromWireResponseUserSearchBegin(c);
        case final IdA1ResponseUserSearchByEmailBegin c ->
          fromWireResponseUserSearchByEmailBegin(c);
        case final IdA1ResponseUserSearchByEmailNext c ->
          fromWireResponseUserSearchByEmailNext(c);
        case final IdA1ResponseUserSearchByEmailPrevious c ->
          fromWireResponseUserSearchByEmailPrevious(c);
        case final IdA1ResponseUserSearchNext c ->
          fromWireResponseUserSearchNext(c);
        case final IdA1ResponseUserSearchPrevious c ->
          fromWireResponseUserSearchPrevious(c);
        case final IdA1ResponseUserUpdate c -> fromWireResponseUserUpdate(c);
        case final IdA1ResponseUserLoginHistory c ->
          fromWireResponseUserLoginHistory(c);

        /*
         * Other commands/responses.
         */

        case final IdA1ResponseMailTest c -> fromWireResponseMailTest(c);
        case final IdA1CommandMailTest c -> fromWireCommandMailTest(c);
        case final IdA1ResponseMaintenanceModeSet c ->
          fromWireResponseMaintenanceModeSet(c);
        case final IdA1CommandMaintenanceModeSet c ->
          fromWireCommandMaintenanceModeSet(c);
      };
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
    return switch (set) {
      case final IdA1PasswordExpirationSet.Never never ->
        new IdAPasswordExpirationSetNever();
      case final IdA1PasswordExpirationSet.Refresh refresh ->
        new IdAPasswordExpirationSetRefresh();
      case final IdA1PasswordExpirationSet.Specific s ->
        new IdAPasswordExpirationSetSpecific(s.fieldTime().value());
    };
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
      p.fieldLimit().value()
    );
  }
}

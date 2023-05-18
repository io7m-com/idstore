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

package com.io7m.idstore.protocol.admin.cb.internal;

import com.io7m.cedarbridge.runtime.api.CBIntegerUnsigned16;
import com.io7m.cedarbridge.runtime.api.CBList;
import com.io7m.cedarbridge.runtime.api.CBOptionType;
import com.io7m.cedarbridge.runtime.api.CBSome;
import com.io7m.cedarbridge.runtime.api.CBString;
import com.io7m.cedarbridge.runtime.api.CBUUID;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.model.IdUserColumn;
import com.io7m.idstore.model.IdUserColumnOrdering;
import com.io7m.idstore.model.IdUserSearchByEmailParameters;
import com.io7m.idstore.model.IdUserSearchParameters;
import com.io7m.idstore.model.IdUserSummary;
import com.io7m.idstore.protocol.admin.IdACommandUserBanCreate;
import com.io7m.idstore.protocol.admin.IdACommandUserBanDelete;
import com.io7m.idstore.protocol.admin.IdACommandUserBanGet;
import com.io7m.idstore.protocol.admin.IdACommandUserCreate;
import com.io7m.idstore.protocol.admin.IdACommandUserDelete;
import com.io7m.idstore.protocol.admin.IdACommandUserEmailAdd;
import com.io7m.idstore.protocol.admin.IdACommandUserEmailRemove;
import com.io7m.idstore.protocol.admin.IdACommandUserGet;
import com.io7m.idstore.protocol.admin.IdACommandUserGetByEmail;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchBegin;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchByEmailBegin;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchByEmailNext;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchNext;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchPrevious;
import com.io7m.idstore.protocol.admin.IdACommandUserUpdateCredentials;
import com.io7m.idstore.protocol.admin.IdAResponseUserBanCreate;
import com.io7m.idstore.protocol.admin.IdAResponseUserBanDelete;
import com.io7m.idstore.protocol.admin.IdAResponseUserBanGet;
import com.io7m.idstore.protocol.admin.IdAResponseUserCreate;
import com.io7m.idstore.protocol.admin.IdAResponseUserDelete;
import com.io7m.idstore.protocol.admin.IdAResponseUserGet;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchBegin;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchByEmailBegin;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchByEmailNext;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchNext;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchPrevious;
import com.io7m.idstore.protocol.admin.IdAResponseUserUpdate;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandUserBanCreate;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandUserBanDelete;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandUserBanGet;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandUserCreate;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandUserDelete;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandUserEmailAdd;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandUserEmailRemove;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandUserGet;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandUserGetByEmail;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandUserSearchBegin;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandUserSearchByEmailBegin;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandUserSearchByEmailNext;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandUserSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandUserSearchNext;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandUserSearchPrevious;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandUserUpdateCredentials;
import com.io7m.idstore.protocol.admin.cb.IdA1ResponseUserBanCreate;
import com.io7m.idstore.protocol.admin.cb.IdA1ResponseUserBanDelete;
import com.io7m.idstore.protocol.admin.cb.IdA1ResponseUserBanGet;
import com.io7m.idstore.protocol.admin.cb.IdA1ResponseUserCreate;
import com.io7m.idstore.protocol.admin.cb.IdA1ResponseUserDelete;
import com.io7m.idstore.protocol.admin.cb.IdA1ResponseUserGet;
import com.io7m.idstore.protocol.admin.cb.IdA1ResponseUserSearchBegin;
import com.io7m.idstore.protocol.admin.cb.IdA1ResponseUserSearchByEmailBegin;
import com.io7m.idstore.protocol.admin.cb.IdA1ResponseUserSearchByEmailNext;
import com.io7m.idstore.protocol.admin.cb.IdA1ResponseUserSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin.cb.IdA1ResponseUserSearchNext;
import com.io7m.idstore.protocol.admin.cb.IdA1ResponseUserSearchPrevious;
import com.io7m.idstore.protocol.admin.cb.IdA1ResponseUserUpdate;
import com.io7m.idstore.protocol.admin.cb.IdA1User;
import com.io7m.idstore.protocol.admin.cb.IdA1UserColumn;
import com.io7m.idstore.protocol.admin.cb.IdA1UserColumnOrdering;
import com.io7m.idstore.protocol.admin.cb.IdA1UserSearchByEmailParameters;
import com.io7m.idstore.protocol.admin.cb.IdA1UserSearchParameters;
import com.io7m.idstore.protocol.admin.cb.IdA1UserSummary;
import com.io7m.idstore.protocol.api.IdProtocolException;

import java.util.Optional;

import static com.io7m.cedarbridge.runtime.api.CBBooleanType.fromBoolean;
import static com.io7m.cedarbridge.runtime.api.CBOptionType.fromOptional;
import static com.io7m.idstore.model.IdUserColumn.BY_ID;
import static com.io7m.idstore.model.IdUserColumn.BY_IDNAME;
import static com.io7m.idstore.model.IdUserColumn.BY_REALNAME;
import static com.io7m.idstore.model.IdUserColumn.BY_TIME_CREATED;
import static com.io7m.idstore.model.IdUserColumn.BY_TIME_UPDATED;
import static com.io7m.idstore.protocol.admin.cb.IdA1UserColumn.ByID;
import static com.io7m.idstore.protocol.admin.cb.IdA1UserColumn.ByIDName;
import static com.io7m.idstore.protocol.admin.cb.IdA1UserColumn.ByRealName;
import static com.io7m.idstore.protocol.admin.cb.IdA1UserColumn.ByTimeCreated;
import static com.io7m.idstore.protocol.admin.cb.IdA1UserColumn.ByTimeUpdated;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationGeneral.fromWireBan;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationGeneral.fromWireEmails;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationGeneral.fromWirePage;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationGeneral.fromWirePassword;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationGeneral.fromWirePasswordOptional;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationGeneral.fromWireTimeRange;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationGeneral.fromWireTimestamp;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationGeneral.toWireBan;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationGeneral.toWirePage;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationGeneral.toWirePassword;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationGeneral.toWireTimeRange;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationGeneral.toWireTimestamp;

/**
 * Functions to translate between the core command set and the Admin v1
 * Cedarbridge encoding command set.
 */

public final class IdACB1ValidationUser
{
  private IdACB1ValidationUser()
  {

  }

  public static IdA1ResponseUserSearchBegin toWireResponseUserSearchBegin(
    final IdAResponseUserSearchBegin r)
  {
    return new IdA1ResponseUserSearchBegin(
      new CBUUID(r.requestId()),
      toWirePage(r.page(), IdACB1ValidationUser::toWireUserSummary)
    );
  }

  public static IdA1ResponseUserSearchNext toWireResponseUserSearchNext(
    final IdAResponseUserSearchNext r)
  {
    return new IdA1ResponseUserSearchNext(
      new CBUUID(r.requestId()),
      toWirePage(r.page(), IdACB1ValidationUser::toWireUserSummary)
    );
  }

  public static IdA1ResponseUserSearchPrevious toWireResponseUserSearchPrevious(
    final IdAResponseUserSearchPrevious r)
  {
    return new IdA1ResponseUserSearchPrevious(
      new CBUUID(r.requestId()),
      toWirePage(r.page(), IdACB1ValidationUser::toWireUserSummary)
    );
  }

  public static IdA1ResponseUserSearchByEmailBegin toWireResponseUserSearchByEmailBegin(
    final IdAResponseUserSearchByEmailBegin r)
  {
    return new IdA1ResponseUserSearchByEmailBegin(
      new CBUUID(r.requestId()),
      toWirePage(r.page(), IdACB1ValidationUser::toWireUserSummary)
    );
  }

  public static IdA1ResponseUserSearchByEmailNext toWireResponseUserSearchByEmailNext(
    final IdAResponseUserSearchByEmailNext r)
  {
    return new IdA1ResponseUserSearchByEmailNext(
      new CBUUID(r.requestId()),
      toWirePage(r.page(), IdACB1ValidationUser::toWireUserSummary)
    );
  }

  public static IdA1ResponseUserSearchByEmailPrevious toWireResponseUserSearchByEmailPrevious(
    final IdAResponseUserSearchByEmailPrevious r)
  {
    return new IdA1ResponseUserSearchByEmailPrevious(
      new CBUUID(r.requestId()),
      toWirePage(r.page(), IdACB1ValidationUser::toWireUserSummary)
    );
  }

  private static IdA1UserSummary toWireUserSummary(
    final IdUserSummary s)
  {
    return new IdA1UserSummary(
      new CBUUID(s.id()),
      new CBString(s.idName().value()),
      new CBString(s.realName().value()),
      toWireTimestamp(s.timeCreated()),
      toWireTimestamp(s.timeUpdated())
    );
  }

  public static IdA1ResponseUserUpdate toWireResponseUserUpdate(
    final IdAResponseUserUpdate r)
  {
    return new IdA1ResponseUserUpdate(
      new CBUUID(r.requestId()),
      toWireUser(r.user())
    );
  }

  public static IdA1ResponseUserGet toWireResponseUserGet(
    final IdAResponseUserGet r)
  {
    return new IdA1ResponseUserGet(
      new CBUUID(r.requestId()),
      fromOptional(r.user().map(IdACB1ValidationUser::toWireUser))
    );
  }

  public static IdA1ResponseUserDelete toWireResponseUserDelete(
    final IdAResponseUserDelete r)
  {
    return new IdA1ResponseUserDelete(
      new CBUUID(r.requestId())
    );
  }

  public static IdA1ResponseUserCreate toWireResponseUserCreate(
    final IdAResponseUserCreate r)
  {
    return new IdA1ResponseUserCreate(
      new CBUUID(r.requestId()),
      toWireUser(r.user())
    );
  }

  private static IdA1User toWireUser(
    final IdUser admin)
  {
    return new IdA1User(
      new CBUUID(admin.id()),
      new CBString(admin.idName().value()),
      new CBString(admin.realName().value()),
      new CBList<>(
        admin.emails()
          .toList()
          .stream()
          .map(IdEmail::value)
          .map(CBString::new)
          .toList()
      ),
      toWireTimestamp(admin.timeCreated()),
      toWireTimestamp(admin.timeUpdated()),
      toWirePassword(admin.password())
    );
  }

  public static IdA1ResponseUserBanCreate toWireResponseUserBanCreate(
    final IdAResponseUserBanCreate r)
  {
    return new IdA1ResponseUserBanCreate(
      new CBUUID(r.requestId()),
      toWireBan(r.ban())
    );
  }

  public static IdA1ResponseUserBanDelete toWireResponseUserBanDelete(
    final IdAResponseUserBanDelete r)
  {
    return new IdA1ResponseUserBanDelete(
      new CBUUID(r.requestId())
    );
  }

  public static IdA1ResponseUserBanGet toWireResponseUserBanGet(
    final IdAResponseUserBanGet r)
  {
    return new IdA1ResponseUserBanGet(
      new CBUUID(r.requestId()),
      fromOptional(r.ban().map(IdACB1ValidationGeneral::toWireBan))
    );
  }

  public static IdA1CommandUserUpdateCredentials toWireCommandUserUpdateCredentials(
    final IdACommandUserUpdateCredentials c)
  {
    return new IdA1CommandUserUpdateCredentials(
      new CBUUID(c.user()),
      fromOptional(c.idName().map(IdName::value).map(CBString::new)),
      fromOptional(c.realName().map(IdRealName::value).map(CBString::new)),
      fromOptional(c.password().map(IdACB1ValidationGeneral::toWirePassword))
    );
  }

  public static IdA1CommandUserSearchByEmailNext toWireCommandUserSearchByEmailNext()
  {
    return new IdA1CommandUserSearchByEmailNext();
  }

  public static IdA1CommandUserSearchByEmailPrevious toWireCommandUserSearchByEmailPrevious()
  {
    return new IdA1CommandUserSearchByEmailPrevious();
  }

  public static IdA1CommandUserSearchByEmailBegin toWireCommandUserSearchByEmailBegin(
    final IdACommandUserSearchByEmailBegin c)
  {
    return new IdA1CommandUserSearchByEmailBegin(
      toWireUserSearchByEmailParameters(c.parameters())
    );
  }

  private static IdA1UserSearchByEmailParameters toWireUserSearchByEmailParameters(
    final IdUserSearchByEmailParameters parameters)
  {
    return new IdA1UserSearchByEmailParameters(
      toWireTimeRange(parameters.timeCreatedRange()),
      toWireTimeRange(parameters.timeUpdatedRange()),
      new CBString(parameters.search()),
      toWireUserColumnOrdering(parameters.ordering()),
      new CBIntegerUnsigned16(parameters.limit())
    );
  }

  public static IdA1CommandUserSearchNext toWireCommandUserSearchNext()
  {
    return new IdA1CommandUserSearchNext();
  }

  public static IdA1CommandUserSearchPrevious toWireCommandUserSearchPrevious()
  {
    return new IdA1CommandUserSearchPrevious();
  }

  public static IdA1CommandUserSearchBegin toWireCommandUserSearchBegin(
    final IdACommandUserSearchBegin c)
  {
    return new IdA1CommandUserSearchBegin(
      toWireUserSearchParameters(c.parameters())
    );
  }

  private static IdA1UserSearchParameters toWireUserSearchParameters(
    final IdUserSearchParameters parameters)
  {
    return new IdA1UserSearchParameters(
      toWireTimeRange(parameters.timeCreatedRange()),
      toWireTimeRange(parameters.timeUpdatedRange()),
      fromOptional(parameters.search().map(CBString::new)),
      toWireUserColumnOrdering(parameters.ordering()),
      new CBIntegerUnsigned16(parameters.limit())
    );
  }

  private static IdA1UserColumnOrdering toWireUserColumnOrdering(
    final IdUserColumnOrdering o)
  {
    return new IdA1UserColumnOrdering(
      toWireUserColumn(o.column()),
      fromBoolean(o.ascending())
    );
  }

  private static IdA1UserColumn toWireUserColumn(
    final IdUserColumn column)
  {
    return switch (column) {
      case BY_ID -> new ByID();
      case BY_IDNAME -> new ByIDName();
      case BY_REALNAME -> new ByRealName();
      case BY_TIME_CREATED -> new ByTimeCreated();
      case BY_TIME_UPDATED -> new ByTimeUpdated();
    };
  }

  public static IdA1CommandUserEmailRemove toWireCommandUserEmailRemove(
    final IdACommandUserEmailRemove c)
  {
    return new IdA1CommandUserEmailRemove(
      new CBUUID(c.user()),
      new CBString(c.email().value())
    );
  }

  public static IdA1CommandUserEmailAdd toWireCommandUserEmailAdd(
    final IdACommandUserEmailAdd c)
  {
    return new IdA1CommandUserEmailAdd(
      new CBUUID(c.user()),
      new CBString(c.email().value())
    );
  }

  public static IdA1CommandUserGet toWireCommandUserGet(
    final IdACommandUserGet c)
  {
    return new IdA1CommandUserGet(
      new CBUUID(c.user())
    );
  }

  public static IdA1CommandUserGetByEmail toWireCommandUserGetByEmail(
    final IdACommandUserGetByEmail c)
  {
    return new IdA1CommandUserGetByEmail(
      new CBString(c.email().value())
    );
  }

  public static IdA1CommandUserBanDelete toWireCommandUserBanDelete(
    final IdACommandUserBanDelete c)
  {
    return new IdA1CommandUserBanDelete(
      new CBUUID(c.user())
    );
  }

  public static IdA1CommandUserBanGet toWireCommandUserBanGet(
    final IdACommandUserBanGet c)
  {
    return new IdA1CommandUserBanGet(
      new CBUUID(c.user())
    );
  }

  public static IdA1CommandUserBanCreate toWireCommandUserBanCreate(
    final IdACommandUserBanCreate c)
  {
    return new IdA1CommandUserBanCreate(
      toWireBan(c.ban())
    );
  }

  public static IdA1CommandUserCreate toWireCommandUserCreate(
    final IdACommandUserCreate c)
  {
    return new IdA1CommandUserCreate(
      fromOptional(c.id().map(CBUUID::new)),
      new CBString(c.idName().value()),
      new CBString(c.realName().value()),
      new CBString(c.email().value()),
      toWirePassword(c.password())
    );
  }

  public static IdA1CommandUserDelete toWireCommandUserDelete(
    final IdACommandUserDelete c)
  {
    return new IdA1CommandUserDelete(
      new CBUUID(c.userId())
    );
  }

  public static IdAResponseUserBanCreate fromWireResponseUserBanCreate(
    final IdA1ResponseUserBanCreate c)
  {
    return new IdAResponseUserBanCreate(
      c.fieldRequestId().value(),
      fromWireBan(c.fieldBan())
    );
  }

  public static IdAResponseUserBanDelete fromWireResponseUserBanDelete(
    final IdA1ResponseUserBanDelete c)
  {
    return new IdAResponseUserBanDelete(
      c.fieldRequestId().value()
    );
  }

  public static IdAResponseUserBanGet fromWireResponseUserBanGet(
    final IdA1ResponseUserBanGet c)
  {
    return new IdAResponseUserBanGet(
      c.fieldRequestId().value(),
      c.fieldBan().asOptional().map(IdACB1ValidationGeneral::fromWireBan)
    );
  }

  public static IdAResponseUserCreate fromWireResponseUserCreate(
    final IdA1ResponseUserCreate c)
    throws IdProtocolException, IdPasswordException
  {
    return new IdAResponseUserCreate(
      c.fieldRequestId().value(),
      fromWireUser(c.fieldUser())
    );
  }

  public static IdAResponseUserDelete fromWireResponseUserDelete(
    final IdA1ResponseUserDelete c)
  {
    return new IdAResponseUserDelete(
      c.fieldRequestId().value()
    );
  }

  public static IdAResponseUserGet fromWireResponseUserGet(
    final IdA1ResponseUserGet c)
    throws IdProtocolException, IdPasswordException
  {
    return new IdAResponseUserGet(
      c.fieldRequestId().value(),
      fromWireUserOptional(c.fieldUser())
    );
  }

  private static Optional<IdUser> fromWireUserOptional(
    final CBOptionType<IdA1User> fieldUser)
    throws IdProtocolException, IdPasswordException
  {
    if (fieldUser instanceof CBSome<IdA1User> some) {
      return Optional.of(fromWireUser(some.value()));
    }
    return Optional.empty();
  }

  public static IdAResponseUserSearchBegin fromWireResponseUserSearchBegin(
    final IdA1ResponseUserSearchBegin c)
  {
    return new IdAResponseUserSearchBegin(
      c.fieldRequestId().value(),
      fromWirePage(c.fieldPage(), IdACB1ValidationUser::fromWireUserSummary)
    );
  }

  public static IdAResponseUserSearchByEmailBegin fromWireResponseUserSearchByEmailBegin(
    final IdA1ResponseUserSearchByEmailBegin c)
  {
    return new IdAResponseUserSearchByEmailBegin(
      c.fieldRequestId().value(),
      fromWirePage(c.fieldPage(), IdACB1ValidationUser::fromWireUserSummary)
    );
  }

  public static IdAResponseUserSearchByEmailNext fromWireResponseUserSearchByEmailNext(
    final IdA1ResponseUserSearchByEmailNext c)
  {
    return new IdAResponseUserSearchByEmailNext(
      c.fieldRequestId().value(),
      fromWirePage(c.fieldPage(), IdACB1ValidationUser::fromWireUserSummary)
    );
  }

  public static IdAResponseUserSearchByEmailPrevious fromWireResponseUserSearchByEmailPrevious(
    final IdA1ResponseUserSearchByEmailPrevious c)
  {
    return new IdAResponseUserSearchByEmailPrevious(
      c.fieldRequestId().value(),
      fromWirePage(c.fieldPage(), IdACB1ValidationUser::fromWireUserSummary)
    );
  }

  public static IdAResponseUserSearchNext fromWireResponseUserSearchNext(
    final IdA1ResponseUserSearchNext c)
  {
    return new IdAResponseUserSearchNext(
      c.fieldRequestId().value(),
      fromWirePage(c.fieldPage(), IdACB1ValidationUser::fromWireUserSummary)
    );
  }

  public static IdAResponseUserSearchPrevious fromWireResponseUserSearchPrevious(
    final IdA1ResponseUserSearchPrevious c)
  {
    return new IdAResponseUserSearchPrevious(
      c.fieldRequestId().value(),
      fromWirePage(c.fieldPage(), IdACB1ValidationUser::fromWireUserSummary)
    );
  }

  public static IdAResponseUserUpdate fromWireResponseUserUpdate(
    final IdA1ResponseUserUpdate c)
    throws IdProtocolException, IdPasswordException
  {
    return new IdAResponseUserUpdate(
      c.fieldRequestId().value(),
      fromWireUser(c.fieldUser())
    );
  }

  public static IdACommandUserBanCreate fromWireCommandUserBanCreate(
    final IdA1CommandUserBanCreate c)
  {
    return new IdACommandUserBanCreate(
      fromWireBan(c.fieldBan())
    );
  }

  public static IdACommandUserBanDelete fromWireCommandUserBanDelete(
    final IdA1CommandUserBanDelete c)
  {
    return new IdACommandUserBanDelete(
      c.fieldUserId().value()
    );
  }

  public static IdACommandUserBanGet fromWireCommandUserBanGet(
    final IdA1CommandUserBanGet c)
  {
    return new IdACommandUserBanGet(
      c.fieldUserId().value()
    );
  }

  public static IdACommandUserCreate fromWireCommandUserCreate(
    final IdA1CommandUserCreate c)
    throws IdPasswordException
  {
    return new IdACommandUserCreate(
      c.fieldUserId().asOptional().map(CBUUID::value),
      new IdName(c.fieldIdName().value()),
      new IdRealName(c.fieldRealName().value()),
      new IdEmail(c.fieldEmail().value()),
      fromWirePassword(c.fieldPassword())
    );
  }

  public static IdACommandUserDelete fromWireCommandUserDelete(
    final IdA1CommandUserDelete c)
  {
    return new IdACommandUserDelete(
      c.fieldUserId().value()
    );
  }

  public static IdACommandUserEmailAdd fromWireCommandUserEmailAdd(
    final IdA1CommandUserEmailAdd c)
  {
    return new IdACommandUserEmailAdd(
      c.fieldUserId().value(),
      new IdEmail(c.fieldEmail().value())
    );
  }

  public static IdACommandUserEmailRemove fromWireCommandUserEmailRemove(
    final IdA1CommandUserEmailRemove c)
  {
    return new IdACommandUserEmailRemove(
      c.fieldUserId().value(),
      new IdEmail(c.fieldEmail().value())
    );
  }

  public static IdACommandUserGet fromWireCommandUserGet(
    final IdA1CommandUserGet c)
  {
    return new IdACommandUserGet(
      c.fieldUserId().value()
    );
  }

  public static IdACommandUserGetByEmail fromWireCommandUserGetByEmail(
    final IdA1CommandUserGetByEmail c)
  {
    return new IdACommandUserGetByEmail(
      new IdEmail(c.fieldEmail().value())
    );
  }

  public static IdACommandUserSearchBegin fromWireCommandUserSearchBegin(
    final IdA1CommandUserSearchBegin c)
  {
    return new IdACommandUserSearchBegin(
      fromWireUserSearchParameters(c.fieldParameters())
    );
  }

  public static IdACommandUserSearchNext fromWireCommandUserSearchNext()
  {
    return new IdACommandUserSearchNext();
  }

  public static IdACommandUserSearchPrevious fromWireCommandUserSearchPrevious()
  {
    return new IdACommandUserSearchPrevious();
  }

  public static IdACommandUserSearchByEmailBegin fromWireCommandUserSearchByEmailBegin(
    final IdA1CommandUserSearchByEmailBegin c)
  {
    return new IdACommandUserSearchByEmailBegin(
      fromWireUserSearchByEmailParameters(c.fieldParameters())
    );
  }

  public static IdACommandUserSearchByEmailNext fromWireCommandUserSearchByEmailNext()
  {
    return new IdACommandUserSearchByEmailNext();
  }

  public static IdACommandUserSearchByEmailPrevious fromWireCommandUserSearchByEmailPrevious()
  {
    return new IdACommandUserSearchByEmailPrevious();
  }

  public static IdACommandUserUpdateCredentials fromWireCommandUserUpdateCredentials(
    final IdA1CommandUserUpdateCredentials c)
    throws IdPasswordException
  {
    return new IdACommandUserUpdateCredentials(
      c.fieldUserId().value(),
      c.fieldIdName().asOptional().map(n -> new IdName(n.value())),
      c.fieldRealName().asOptional().map(n -> new IdRealName(n.value())),
      fromWirePasswordOptional(c.fieldPassword())
    );
  }

  private static IdUserSearchByEmailParameters fromWireUserSearchByEmailParameters(
    final IdA1UserSearchByEmailParameters p)
  {
    return new IdUserSearchByEmailParameters(
      fromWireTimeRange(p.fieldTimeCreatedRange()),
      fromWireTimeRange(p.fieldTimeUpdatedRange()),
      p.fieldSearch().value(),
      fromWireUserColumnOrdering(p.fieldOrdering()),
      p.fieldLimit().value()
    );
  }

  private static IdUserSearchParameters fromWireUserSearchParameters(
    final IdA1UserSearchParameters p)
  {
    return new IdUserSearchParameters(
      fromWireTimeRange(p.fieldTimeCreatedRange()),
      fromWireTimeRange(p.fieldTimeUpdatedRange()),
      p.fieldSearch().asOptional().map(CBString::value),
      fromWireUserColumnOrdering(p.fieldOrdering()),
      p.fieldLimit().value()
    );
  }

  private static IdUserColumnOrdering fromWireUserColumnOrdering(
    final IdA1UserColumnOrdering o)
  {
    return new IdUserColumnOrdering(
      fromWireUserColumn(o.fieldColumn()),
      o.fieldAscending().asBoolean()
    );
  }

  private static IdUserColumn fromWireUserColumn(
    final IdA1UserColumn c)
  {
    if (c instanceof ByID) {
      return BY_ID;
    } else if (c instanceof ByIDName) {
      return BY_IDNAME;
    } else if (c instanceof ByRealName) {
      return BY_REALNAME;
    } else if (c instanceof ByTimeCreated) {
      return BY_TIME_CREATED;
    } else if (c instanceof ByTimeUpdated) {
      return BY_TIME_UPDATED;
    }

    throw new IllegalArgumentException(
      "Unrecognized user column: %s".formatted(c)
    );
  }

  private static IdUserSummary fromWireUserSummary(
    final IdA1UserSummary i)
  {
    return new IdUserSummary(
      i.fieldId().value(),
      new IdName(i.fieldIdName().value()),
      new IdRealName(i.fieldRealName().value()),
      fromWireTimestamp(i.fieldTimeCreated()),
      fromWireTimestamp(i.fieldTimeUpdated())
    );
  }

  private static IdUser fromWireUser(
    final IdA1User fieldUser)
    throws IdPasswordException, IdProtocolException
  {
    return new IdUser(
      fieldUser.fieldId().value(),
      new IdName(fieldUser.fieldIdName().value()),
      new IdRealName(fieldUser.fieldRealName().value()),
      fromWireEmails(fieldUser.fieldEmails()),
      fromWireTimestamp(fieldUser.fieldTimeCreated()),
      fromWireTimestamp(fieldUser.fieldTimeUpdated()),
      fromWirePassword(fieldUser.fieldPassword())
    );
  }
}

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

package com.io7m.idstore.admin_client.internal;

import com.io7m.idstore.admin_client.api.IdAClientException;
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdAdminPermission;
import com.io7m.idstore.model.IdAdminPermissionSet;
import com.io7m.idstore.model.IdAdminSearchByEmailParameters;
import com.io7m.idstore.model.IdAdminSearchParameters;
import com.io7m.idstore.model.IdAdminSummary;
import com.io7m.idstore.model.IdAuditEvent;
import com.io7m.idstore.model.IdBan;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdLogin;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPage;
import com.io7m.idstore.model.IdPassword;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdTimeRange;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.model.IdUserSearchByEmailParameters;
import com.io7m.idstore.model.IdUserSearchParameters;
import com.io7m.idstore.model.IdUserSummary;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.NOT_LOGGED_IN;

/**
 * The "disconnected" protocol handler.
 */

public final class IdAClientProtocolHandlerDisconnected
  implements IdAClientProtocolHandlerType
{
  private final HttpClient httpClient;
  private final Locale locale;
  private final IdAStrings strings;

  /**
   * The "disconnected" protocol handler.
   *
   * @param inLocale     The locale
   * @param inStrings    The string resources
   * @param inHttpClient The HTTP client
   */

  public IdAClientProtocolHandlerDisconnected(
    final Locale inLocale,
    final IdAStrings inStrings,
    final HttpClient inHttpClient)
  {
    this.locale =
      Objects.requireNonNull(inLocale, "locale");
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.httpClient =
      Objects.requireNonNull(inHttpClient, "httpClient");
  }

  @Override
  public IdAClientProtocolHandlerType login(
    final String admin,
    final String password,
    final URI base)
    throws IdAClientException, InterruptedException
  {
    return IdAProtocolNegotiation.negotiateProtocolHandler(
      this.locale,
      this.httpClient,
      this.strings,
      admin,
      password,
      base
    );
  }

  private IdAClientException notLoggedIn()
  {
    return new IdAClientException(
      NOT_LOGGED_IN,
      this.strings.format("notLoggedIn")
    );
  }

  @Override
  public IdAdmin adminSelf()
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public IdPage<IdAdminSummary> adminSearchBegin(
    final IdAdminSearchParameters parameters)
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public IdPage<IdAdminSummary> adminSearchNext()
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public IdPage<IdAdminSummary> adminSearchPrevious()
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public IdPage<IdAdminSummary> adminSearchByEmailBegin(
    final IdAdminSearchByEmailParameters parameters)
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public IdPage<IdAdminSummary> adminSearchByEmailNext()
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public IdPage<IdAdminSummary> adminSearchByEmailPrevious()
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public Optional<IdAdmin> adminGet(
    final UUID id)
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public Optional<IdAdmin> adminGetByEmail(
    final IdEmail email)
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public IdAdmin adminUpdate(
    final UUID admin,
    final Optional<IdName> idName,
    final Optional<IdRealName> realName,
    final Optional<IdPassword> password)
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public IdAdmin adminCreate(
    final Optional<UUID> id,
    final IdName idName,
    final IdRealName realName,
    final IdEmail email,
    final IdPassword password,
    final IdAdminPermissionSet permissions)
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public void adminDelete(final UUID id)
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public IdAdmin adminEmailAdd(
    final UUID id,
    final IdEmail email)
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public IdAdmin adminEmailRemove(
    final UUID id,
    final IdEmail email)
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public IdAdmin adminPermissionGrant(
    final UUID id,
    final IdAdminPermission permission)
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public IdAdmin adminPermissionRevoke(
    final UUID id,
    final IdAdminPermission permission)
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public void adminBanCreate(final IdBan ban)
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public Optional<IdBan> adminBanGet(final UUID id)
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public void adminBanDelete(final IdBan ban)
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public IdPage<IdUserSummary> userSearchBegin(
    final IdUserSearchParameters parameters)
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public IdPage<IdUserSummary> userSearchNext()
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public IdPage<IdUserSummary> userSearchPrevious()
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public IdPage<IdUserSummary> userSearchByEmailBegin(
    final IdUserSearchByEmailParameters parameters)
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public IdPage<IdUserSummary> userSearchByEmailNext()
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public IdPage<IdUserSummary> userSearchByEmailPrevious()
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public Optional<IdUser> userGet(
    final UUID id)
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public Optional<IdUser> userGetByEmail(
    final IdEmail email)
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public IdUser userUpdate(
    final UUID user,
    final Optional<IdName> idName,
    final Optional<IdRealName> realName,
    final Optional<IdPassword> password)
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public IdUser userEmailAdd(
    final UUID id,
    final IdEmail email)
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public IdUser userEmailRemove(
    final UUID id,
    final IdEmail email)
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public IdUser userCreate(
    final Optional<UUID> id,
    final IdName idName,
    final IdRealName realName,
    final IdEmail email,
    final IdPassword password)
    throws IdAClientException, InterruptedException
  {
    throw this.notLoggedIn();
  }

  @Override
  public void userDelete(final UUID id)
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public void userBanCreate(final IdBan ban)
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public Optional<IdBan> userBanGet(final UUID id)
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public void userBanDelete(final IdBan ban)
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public List<IdLogin> userLoginHistory(
    final UUID id)
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public IdPage<IdAuditEvent> auditSearchBegin(
    final IdTimeRange timeRange,
    final Optional<String> owner,
    final Optional<String> type,
    final Optional<String> message,
    final int pageSize)
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public IdPage<IdAuditEvent> auditSearchNext()
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public IdPage<IdAuditEvent> auditSearchPrevious()
    throws IdAClientException
  {
    throw this.notLoggedIn();
  }
}

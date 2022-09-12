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
import com.io7m.idstore.admin_client.api.IdAClientType;
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdAdminPermission;
import com.io7m.idstore.model.IdAdminSearchByEmailParameters;
import com.io7m.idstore.model.IdAdminSearchParameters;
import com.io7m.idstore.model.IdAdminSummary;
import com.io7m.idstore.model.IdAuditEvent;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPage;
import com.io7m.idstore.model.IdPassword;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdTimeRange;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.model.IdUserSearchByEmailParameters;
import com.io7m.idstore.model.IdUserSearchParameters;
import com.io7m.idstore.model.IdUserSummary;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The default client implementation.
 */

public final class IdAClient implements IdAClientType
{
  private final IdAStrings strings;
  private final HttpClient httpClient;
  private final Locale locale;
  private volatile IdAClientProtocolHandlerType handler;

  /**
   * The default client implementation.
   *
   * @param inLocale     The locale
   * @param inStrings    The string resources
   * @param inHttpClient The HTTP client
   * @param inHandler    The versioned handler
   */

  public IdAClient(
    final Locale inLocale,
    final IdAStrings inStrings,
    final HttpClient inHttpClient,
    final IdAClientProtocolHandlerType inHandler)
  {
    this.locale =
      Objects.requireNonNull(inLocale, "locale");
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.httpClient =
      Objects.requireNonNull(inHttpClient, "httpClient");
    this.handler =
      Objects.requireNonNull(inHandler, "handler");
  }

  @Override
  public IdPage<IdAdminSummary> adminSearchBegin(
    final IdAdminSearchParameters parameters)
    throws IdAClientException, InterruptedException
  {
    return this.handler.adminSearchBegin(parameters);
  }

  @Override
  public IdPage<IdAdminSummary> adminSearchNext()
    throws IdAClientException, InterruptedException
  {
    return this.handler.adminSearchNext();
  }

  @Override
  public IdPage<IdAdminSummary> adminSearchPrevious()
    throws IdAClientException, InterruptedException
  {
    return this.handler.adminSearchPrevious();
  }

  @Override
  public IdPage<IdAdminSummary> adminSearchByEmailBegin(
    final IdAdminSearchByEmailParameters parameters)
    throws IdAClientException, InterruptedException
  {
    return this.handler.adminSearchByEmailBegin(parameters);
  }

  @Override
  public IdPage<IdAdminSummary> adminSearchByEmailNext()
    throws IdAClientException, InterruptedException
  {
    return this.handler.adminSearchByEmailNext();
  }

  @Override
  public IdPage<IdAdminSummary> adminSearchByEmailPrevious()
    throws IdAClientException, InterruptedException
  {
    return this.handler.adminSearchByEmailPrevious();
  }

  @Override
  public Optional<IdAdmin> adminGet(final UUID id)
    throws IdAClientException, InterruptedException
  {
    return this.handler.adminGet(id);
  }

  @Override
  public Optional<IdAdmin> adminGetByEmail(
    final IdEmail email)
    throws IdAClientException, InterruptedException
  {
    return this.handler.adminGetByEmail(email);
  }

  @Override
  public IdAdmin adminUpdate(
    final IdAdmin admin)
    throws IdAClientException, InterruptedException
  {
    return this.handler.adminUpdate(admin);
  }

  @Override
  public IdAdmin adminCreate(
    final Optional<UUID> id,
    final IdName idName,
    final IdRealName realName,
    final IdEmail email,
    final IdPassword password,
    final Set<IdAdminPermission> permissions)
    throws IdAClientException, InterruptedException
  {
    return this.handler.adminCreate(
      id,
      idName,
      realName,
      email,
      password,
      permissions);
  }

  @Override
  public void close()
    throws IOException
  {

  }

  @Override
  public void login(
    final String admin,
    final String password,
    final URI base)
    throws IdAClientException, InterruptedException
  {
    final var newHandler =
      IdAProtocolNegotiation.negotiateProtocolHandler(
        this.locale,
        this.httpClient,
        this.strings,
        admin,
        password,
        base
      );

    this.handler = newHandler.login(admin, password, base);
  }

  @Override
  public IdAdmin adminSelf()
    throws IdAClientException, InterruptedException
  {
    return this.handler.adminSelf();
  }

  @Override
  public String toString()
  {
    return String.format(
      "[IdAClient 0x%s",
      Integer.toUnsignedString(this.hashCode())
    );
  }

  @Override
  public IdPage<IdUserSummary> userSearchBegin(
    final IdUserSearchParameters parameters)
    throws IdAClientException, InterruptedException
  {
    return this.handler.userSearchBegin(parameters);
  }

  @Override
  public IdPage<IdUserSummary> userSearchNext()
    throws IdAClientException, InterruptedException
  {
    return this.handler.userSearchNext();
  }

  @Override
  public IdPage<IdUserSummary> userSearchPrevious()
    throws IdAClientException, InterruptedException
  {
    return this.handler.userSearchPrevious();
  }

  @Override
  public IdPage<IdUserSummary> userSearchByEmailBegin(
    final IdUserSearchByEmailParameters parameters)
    throws IdAClientException, InterruptedException
  {
    return this.handler.userSearchByEmailBegin(parameters);
  }

  @Override
  public IdPage<IdUserSummary> userSearchByEmailNext()
    throws IdAClientException, InterruptedException
  {
    return this.handler.userSearchByEmailNext();
  }

  @Override
  public IdPage<IdUserSummary> userSearchByEmailPrevious()
    throws IdAClientException, InterruptedException
  {
    return this.handler.userSearchByEmailPrevious();
  }

  @Override
  public Optional<IdUser> userGet(
    final UUID id)
    throws IdAClientException, InterruptedException
  {
    return this.handler.userGet(id);
  }

  @Override
  public Optional<IdUser> userGetByEmail(
    final IdEmail email)
    throws IdAClientException, InterruptedException
  {
    return this.handler.userGetByEmail(email);
  }

  @Override
  public IdUser userUpdate(final IdUser user)
    throws IdAClientException, InterruptedException
  {
    return this.handler.userUpdate(user);
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
    return this.handler.userCreate(id, idName, realName, email, password);
  }

  @Override
  public IdPage<IdAuditEvent> auditSearchBegin(
    final IdTimeRange timeRange,
    final Optional<String> owner,
    final Optional<String> type,
    final Optional<String> message,
    final int pageSize)
    throws IdAClientException, InterruptedException
  {
    return this.handler.auditSearchBegin(
      timeRange,
      owner,
      type,
      message,
      pageSize
    );
  }

  @Override
  public IdPage<IdAuditEvent> auditSearchNext()
    throws IdAClientException, InterruptedException
  {
    return this.handler.auditSearchNext();
  }

  @Override
  public IdPage<IdAuditEvent> auditSearchPrevious()
    throws IdAClientException, InterruptedException
  {
    return this.handler.auditSearchPrevious();
  }

  @Override
  public void adminDelete(
    final UUID id)
    throws IdAClientException, InterruptedException
  {
    this.handler.adminDelete(id);
  }

  @Override
  public void userDelete(
    final UUID id)
    throws IdAClientException, InterruptedException
  {
    this.handler.userDelete(id);
  }
}

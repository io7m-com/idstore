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

package com.io7m.idstore.user_client.internal;

import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdToken;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.user_client.api.IdUClientException;
import com.io7m.idstore.user_client.api.IdUClientType;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.Objects;

/**
 * The default client implementation.
 */

public final class IdUClient implements IdUClientType
{
  private final IdUStrings strings;
  private final HttpClient httpClient;
  private volatile IdUClientProtocolHandlerType handler;

  /**
   * The default client implementation.
   *
   * @param inStrings    The string resources
   * @param inHttpClient The HTTP client
   * @param inHandler    The versioned handler
   */

  public IdUClient(
    final IdUStrings inStrings,
    final HttpClient inHttpClient,
    final IdUClientProtocolHandlerType inHandler)
  {
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.httpClient =
      Objects.requireNonNull(inHttpClient, "httpClient");
    this.handler =
      Objects.requireNonNull(inHandler, "handler");
  }

  @Override
  public void close()
    throws IOException
  {

  }

  @Override
  public void login(
    final String user,
    final String password,
    final URI base)
    throws IdUClientException, InterruptedException
  {
    final var newHandler =
      IdUProtocolNegotiation.negotiateProtocolHandler(
        this.httpClient,
        this.strings,
        user,
        password,
        base
      );

    this.handler = newHandler.login(user, password, base);
  }

  @Override
  public IdUser userSelf()
    throws IdUClientException, InterruptedException
  {
    return this.handler.userSelf();
  }

  @Override
  public void userEmailAddBegin(
    final IdEmail email)
    throws IdUClientException, InterruptedException
  {
    this.handler.userEmailAddBegin(email);
  }

  @Override
  public void userEmailAddPermit(
    final IdToken token)
    throws IdUClientException, InterruptedException
  {
    this.handler.userEmailAddPermit(token);
  }

  @Override
  public void userEmailAddDeny(
    final IdToken token)
    throws IdUClientException, InterruptedException
  {
    this.handler.userEmailAddDeny(token);
  }

  @Override
  public void userEmailRemoveBegin(
    final IdEmail email)
    throws IdUClientException, InterruptedException
  {
    this.handler.userEmailRemoveBegin(email);
  }

  @Override
  public void userEmailRemovePermit(
    final IdToken token)
    throws IdUClientException, InterruptedException
  {
    this.handler.userEmailRemovePermit(token);
  }

  @Override
  public void userEmailRemoveDeny(
    final IdToken token)
    throws IdUClientException, InterruptedException
  {
    this.handler.userEmailRemoveDeny(token);
  }

  @Override
  public void userRealNameUpdate(
    final IdRealName realName)
    throws IdUClientException, InterruptedException
  {
    this.handler.userRealNameUpdate(realName);
  }
}

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

import java.net.URI;
import java.net.http.HttpClient;
import java.util.Objects;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.NOT_LOGGED_IN;

/**
 * The "disconnected" protocol handler.
 */

public final class IdUClientProtocolHandlerDisconnected
  implements IdUClientProtocolHandlerType
{
  private final HttpClient httpClient;
  private final IdUStrings strings;

  /**
   * The "disconnected" protocol handler.
   *
   * @param inStrings    The string resources
   * @param inHttpClient The HTTP client
   */

  public IdUClientProtocolHandlerDisconnected(
    final IdUStrings inStrings,
    final HttpClient inHttpClient)
  {
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.httpClient =
      Objects.requireNonNull(inHttpClient, "httpClient");
  }

  @Override
  public IdUNewHandler login(
    final String user,
    final String password,
    final URI base)
    throws IdUClientException, InterruptedException
  {
    final var handler =
      IdUProtocolNegotiation.negotiateProtocolHandler(
        this.httpClient,
        this.strings,
        user,
        password,
        base
      );

    return handler.login(user, password, base);
  }

  private IdUClientException notLoggedIn()
  {
    return new IdUClientException(
      NOT_LOGGED_IN,
      this.strings.format("notLoggedIn")
    );
  }

  @Override
  public IdUser userSelf()
    throws IdUClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public void userEmailAddBegin(
    final IdEmail email)
    throws IdUClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public void userEmailAddPermit(
    final IdToken token)
    throws IdUClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public void userEmailAddDeny(final IdToken token)
    throws IdUClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public void userEmailRemoveBegin(
    final IdEmail email)
    throws IdUClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public void userEmailRemovePermit(
    final IdToken token)
    throws IdUClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public void userEmailRemoveDeny(
    final IdToken token)
    throws IdUClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public void userRealNameUpdate(
    final IdRealName realName)
    throws IdUClientException
  {
    throw this.notLoggedIn();
  }
}

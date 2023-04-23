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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.NOT_LOGGED_IN;

/**
 * The "disconnected" protocol handler.
 */

public final class IdUClientProtocolHandlerDisconnected
  implements IdUClientProtocolHandlerType
{
  private final Locale locale;
  private final HttpClient httpClient;
  private final IdUStrings strings;

  /**
   * The "disconnected" protocol handler.
   *
   * @param inLocale     The locale
   * @param inStrings    The string resources
   * @param inHttpClient The HTTP client
   */

  public IdUClientProtocolHandlerDisconnected(
    final Locale inLocale,
    final IdUStrings inStrings,
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
  public IdUNewHandler login(
    final String user,
    final String password,
    final URI base,
    final Map<String, String> metadata)
    throws IdUClientException, InterruptedException
  {
    final var handler =
      IdUProtocolNegotiation.negotiateProtocolHandler(
        this.locale,
        this.httpClient,
        this.strings,
        base
      );

    return handler.login(user, password, base, metadata);
  }

  private IdUClientException notLoggedIn()
  {
    final var msg = this.strings.format("notLoggedIn");
    return new IdUClientException(
      msg,
      NOT_LOGGED_IN,
      Map.of(),
      Optional.empty(),
      Optional.empty()
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

  @Override
  public void userPasswordUpdate(
    final String password,
    final String passwordConfirm)
    throws IdUClientException
  {
    throw this.notLoggedIn();
  }
}

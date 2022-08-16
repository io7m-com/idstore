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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.Locale;
import java.util.Objects;

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
   * @param inLocale       The locale
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
}

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

package com.io7m.idstore.admin_client;

import com.io7m.idstore.admin_client.api.IdAClientFactoryType;
import com.io7m.idstore.admin_client.api.IdAClientType;
import com.io7m.idstore.admin_client.internal.IdAClient;
import com.io7m.idstore.admin_client.internal.IdAClientProtocolHandlerDisconnected;
import com.io7m.idstore.admin_client.internal.IdAStrings;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.CookieManager;
import java.net.http.HttpClient;
import java.util.Locale;

/**
 * The default client factory.
 */

public final class IdAClients implements IdAClientFactoryType
{
  /**
   * The default client factory.
   */

  public IdAClients()
  {

  }

  @Override
  public IdAClientType create(final Locale locale)
  {
    final var cookieJar =
      new CookieManager();

    final IdAStrings strings;
    try {
      strings = new IdAStrings(locale);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }

    final var httpClient =
      HttpClient.newBuilder()
        .cookieHandler(cookieJar)
        .build();

    return new IdAClient(
      locale,
      strings,
      httpClient,
      new IdAClientProtocolHandlerDisconnected(locale, strings, httpClient)
    );
  }
}

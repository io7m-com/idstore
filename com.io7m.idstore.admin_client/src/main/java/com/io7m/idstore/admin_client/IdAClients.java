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

package com.io7m.idstore.admin_client;

import com.io7m.idstore.admin_client.api.IdAClientAsynchronousType;
import com.io7m.idstore.admin_client.api.IdAClientConfiguration;
import com.io7m.idstore.admin_client.api.IdAClientFactoryType;
import com.io7m.idstore.admin_client.api.IdAClientSynchronousType;
import com.io7m.idstore.admin_client.internal.IdAClientAsynchronous;
import com.io7m.idstore.admin_client.internal.IdAClientSynchronous;
import com.io7m.idstore.strings.IdStrings;

import java.net.CookieManager;
import java.net.http.HttpClient;

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
  public IdAClientAsynchronousType openAsynchronousClient(
    final IdAClientConfiguration configuration)
  {
    final var cookieJar =
      new CookieManager();
    final var locale =
      configuration.locale();
    final var strings =
      IdStrings.create(locale);

    final var httpClient =
      HttpClient.newBuilder()
        .cookieHandler(cookieJar)
        .build();

    return new IdAClientAsynchronous(configuration, strings, httpClient);
  }

  @Override
  public IdAClientSynchronousType openSynchronousClient(
    final IdAClientConfiguration configuration)
  {
    final var cookieJar =
      new CookieManager();
    final var locale =
      configuration.locale();
    final var strings =
      IdStrings.create(locale);

    final var httpClient =
      HttpClient.newBuilder()
        .cookieHandler(cookieJar)
        .build();

    return new IdAClientSynchronous(configuration, strings, httpClient);
  }
}

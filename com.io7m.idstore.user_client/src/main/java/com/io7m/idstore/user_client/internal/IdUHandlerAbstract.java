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

package com.io7m.idstore.user_client.internal;

import com.io7m.idstore.user_client.api.IdUClientConfiguration;
import com.io7m.idstore.user_client.api.IdUClientException;
import com.io7m.idstore.strings.IdStrings;

import java.net.http.HttpClient;
import java.util.Objects;
import java.util.function.Function;

/**
 * The abstract base class for handlers.
 */

abstract class IdUHandlerAbstract
  implements IdUHandlerType
{
  private final IdStrings strings;
  private final HttpClient httpClient;
  private final IdUClientConfiguration configuration;

  protected IdUHandlerAbstract(
    final IdUClientConfiguration inConfiguration,
    final IdStrings inStrings,
    final HttpClient inHttpClient)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.httpClient =
      Objects.requireNonNull(inHttpClient, "httpClient");
  }

  @Override
  public final Function<Throwable, IdUClientException> exceptionTransformer()
  {
    return IdUClientException::ofException;
  }

  protected final IdStrings strings()
  {
    return this.strings;
  }

  protected final HttpClient httpClient()
  {
    return this.httpClient;
  }

  protected final IdUClientConfiguration configuration()
  {
    return this.configuration;
  }
}

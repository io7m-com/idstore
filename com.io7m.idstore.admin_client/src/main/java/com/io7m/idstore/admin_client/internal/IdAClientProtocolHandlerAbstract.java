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

import java.net.URI;
import java.net.http.HttpClient;
import java.util.Objects;

/**
 * Abstract base class for versioned handlers.
 */

public abstract class IdAClientProtocolHandlerAbstract
  implements IdAClientProtocolHandlerType
{
  private final HttpClient httpClient;
  private final IdAStrings strings;
  private final URI base;

  /**
   * Abstract base class for versioned handlers.
   *
   * @param inHttpClient The HTTP client
   * @param inBase       The base URI
   * @param inStrings    The string resources
   */

  public IdAClientProtocolHandlerAbstract(
    final HttpClient inHttpClient,
    final IdAStrings inStrings,
    final URI inBase)
  {
    this.httpClient =
      Objects.requireNonNull(inHttpClient, "inHttpClient");
    this.strings =
      Objects.requireNonNull(inStrings, "inStrings");
    this.base =
      Objects.requireNonNull(inBase, "inBase");
  }

  protected final HttpClient httpClient()
  {
    return this.httpClient;
  }

  protected final IdAStrings strings()
  {
    return this.strings;
  }

  protected final URI base()
  {
    return this.base;
  }
}

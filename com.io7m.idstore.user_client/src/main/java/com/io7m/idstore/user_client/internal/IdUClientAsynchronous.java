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

import com.io7m.hibiscus.basic.HBClientAsynchronousAbstract;
import com.io7m.idstore.protocol.user.IdUCommandType;
import com.io7m.idstore.protocol.user.IdUResponseError;
import com.io7m.idstore.protocol.user.IdUResponseType;
import com.io7m.idstore.strings.IdStrings;
import com.io7m.idstore.user_client.api.IdUClientAsynchronousType;
import com.io7m.idstore.user_client.api.IdUClientConfiguration;
import com.io7m.idstore.user_client.api.IdUClientCredentials;
import com.io7m.idstore.user_client.api.IdUClientEventType;
import com.io7m.idstore.user_client.api.IdUClientException;

import java.net.http.HttpClient;

/**
 * The asynchronous client.
 */

public final class IdUClientAsynchronous
  extends HBClientAsynchronousAbstract<
  IdUClientException,
  IdUCommandType<?>,
  IdUResponseType,
  IdUResponseType,
  IdUResponseError,
  IdUClientEventType,
  IdUClientCredentials>
  implements IdUClientAsynchronousType
{
  /**
   * The asynchronous client.
   *
   * @param inConfiguration The configuration
   * @param inHttpClient    The HTTP client
   * @param inStrings       The string resources
   */

  public IdUClientAsynchronous(
    final IdUClientConfiguration inConfiguration,
    final IdStrings inStrings,
    final HttpClient inHttpClient)
  {
    super(
      new IdUClientSynchronous(inConfiguration, inStrings, inHttpClient),
      "com.io7m.idstore.admin_client"
    );
  }
}

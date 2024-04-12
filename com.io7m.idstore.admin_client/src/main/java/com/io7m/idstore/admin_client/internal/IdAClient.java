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


package com.io7m.idstore.admin_client.internal;

import com.io7m.hibiscus.api.HBClientAbstract;
import com.io7m.idstore.admin_client.api.IdAClientConfiguration;
import com.io7m.idstore.admin_client.api.IdAClientConnectionParameters;
import com.io7m.idstore.admin_client.api.IdAClientException;
import com.io7m.idstore.admin_client.api.IdAClientType;
import com.io7m.idstore.protocol.admin.IdAMessageType;
import com.io7m.idstore.strings.IdStrings;

import java.net.http.HttpClient;
import java.util.function.Supplier;

/**
 * The client.
 */

public final class IdAClient
  extends HBClientAbstract<
  IdAMessageType,
  IdAClientConnectionParameters,
  IdAClientException>
  implements IdAClientType
{
  /**
   * The client.
   *
   * @param inConfiguration The configuration
   * @param inHttpClients   The HTTP clients
   * @param inStrings       The string resources
   */

  public IdAClient(
    final IdAClientConfiguration inConfiguration,
    final IdStrings inStrings,
    final Supplier<HttpClient> inHttpClients)
  {
    super(
      new IdAHandlerDisconnected(inConfiguration, inStrings, inHttpClients)
    );
  }

  @Override
  public String toString()
  {
    return "[%s 0x%s]".formatted(
      this.getClass().getSimpleName(),
      Integer.toUnsignedString(this.hashCode(), 16)
    );
  }
}

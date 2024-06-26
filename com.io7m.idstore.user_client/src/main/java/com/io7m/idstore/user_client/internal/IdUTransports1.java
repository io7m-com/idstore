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

import com.io7m.genevan.core.GenProtocolIdentifier;
import com.io7m.genevan.core.GenProtocolVersion;
import com.io7m.idstore.protocol.user.cb.IdUCB1Messages;
import com.io7m.idstore.strings.IdStrings;
import com.io7m.idstore.user_client.api.IdUClientConfiguration;

import java.net.URI;
import java.net.http.HttpClient;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

/**
 * The factory of version 1 protocol transports.
 */

public final class IdUTransports1
  implements IdUTransportFactoryType
{
  /**
   * The factory of version 1 protocol transports.
   */

  public IdUTransports1()
  {

  }

  @Override
  public GenProtocolIdentifier supported()
  {
    return new GenProtocolIdentifier(
      IdUCB1Messages.protocolId().toString(),
      new GenProtocolVersion(ONE, ZERO)
    );
  }

  @Override
  public IdUTransportType createTransport(
    final IdUClientConfiguration configuration,
    final HttpClient inHttpClient,
    final IdStrings inStrings,
    final URI inBaseURI)
  {
    return new IdUTransport1(
      inStrings,
      inHttpClient,
      inBaseURI
    );
  }
}

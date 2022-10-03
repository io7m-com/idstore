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


package com.io7m.idstore.server.internal;

import com.io7m.idstore.services.api.IdServiceType;
import com.io7m.verdant.core.VProtocolException;
import com.io7m.verdant.core.VProtocols;
import com.io7m.verdant.core.cb.VProtocolMessages;

import java.net.URI;

/**
 * Service wrapper for Verdant messages.
 */

public final class IdVerdantMessages implements IdServiceType
{
  private final VProtocolMessages messages;

  /**
   * Service wrapper for Verdant messages.
   */

  public IdVerdantMessages()
  {
    this.messages = VProtocolMessages.create();
  }

  /**
   * @return The content type used for versioning
   */

  public static String contentType()
  {
    return "application/verdant+cedarbridge";
  }

  /**
   * Serialize the given protocols.
   *
   * @param protocols The protocols
   * @param version   The container version
   *
   * @return The serialized bytes
   *
   * @throws VProtocolException On errors
   */


  public byte[] serialize(
    final VProtocols protocols,
    final int version)
    throws VProtocolException
  {
    return this.messages.serialize(protocols, version);
  }

  /**
   * Parse versions from the given source.
   *
   * @param source The source
   * @param data   The read bytes
   *
   * @return A set of parsed versions
   *
   * @throws VProtocolException If the input cannot be parsed
   */

  public VProtocols parse(
    final URI source,
    final byte[] data)
    throws VProtocolException
  {
    return this.messages.parse(source, data);
  }

  @Override
  public String description()
  {
    return "Verdant message service.";
  }
}

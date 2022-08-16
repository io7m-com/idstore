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

package com.io7m.idstore.server.internal.admin_v1;

import com.io7m.idstore.protocol.admin_v1.IdA1Messages;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.versions.IdVMessages;
import com.io7m.idstore.protocol.versions.IdVProtocolSupported;
import com.io7m.idstore.protocol.versions.IdVProtocols;
import com.io7m.idstore.services.api.IdServiceDirectoryType;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * A versioning servlet.
 */

public final class IdA1Versions extends HttpServlet
{
  private static final IdVProtocols PROTOCOLS =
    createProtocols();

  private final IdVMessages messages;

  /**
   * A versioning servlet.
   *
   * @param inServices The service directory
   */

  public IdA1Versions(
    final IdServiceDirectoryType inServices)
  {
    this.messages =
      inServices.requireService(IdVMessages.class);
  }

  private static IdVProtocols createProtocols()
  {
    final var supported = new ArrayList<IdVProtocolSupported>();
    supported.add(
      new IdVProtocolSupported(
        IdA1Messages.schemaId(),
        BigInteger.ONE,
        BigInteger.ZERO,
        "/admin/1/0/"
      )
    );
    return new IdVProtocols(List.copyOf(supported));
  }

  @Override
  protected void service(
    final HttpServletRequest request,
    final HttpServletResponse response)
    throws IOException
  {
    try {
      response.setContentType(IdVMessages.contentType());
      response.setStatus(200);

      final var data = this.messages.serialize(PROTOCOLS);
      response.setContentLength(data.length + 2);

      try (var output = response.getOutputStream()) {
        output.write(data);
        output.write('\r');
        output.write('\n');
      }
    } catch (final IdProtocolException e) {
      throw new IOException(e);
    }
  }
}

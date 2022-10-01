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

import com.io7m.idstore.protocol.admin.cb.IdACB1Messages;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.versions.IdVProtocolSupported;
import com.io7m.idstore.protocol.versions.IdVProtocolsSupported;
import com.io7m.idstore.protocol.versions.cb.IdVCB1Messages;
import com.io7m.idstore.server.internal.common.IdCommonInstrumentedServlet;
import com.io7m.idstore.services.api.IdServiceDirectoryType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A versioning servlet.
 */

public final class IdA1Versions extends IdCommonInstrumentedServlet
{
  private static final IdVProtocolsSupported PROTOCOLS =
    createProtocols();

  private final IdVCB1Messages messages;

  /**
   * A versioning servlet.
   *
   * @param inServices The service directory
   */

  public IdA1Versions(
    final IdServiceDirectoryType inServices)
  {
    super(Objects.requireNonNull(inServices, "services"));

    this.messages =
      inServices.requireService(IdVCB1Messages.class);
  }

  private static IdVProtocolsSupported createProtocols()
  {
    final var supported = new ArrayList<IdVProtocolSupported>();
    supported.add(
      new IdVProtocolSupported(
        IdACB1Messages.protocolId(),
        BigInteger.ONE,
        BigInteger.ZERO,
        "/admin/1/0/"
      )
    );
    return new IdVProtocolsSupported(List.copyOf(supported));
  }

  @Override
  protected void service(
    final HttpServletRequest request,
    final HttpServletResponse response)
    throws IOException
  {
    try {
      response.setContentType(IdVCB1Messages.contentType());
      response.setStatus(200);

      final var data = this.messages.serialize(PROTOCOLS);
      response.setContentLength(data.length);

      try (var output = response.getOutputStream()) {
        output.write(data);
      }
    } catch (final IdProtocolException e) {
      throw new IOException(e);
    }
  }
}

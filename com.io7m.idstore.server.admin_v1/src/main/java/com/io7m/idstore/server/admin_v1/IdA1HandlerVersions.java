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


package com.io7m.idstore.server.admin_v1;

import com.io7m.idstore.model.IdUserDomain;
import com.io7m.idstore.protocol.admin.cb.IdACB1Messages;
import com.io7m.idstore.server.http.IdHTTPHandlerFunctional;
import com.io7m.idstore.server.http.IdHTTPHandlerFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPResponseType;
import com.io7m.idstore.server.service.verdant.IdVerdantMessages;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import com.io7m.verdant.core.VProtocolException;
import com.io7m.verdant.core.VProtocolSupported;
import com.io7m.verdant.core.VProtocols;

import java.util.List;
import java.util.Set;

import static com.io7m.idstore.server.http.IdHTTPHandlerCoreInstrumented.withInstrumentation;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The v1 version servlet.
 */

public final class IdA1HandlerVersions
  extends IdHTTPHandlerFunctional
{
  private static final VProtocols PROTOCOLS =
    createProtocols();

  /**
   * The v1 version servlet.
   *
   * @param services The services
   */

  public IdA1HandlerVersions(
    final RPServiceDirectoryType services)
  {
    super(createCore(services));
  }

  private static IdHTTPHandlerFunctionalCoreType createCore(
    final RPServiceDirectoryType services)
  {
    final var messages =
      services.requireService(IdVerdantMessages.class);

    return (request, information) -> {
      return withInstrumentation(
        services,
        IdUserDomain.ADMIN,
        (req0, info0) -> {
          return execute(messages);
        }
      ).execute(request, information);
    };
  }

  private static IdHTTPResponseType execute(
    final IdVerdantMessages messages)
  {
    try {
      return new IdHTTPResponseFixedSize(
        200,
        Set.of(),
        IdVerdantMessages.contentType(),
        messages.serialize(PROTOCOLS, 1)
      );
    } catch (final VProtocolException e) {
      return new IdHTTPResponseFixedSize(
        500,
        Set.of(),
        "text/plain",
        e.getMessage().getBytes(UTF_8)
      );
    }
  }

  private static VProtocols createProtocols()
  {
    return new VProtocols(List.of(
      new VProtocolSupported(
        IdACB1Messages.protocolId(),
        1L,
        0L,
        "/admin/1/0/"
      )
    ));
  }
}

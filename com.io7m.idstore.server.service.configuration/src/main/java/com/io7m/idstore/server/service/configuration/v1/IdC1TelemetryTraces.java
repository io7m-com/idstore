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


package com.io7m.idstore.server.service.configuration.v1;

import com.io7m.blackthorne.core.BTElementHandlerType;
import com.io7m.blackthorne.core.BTElementParsingContextType;
import com.io7m.idstore.server.api.IdServerOpenTelemetryConfiguration.IdOTLPProtocol;
import com.io7m.idstore.server.api.IdServerOpenTelemetryConfiguration.IdTraces;
import org.xml.sax.Attributes;

import java.net.URI;

final class IdC1TelemetryTraces
  implements BTElementHandlerType<Object, IdTraces>
{
  private IdTraces result;

  IdC1TelemetryTraces(
    final BTElementParsingContextType context)
  {

  }

  @Override
  public void onElementStart(
    final BTElementParsingContextType context,
    final Attributes attributes)
  {
    this.result = new IdTraces(
      URI.create(attributes.getValue("Endpoint")),
      IdOTLPProtocol.valueOf(attributes.getValue("Protocol"))
    );
  }

  @Override
  public IdTraces onElementFinished(
    final BTElementParsingContextType context)
  {
    return this.result;
  }
}

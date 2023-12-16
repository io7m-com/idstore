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

import com.io7m.blackthorne.core.BTElementHandlerConstructorType;
import com.io7m.blackthorne.core.BTElementHandlerType;
import com.io7m.blackthorne.core.BTElementParsingContextType;
import com.io7m.blackthorne.core.BTQualifiedName;
import com.io7m.idstore.server.api.IdServerHTTPServiceConfiguration;
import com.io7m.idstore.tls.IdTLSConfigurationType;
import com.io7m.idstore.tls.IdTLSDisabled;
import com.io7m.idstore.tls.IdTLSEnabled;
import org.xml.sax.Attributes;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

import static com.io7m.idstore.server.service.configuration.v1.IdC1Names.tlsQName;
import static java.util.Map.entry;

abstract class IdC1AbstractHTTPService
  implements BTElementHandlerType<Object, IdC1HTTPServiceConfiguration>
{
  private final String semantic;
  private String listenAddress;
  private int listenPort;
  private URI externalAddress;
  private IdTLSConfigurationType tls;

  IdC1AbstractHTTPService(
    final String inSemantic,
    final BTElementParsingContextType context)
  {
    this.semantic =
      Objects.requireNonNull(inSemantic, "semantic");
  }

  @Override
  public final Map<BTQualifiedName, BTElementHandlerConstructorType<?, ?>>
  onChildHandlersRequested(
    final BTElementParsingContextType context)
  {
    return Map.ofEntries(
      entry(tlsQName("TLSEnabled"), IdC1TLSEnabled::new),
      entry(tlsQName("TLSDisabled"), IdC1TLSDisabled::new)
    );
  }

  @Override
  public final void onChildValueProduced(
    final BTElementParsingContextType context,
    final Object result)
    throws Exception
  {
    switch (result) {
      case final IdTLSEnabled s -> {
        this.tls = s;
      }
      case final IdTLSDisabled s -> {
        this.tls = s;
      }
      default -> {
        throw new IllegalArgumentException(
          "Unrecognized element: %s".formatted(result)
        );
      }
    }
  }

  @Override
  public final void onElementStart(
    final BTElementParsingContextType context,
    final Attributes attributes)
    throws Exception
  {
    this.listenAddress =
      attributes.getValue("ListenAddress");
    this.listenPort =
      Integer.parseUnsignedInt(attributes.getValue("ListenPort"));
    this.externalAddress =
      URI.create(attributes.getValue("ExternalURI"));
  }

  @Override
  public final IdC1HTTPServiceConfiguration onElementFinished(
    final BTElementParsingContextType context)
    throws Exception
  {
    return new IdC1HTTPServiceConfiguration(
      this.semantic,
      new IdServerHTTPServiceConfiguration(
        this.listenAddress,
        this.listenPort,
        this.externalAddress,
        this.tls
      )
    );
  }
}

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
import com.io7m.idstore.server.api.IdServerMailAuthenticationConfiguration;
import com.io7m.idstore.server.api.IdServerMailConfiguration;
import com.io7m.idstore.server.api.IdServerMailTransportConfigurationType;
import org.xml.sax.Attributes;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static com.io7m.idstore.server.service.configuration.v1.IdC1Names.qName;

final class IdC1Mail
  implements BTElementHandlerType<Object, IdServerMailConfiguration>
{
  private IdServerMailTransportConfigurationType transport;
  private Optional<IdServerMailAuthenticationConfiguration> authentication;
  private String senderAddress;
  private Duration verificationExpiration;

  IdC1Mail(
    final BTElementParsingContextType context)
  {
    this.authentication = Optional.empty();
  }

  @Override
  public Map<BTQualifiedName, BTElementHandlerConstructorType<?, ?>>
  onChildHandlersRequested(
    final BTElementParsingContextType context)
  {
    return Map.ofEntries(
      Map.entry(qName("SMTP"), IdC1SMTP::new),
      Map.entry(qName("SMTPTLS"), IdC1SMTPTLS::new),
      Map.entry(qName("SMTPS"), IdC1SMTPS::new),
      Map.entry(qName("MailAuthentication"), IdC1MailAuthentication::new)
    );
  }

  @Override
  public void onElementStart(
    final BTElementParsingContextType context,
    final Attributes attributes)
    throws Exception
  {
    this.senderAddress =
      attributes.getValue("SenderAddress");
    this.verificationExpiration =
      IdC1Durations.parse(attributes.getValue("VerificationExpiration"));
  }

  @Override
  public void onChildValueProduced(
    final BTElementParsingContextType context,
    final Object result)
  {
    switch (result) {
      case final IdServerMailTransportConfigurationType c -> {
        this.transport = c;
      }
      case final IdServerMailAuthenticationConfiguration c -> {
        this.authentication = Optional.of(c);
      }
      default -> {
        throw new IllegalArgumentException(
          "Unrecognized element: %s".formatted(result)
        );
      }
    }
  }

  @Override
  public IdServerMailConfiguration onElementFinished(
    final BTElementParsingContextType context)
  {
    return new IdServerMailConfiguration(
      this.transport,
      this.authentication,
      this.senderAddress,
      this.verificationExpiration
    );
  }
}

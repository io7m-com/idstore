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

import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.server.api.IdServerMailConfiguration;
import com.io7m.idstore.server.api.IdServerMailTransportSMTP;
import com.io7m.idstore.server.api.IdServerMailTransportSMTPS;
import com.io7m.idstore.server.api.IdServerMailTransportSMTP_TLS;
import com.io7m.idstore.server.api.events.IdServerEventMailServiceFailure;
import com.io7m.idstore.services.api.IdServiceType;
import com.io7m.junreachable.UnreachableCodeException;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;
import org.simplejavamail.mailer.internal.MailerRegularBuilderImpl;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.simplejavamail.api.mailer.config.TransportStrategy.SMTP;
import static org.simplejavamail.api.mailer.config.TransportStrategy.SMTPS;
import static org.simplejavamail.api.mailer.config.TransportStrategy.SMTP_TLS;

/**
 * A mail service.
 */

public final class IdServerMailService implements IdServiceType
{
  private final Clock clock;
  private final IdServerMailConfiguration configuration;
  private final IdServerEventBusService events;
  private final IdServerTelemetryService telemetry;
  private final Mailer mailer;

  private IdServerMailService(
    final Clock inClock,
    final IdServerMailConfiguration inConfiguration,
    final IdServerEventBusService inEvents,
    final IdServerTelemetryService inTelemetry,
    final Mailer inMailer)
  {
    this.clock =
      Objects.requireNonNull(inClock, "clock");
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.events =
      Objects.requireNonNull(inEvents, "events");
    this.telemetry =
      Objects.requireNonNull(inTelemetry, "telemetry");
    this.mailer =
      Objects.requireNonNull(inMailer, "mailer");
  }

  /**
   * Create a new mail service.
   *
   * @param clock         The clock
   * @param telemetry     The telemetry service
   * @param events        The event bus service
   * @param configuration The mail configuration
   *
   * @return The service
   */

  public static IdServerMailService create(
    final Clock clock,
    final IdServerTelemetryService telemetry,
    final IdServerEventBusService events,
    final IdServerMailConfiguration configuration)
  {
    Objects.requireNonNull(clock, "clock");
    Objects.requireNonNull(telemetry, "telemetry");
    Objects.requireNonNull(events, "events");
    Objects.requireNonNull(configuration, "configuration");

    MailerRegularBuilderImpl mailerBuilder;

    final var authOpt =
      configuration.authenticationConfiguration();
    final var transport =
      configuration.transportConfiguration();

    if (authOpt.isPresent()) {
      mailerBuilder = MailerBuilder.withSMTPServer(
        transport.host(),
        Integer.valueOf(transport.port()),
        authOpt.get().userName(),
        authOpt.get().password()
      );
    } else {
      mailerBuilder = MailerBuilder.withSMTPServer(
        transport.host(),
        Integer.valueOf(transport.port())
      );
    }

    if (transport instanceof IdServerMailTransportSMTP) {
      mailerBuilder = mailerBuilder.withTransportStrategy(SMTP);
    } else if (transport instanceof IdServerMailTransportSMTPS) {
      mailerBuilder = mailerBuilder.withTransportStrategy(SMTPS);
    } else if (transport instanceof IdServerMailTransportSMTP_TLS) {
      mailerBuilder = mailerBuilder.withTransportStrategy(SMTP_TLS);
    } else {
      throw new UnreachableCodeException();
    }

    return new IdServerMailService(
      clock,
      configuration,
      events,
      telemetry,
      mailerBuilder.buildMailer()
    );
  }

  /**
   * Send a message to the given target address.
   *
   * @param requestId The request ID
   * @param to        The target address
   * @param subject   The message subject
   * @param text      The message text
   * @param headers   Extra message headers
   *
   * @return The send in progress
   */

  public CompletableFuture<Void> sendMail(
    final UUID requestId,
    final IdEmail to,
    final Map<String, String> headers,
    final String subject,
    final String text)
  {
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(to, "to");
    Objects.requireNonNull(subject, "subject");
    Objects.requireNonNull(headers, "headers");
    Objects.requireNonNull(text, "text");

    final var transport =
      this.configuration.transportConfiguration();

    final var span =
      this.telemetry.tracer()
        .spanBuilder("IdServerMailService.sendMail")
        .setAttribute("smtp.source_request", requestId.toString())
        .setAttribute("smtp.to", to.value())
        .setAttribute("smtp.subject", subject)
        .setAttribute("smtp.from", this.configuration.senderAddress())
        .setAttribute("smtp.host", transport.host())
        .setAttribute("smtp.port", transport.port())
        .startSpan();

    try {
      final var emailBuilder =
        EmailBuilder.startingBlank();

      for (final var entry : headers.entrySet()) {
        emailBuilder.withHeader(entry.getKey(), entry.getValue());
      }

      final var email =
        emailBuilder
          .to(to.value())
          .from(this.configuration.senderAddress())
          .withSubject(subject)
          .appendText(text)
          .buildEmail();

      return this.mailer.sendMail(email);
    } catch (final Exception e) {
      span.recordException(e);
      this.events.publish(
        new IdServerEventMailServiceFailure(
          OffsetDateTime.now(this.clock),
          requestId,
          e.getMessage()
        )
      );
      throw e;
    } finally {
      span.end();
    }
  }

  @Override
  public String description()
  {
    return "Mail service.";
  }

  @Override
  public String toString()
  {
    return "[IdServerMailService 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode()));
  }
}

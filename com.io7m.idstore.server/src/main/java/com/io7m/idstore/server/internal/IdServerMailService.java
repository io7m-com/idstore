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
import com.io7m.idstore.services.api.IdServiceType;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.time.Clock;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A mail service.
 */

public final class IdServerMailService implements IdServiceType, AutoCloseable
{
  private final Clock clock;
  private final IdServerMailConfiguration configuration;
  private final IdServerTelemetryService telemetry;
  private final Session session;
  private final ExecutorService executor;

  private IdServerMailService(
    final Clock inClock,
    final IdServerMailConfiguration inConfiguration,
    final IdServerTelemetryService inTelemetry,
    final Session inSession,
    final ExecutorService inExecutor)
  {
    this.clock =
      Objects.requireNonNull(inClock, "clock");
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.telemetry =
      Objects.requireNonNull(inTelemetry, "telemetry");
    this.session =
      Objects.requireNonNull(inSession, "session");
    this.executor =
      Objects.requireNonNull(inExecutor, "executor");
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

    final var executor =
      Executors.newFixedThreadPool(1, r -> {
        final var thread = new Thread(r);
        thread.setName(
          "com.io7m.idstore.server.internal.IdServerMailService[%d]"
            .formatted(thread.getId()));
        thread.setDaemon(true);
        return thread;
      });

    final var authOpt =
      configuration.authenticationConfiguration();
    final var transport =
      configuration.transportConfiguration();

    final var props = new Properties();
    if (transport instanceof IdServerMailTransportSMTP smtp) {
      props.put("mail.transport.protocol", "smtp");
      props.put("mail.smtp.host", transport.host());
      props.put("mail.smtp.port", Integer.toUnsignedString(transport.port()));
      props.put("mail.smtp.starttls.enable", "true");
      props.put("mail.smtp.starttls.required", "false");
      authOpt.ifPresent(auth -> {
        props.put("mail.smtp.username", auth.userName());
        props.put("mail.smtp.password", auth.password());
      });
    } else if (transport instanceof IdServerMailTransportSMTP_TLS smtpTls) {
      props.put("mail.transport.protocol", "smtp");
      props.put("mail.smtp.host", transport.host());
      props.put("mail.smtp.port", Integer.toUnsignedString(transport.port()));
      props.put("mail.smtp.starttls.enable", "true");
      props.put("mail.smtp.starttls.required", "true");
      authOpt.ifPresent(auth -> {
        props.put("mail.smtp.username", auth.userName());
        props.put("mail.smtp.password", auth.password());
      });
    } else if (transport instanceof IdServerMailTransportSMTPS smtps) {
      props.put("mail.transport.protocol", "smtps");
      props.put("mail.smtps.quitwait", "false");
      props.put("mail.smtps.host", transport.host());
      props.put("mail.smtps.port", Integer.toUnsignedString(transport.port()));
      authOpt.ifPresent(auth -> {
        props.put("mail.smtps.username", auth.userName());
        props.put("mail.smtps.password", auth.password());
      });
    }

    final var session = Session.getDefaultInstance(props, null);
    session.setDebug(false);

    return new IdServerMailService(
      clock,
      configuration,
      telemetry,
      session,
      executor
    );
  }

  /**
   * Send a message to the given target address.
   *
   * @param parentSpan The parent span for metrics
   * @param requestId  The request ID
   * @param to         The target address
   * @param subject    The message subject
   * @param text       The message text
   * @param headers    Extra message headers
   *
   * @return The send in progress
   */

  public CompletableFuture<Void> sendMail(
    final Span parentSpan,
    final UUID requestId,
    final IdEmail to,
    final Map<String, String> headers,
    final String subject,
    final String text)
  {
    Objects.requireNonNull(parentSpan, "span");
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(to, "to");
    Objects.requireNonNull(subject, "subject");
    Objects.requireNonNull(headers, "headers");
    Objects.requireNonNull(text, "text");

    final var future = new CompletableFuture<Void>();
    this.executor.execute(() -> {

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
          .setParent(Context.current().with(parentSpan))
          .startSpan();

      try {
        final Message message =
          new MimeMessage(this.session);
        final var addressFrom =
          new InternetAddress(this.configuration.senderAddress());
        message.setFrom(addressFrom);

        final var addressTo = new InternetAddress[1];
        addressTo[0] = new InternetAddress(to.value());
        message.setRecipients(Message.RecipientType.TO, addressTo);

        for (final var e : headers.entrySet()) {
          message.addHeader(e.getKey(), e.getValue());
        }

        message.setSubject(subject);
        message.setContent(text, "text/plain");
        Transport.send(message);

        future.complete(null);
      } catch (final Exception e) {
        span.recordException(e);
        future.completeExceptionally(e);
      } finally {
        span.end();
      }
    });
    return future;
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

  @Override
  public void close()
    throws Exception
  {
    this.executor.shutdown();
  }
}

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

package com.io7m.idstore.server.service.mail;

import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.server.api.IdServerMailConfiguration;
import com.io7m.idstore.server.api.IdServerMailTransportSMTP;
import com.io7m.idstore.server.api.IdServerMailTransportSMTPS;
import com.io7m.idstore.server.api.IdServerMailTransportSMTP_TLS;
import com.io7m.idstore.server.service.events.IdEventMailFailed;
import com.io7m.idstore.server.service.events.IdEventMailSent;
import com.io7m.idstore.server.service.events.IdEventServiceType;
import com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType.recordSpanException;

/**
 * A mail service.
 */

public final class IdServerMailService implements IdServerMailServiceType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdServerMailService.class);

  private final IdServerMailConfiguration configuration;
  private final IdServerTelemetryServiceType telemetry;
  private final Session session;
  private final ExecutorService executor;
  private final IdEventServiceType events;

  private IdServerMailService(
    final IdServerMailConfiguration inConfiguration,
    final IdServerTelemetryServiceType inTelemetry,
    final IdEventServiceType inEvents,
    final Session inSession,
    final ExecutorService inExecutor)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.telemetry =
      Objects.requireNonNull(inTelemetry, "telemetry");
    this.events =
      Objects.requireNonNull(inEvents, "inEvents");
    this.session =
      Objects.requireNonNull(inSession, "session");
    this.executor =
      Objects.requireNonNull(inExecutor, "executor");
  }

  /**
   * Create a new mail service.
   *
   * @param telemetry     The telemetry service
   * @param events        The events service
   * @param configuration The mail configuration
   *
   * @return The service
   */

  public static IdServerMailServiceType create(
    final IdServerTelemetryServiceType telemetry,
    final IdEventServiceType events,
    final IdServerMailConfiguration configuration)
  {
    Objects.requireNonNull(telemetry, "telemetry");
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

    final var p = new Properties();
    if (transport instanceof IdServerMailTransportSMTP) {
      p.setProperty("mail.transport.protocol", "smtp");
      p.setProperty("mail.smtp.host", transport.host());
      p.setProperty(
        "mail.smtp.port",
        Integer.toUnsignedString(transport.port()));
      p.setProperty("mail.smtp.starttls.enable", "true");
      p.setProperty("mail.smtp.starttls.required", "false");
      authOpt.ifPresent(auth -> {
        p.setProperty("mail.smtp.username", auth.userName());
        p.setProperty("mail.smtp.password", auth.password());
      });
    } else if (transport instanceof IdServerMailTransportSMTP_TLS) {
      p.setProperty("mail.transport.protocol", "smtp");
      p.setProperty("mail.smtp.host", transport.host());
      p.setProperty(
        "mail.smtp.port",
        Integer.toUnsignedString(transport.port()));
      p.setProperty("mail.smtp.starttls.enable", "true");
      p.setProperty("mail.smtp.starttls.required", "true");
      authOpt.ifPresent(auth -> {
        p.setProperty("mail.smtp.username", auth.userName());
        p.setProperty("mail.smtp.password", auth.password());
      });
    } else if (transport instanceof IdServerMailTransportSMTPS) {
      p.setProperty("mail.transport.protocol", "smtps");
      p.setProperty("mail.smtps.quitwait", "false");
      p.setProperty("mail.smtps.host", transport.host());
      p.setProperty(
        "mail.smtps.port",
        Integer.toUnsignedString(transport.port()));
      authOpt.ifPresent(auth -> {
        p.setProperty("mail.smtps.username", auth.userName());
        p.setProperty("mail.smtps.password", auth.password());
      });
    }

    final var session = Session.getDefaultInstance(p, null);
    session.setDebug(false);

    return new IdServerMailService(
      configuration,
      telemetry,
      events,
      session,
      executor
    );
  }

  @Override
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

      final var timeThen = OffsetDateTime.now();

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

        final var timeNow = OffsetDateTime.now();
        this.events.emit(
          new IdEventMailSent(to, Duration.between(timeThen, timeNow)));
        future.complete(null);
      } catch (final Exception e) {
        LOG.debug("send failed: ", e);
        final var timeNow = OffsetDateTime.now();
        this.events.emit(
          new IdEventMailFailed(to, Duration.between(timeThen, timeNow)));
        recordSpanException(e);
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
      .formatted(Long.toUnsignedString(this.hashCode(), 16));
  }

  @Override
  public void close()
  {
    this.executor.shutdown();
  }
}

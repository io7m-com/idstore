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


package com.io7m.idstore.server.controller.user_pwreset;

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseTransactionType;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdNonEmptyList;
import com.io7m.idstore.model.IdPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdToken;
import com.io7m.idstore.model.IdUserPasswordReset;
import com.io7m.idstore.model.IdValidityException;
import com.io7m.idstore.server.api.IdServerConfiguration;
import com.io7m.idstore.server.controller.IdServerStrings;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.clock.IdServerClock;
import com.io7m.idstore.server.service.mail.IdServerMailServiceType;
import com.io7m.idstore.server.service.ratelimit.IdRateLimitPasswordResetServiceType;
import com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType;
import com.io7m.idstore.server.service.templating.IdFMEmailPasswordResetData;
import com.io7m.idstore.server.service.templating.IdFMTemplateServiceType;
import com.io7m.idstore.server.service.templating.IdFMTemplateType;
import com.io7m.jdeferthrow.core.ExceptionTracker;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;

import java.io.StringWriter;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.HTTP_PARAMETER_INVALID;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.HTTP_PARAMETER_NONEXISTENT;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.IO_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.MAIL_SYSTEM_FAILURE;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PASSWORD_RESET_MISMATCH;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PASSWORD_RESET_NONEXISTENT;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.RATE_LIMIT_EXCEEDED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.USER_NONEXISTENT;
import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_CLIENT_IP;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_USER_AGENT;

/**
 * The user password reset service.
 */

public final class IdUserPasswordResetService
  implements IdUserPasswordResetServiceType
{
  private final IdServerTelemetryServiceType telemetry;
  private final IdServerBrandingServiceType branding;
  private final IdFMTemplateType<IdFMEmailPasswordResetData> emailTemplate;
  private final IdServerMailServiceType mailService;
  private final IdServerConfiguration configuration;
  private final IdServerClock clock;
  private final IdDatabaseType database;
  private final IdServerStrings strings;
  private final IdRateLimitPasswordResetServiceType rateLimit;

  private IdUserPasswordResetService(
    final IdServerTelemetryServiceType inTelemetry,
    final IdServerBrandingServiceType inBranding,
    final IdFMTemplateType<IdFMEmailPasswordResetData> inEmailTemplate,
    final IdServerMailServiceType inMailService,
    final IdServerConfiguration inConfiguration,
    final IdServerClock inClock,
    final IdDatabaseType inDatabase,
    final IdServerStrings inStrings,
    final IdRateLimitPasswordResetServiceType inRateLimit)
  {
    this.telemetry =
      Objects.requireNonNull(inTelemetry, "telemetry");
    this.branding =
      Objects.requireNonNull(inBranding, "branding");
    this.emailTemplate =
      Objects.requireNonNull(inEmailTemplate, "emailTemplate");
    this.mailService =
      Objects.requireNonNull(inMailService, "mailService");
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.clock =
      Objects.requireNonNull(inClock, "clock");
    this.database =
      Objects.requireNonNull(inDatabase, "database");
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.rateLimit =
      Objects.requireNonNull(inRateLimit, "rateLimit");
  }

  /**
   * Create a new password reset service.
   *
   * @param inTelemetry     The telemetry service
   * @param inBranding      The branding service
   * @param inTemplates     The template service
   * @param inMailService   The mail service
   * @param inConfiguration The configuration
   * @param inClock         The clock service
   * @param inDatabase      The database service
   * @param inStrings       The string resources
   * @param inRateLimit     The rate limit service
   *
   * @return A password reset service
   */

  public static IdUserPasswordResetServiceType create(
    final IdServerTelemetryServiceType inTelemetry,
    final IdServerBrandingServiceType inBranding,
    final IdFMTemplateServiceType inTemplates,
    final IdServerMailServiceType inMailService,
    final IdServerConfiguration inConfiguration,
    final IdServerClock inClock,
    final IdDatabaseType inDatabase,
    final IdServerStrings inStrings,
    final IdRateLimitPasswordResetServiceType inRateLimit)
  {
    return new IdUserPasswordResetService(
      inTelemetry,
      inBranding,
      inTemplates.emailPasswordResetTemplate(),
      inMailService,
      inConfiguration,
      inClock,
      inDatabase,
      inStrings,
      inRateLimit
    );
  }

  @Override
  public void resetBegin(
    final String sourceHost,
    final String userAgent,
    final UUID requestId,
    final Optional<String> email,
    final Optional<String> userName)
    throws IdCommandExecutionFailure
  {
    Objects.requireNonNull(sourceHost, "sourceHost");
    Objects.requireNonNull(userAgent, "userAgent");
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(email, "email");
    Objects.requireNonNull(userName, "userName");

    final var span =
      this.telemetry.tracer()
        .spanBuilder("IdUserPasswordResetService.resetBegin")
        .setSpanKind(CLIENT)
        .setAttribute(HTTP_USER_AGENT, userAgent)
        .setAttribute(HTTP_CLIENT_IP, sourceHost)
        .setAttribute("http.request_id", requestId.toString())
        .setParent(Context.current().with(Span.current()))
        .startSpan();

    try (var ignored = span.makeCurrent()) {
      new OpResetBegin(
        this,
        sourceHost,
        userAgent,
        requestId,
        email,
        userName
      ).run();
    } catch (final Exception e) {
      span.recordException(e);
      throw e;
    } finally {
      span.end();
    }
  }

  @Override
  public String description()
  {
    return "User password reset service.";
  }

  @Override
  public IdToken resetCheck(
    final String sourceHost,
    final String userAgent,
    final UUID requestId,
    final Optional<String> token)
    throws IdCommandExecutionFailure
  {
    Objects.requireNonNull(sourceHost, "sourceHost");
    Objects.requireNonNull(userAgent, "userAgent");
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(token, "token");

    final var span =
      this.telemetry.tracer()
        .spanBuilder("IdUserPasswordResetService.resetCheck")
        .setSpanKind(CLIENT)
        .setAttribute(HTTP_USER_AGENT, userAgent)
        .setAttribute(HTTP_CLIENT_IP, sourceHost)
        .setAttribute("http.request_id", requestId.toString())
        .setParent(Context.current().with(Span.current()))
        .startSpan();

    try (var ignored = span.makeCurrent()) {
      return new OpResetCheck(
        this,
        requestId,
        token
      ).run();
    } catch (final Exception e) {
      span.recordException(e);
      throw e;
    } finally {
      span.end();
    }
  }

  @Override
  public void resetConfirm(
    final String sourceHost,
    final String userAgent,
    final UUID requestId,
    final Optional<String> password0Opt,
    final Optional<String> password1Opt,
    final Optional<String> tokenOpt)
    throws IdCommandExecutionFailure
  {
    Objects.requireNonNull(sourceHost, "sourceHost");
    Objects.requireNonNull(userAgent, "userAgent");
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(password0Opt, "password0Opt");
    Objects.requireNonNull(password1Opt, "password1Opt");
    Objects.requireNonNull(tokenOpt, "tokenOpt");

    final var span =
      this.telemetry.tracer()
        .spanBuilder("IdUserPasswordResetService.resetConfirm")
        .setSpanKind(CLIENT)
        .setAttribute(HTTP_USER_AGENT, userAgent)
        .setAttribute(HTTP_CLIENT_IP, sourceHost)
        .setAttribute("http.request_id", requestId.toString())
        .setParent(Context.current().with(Span.current()))
        .startSpan();

    try (var ignored = span.makeCurrent()) {
      new OpResetConfirm(
        this,
        requestId,
        password0Opt,
        password1Opt,
        tokenOpt
      ).run();
    } catch (final Exception e) {
      span.recordException(e);
      throw e;
    } finally {
      span.end();
    }
  }

  @Override
  public String toString()
  {
    return "[IdUserPasswordResetService 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode(), 16));
  }

  private static final class OpResetConfirm
  {
    private final IdUserPasswordResetService service;
    private final UUID requestId;
    private final Optional<String> password0Opt;
    private final Optional<String> password1Opt;
    private final Optional<String> tokenOpt;
    private String password0;
    private String password1;
    private IdToken token;

    OpResetConfirm(
      final IdUserPasswordResetService inService,
      final UUID inRequestId,
      final Optional<String> inPassword0Opt,
      final Optional<String> inPassword1Opt,
      final Optional<String> inTokenOpt)
    {
      this.service = inService;
      this.requestId = inRequestId;
      this.password0Opt = inPassword0Opt;
      this.password1Opt = inPassword1Opt;
      this.tokenOpt = inTokenOpt;
    }

    public void run()
      throws IdCommandExecutionFailure
    {
      this.checkParameters();
      this.executeNewPassword();
    }

    private void executeNewPassword()
      throws IdCommandExecutionFailure
    {
      Span.current()
        .setAttribute("password.reset_token", this.token.value());

      try (var connection =
             this.service.database.openConnection(IDSTORE)) {
        try (var transaction =
               connection.openTransaction()) {
          final var queries =
            transaction.queries(IdDatabaseUsersQueriesType.class);
          final var resetOpt =
            queries.userPasswordResetGetForToken(this.token);

          if (resetOpt.isEmpty()) {
            throw new IdCommandExecutionFailure(
              this.service.strings.format("passwordResetNonexistent"),
              PASSWORD_RESET_NONEXISTENT,
              Map.of(),
              Optional.empty(),
              this.requestId,
              400
            );
          }

          final var reset = resetOpt.get();
          final var timeNow = this.service.clock.now();
          if (timeNow.isAfter(reset.expires())) {
            throw new IdCommandExecutionFailure(
              this.service.strings.format("passwordResetNonexistent"),
              PASSWORD_RESET_NONEXISTENT,
              Map.of(),
              Optional.empty(),
              this.requestId,
              400
            );
          }

          final var password =
            IdPasswordAlgorithmPBKDF2HmacSHA256.create()
              .createHashed(this.password0);

          transaction.userIdSet(reset.user());

          queries.userUpdate(
            reset.user(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(password)
          );

          queries.userPasswordResetDelete(reset);
          transaction.commit();
        }
      } catch (final IdDatabaseException | IdPasswordException e) {
        throw new IdCommandExecutionFailure(
          e.getMessage(),
          e,
          e.errorCode(),
          e.attributes(),
          e.remediatingAction(),
          this.requestId,
          500
        );
      }
    }

    private void checkParameters()
      throws IdCommandExecutionFailure
    {
      if (this.password0Opt.isEmpty()) {
        throw new IdCommandExecutionFailure(
          this.service.strings.format("missingParameter", "password0"),
          HTTP_PARAMETER_NONEXISTENT,
          Map.of(),
          Optional.empty(),
          this.requestId,
          400
        );
      }

      if (this.password1Opt.isEmpty()) {
        throw new IdCommandExecutionFailure(
          this.service.strings.format("missingParameter", "password1"),
          HTTP_PARAMETER_NONEXISTENT,
          Map.of(),
          Optional.empty(),
          this.requestId,
          400
        );
      }

      if (this.tokenOpt.isEmpty()) {
        throw new IdCommandExecutionFailure(
          this.service.strings.format("missingParameter", "token"),
          HTTP_PARAMETER_NONEXISTENT,
          Map.of(),
          Optional.empty(),
          this.requestId,
          400
        );
      }

      try {
        this.password0 = this.password0Opt.get();
        this.password1 = this.password1Opt.get();
        this.token = new IdToken(this.tokenOpt.get());
      } catch (final IdValidityException e) {
        throw new IdCommandExecutionFailure(
          e.getMessage(),
          e,
          HTTP_PARAMETER_INVALID,
          Map.of(),
          Optional.empty(),
          this.requestId,
          400
        );
      }

      if (!this.password0.equals(this.password1)) {
        throw new IdCommandExecutionFailure(
          this.service.strings.format("passwordResetMismatch"),
          PASSWORD_RESET_MISMATCH,
          Map.of(),
          Optional.empty(),
          this.requestId,
          400
        );
      }
    }
  }

  private static final class OpResetCheck
  {
    private final IdUserPasswordResetService service;
    private final UUID requestId;
    private final Optional<String> tokenOpt;
    private IdToken token;

    OpResetCheck(
      final IdUserPasswordResetService inService,
      final UUID inRequestId,
      final Optional<String> inToken)
    {
      this.service = inService;
      this.requestId = inRequestId;
      this.tokenOpt = inToken;
    }

    public IdToken run()
      throws IdCommandExecutionFailure
    {
      this.checkParameters();
      this.executeCheck();
      return this.token;
    }

    private void executeCheck()
      throws IdCommandExecutionFailure
    {
      Span.current()
        .setAttribute("password.reset_token", this.token.value());

      try (var connection =
             this.service.database.openConnection(IDSTORE)) {
        try (var transaction =
               connection.openTransaction()) {
          final var queries =
            transaction.queries(IdDatabaseUsersQueriesType.class);
          final var resetOpt =
            queries.userPasswordResetGetForToken(this.token);

          if (resetOpt.isEmpty()) {
            throw new IdCommandExecutionFailure(
              this.service.strings.format("passwordResetNonexistent"),
              PASSWORD_RESET_NONEXISTENT,
              Map.of(),
              Optional.empty(),
              this.requestId,
              400
            );
          }

          final var reset = resetOpt.get();
          final var timeNow = this.service.clock.now();
          if (timeNow.isAfter(reset.expires())) {
            throw new IdCommandExecutionFailure(
              this.service.strings.format("passwordResetNonexistent"),
              PASSWORD_RESET_NONEXISTENT,
              Map.of(),
              Optional.empty(),
              this.requestId,
              400
            );
          }
        }
      } catch (final IdDatabaseException e) {
        throw new IdCommandExecutionFailure(
          e.getMessage(),
          e,
          e.errorCode(),
          e.attributes(),
          e.remediatingAction(),
          this.requestId,
          500
        );
      }
    }

    private void checkParameters()
      throws IdCommandExecutionFailure
    {
      if (this.tokenOpt.isEmpty()) {
        throw new IdCommandExecutionFailure(
          this.service.strings.format("missingParameter", "token"),
          HTTP_PARAMETER_NONEXISTENT,
          Map.of(),
          Optional.empty(),
          this.requestId,
          400
        );
      }

      try {
        this.token = new IdToken(this.tokenOpt.get());
      } catch (final IdValidityException e) {
        throw new IdCommandExecutionFailure(
          e.getMessage(),
          e,
          HTTP_PARAMETER_INVALID,
          Map.of(),
          Optional.empty(),
          this.requestId,
          400
        );
      }
    }
  }

  private static final class OpResetBegin
  {
    private final IdUserPasswordResetService service;
    private final String sourceHost;
    private final String userAgent;
    private final UUID requestId;
    private final Optional<String> emailOpt;
    private final Optional<String> userNameOpt;

    OpResetBegin(
      final IdUserPasswordResetService inService,
      final String inSourceHost,
      final String inUserAgent,
      final UUID inRequestId,
      final Optional<String> inEmail,
      final Optional<String> inUserName)
    {
      this.service = inService;
      this.sourceHost = inSourceHost;
      this.userAgent = inUserAgent;
      this.requestId = inRequestId;
      this.emailOpt = inEmail;
      this.userNameOpt = inUserName;
    }

    public void run()
      throws IdCommandExecutionFailure
    {
      try {
        if (!this.service.rateLimit.isAllowedByRateLimit(this.sourceHost)) {
          throw new IdCommandExecutionFailure(
            this.service.strings.format("pwResetRateLimited"),
            RATE_LIMIT_EXCEEDED,
            Map.of(),
            Optional.empty(),
            this.requestId,
            400
          );
        }

        if (this.emailOpt.isEmpty() && this.userNameOpt.isEmpty()) {
          throw new IdCommandExecutionFailure(
            this.service.strings.format("pwResetRequireParameters"),
            HTTP_PARAMETER_NONEXISTENT,
            Map.of(),
            Optional.empty(),
            this.requestId,
            400
          );
        }

        if (this.userNameOpt.isPresent()) {
          this.resetBeginForUserName(new IdName(this.userNameOpt.get()));
          return;
        }

        this.resetBeginForEmail(new IdEmail(this.emailOpt.get()));
      } catch (final IdDatabaseException e) {
        throw new IdCommandExecutionFailure(
          e.getMessage(),
          e,
          e.errorCode(),
          e.attributes(),
          e.remediatingAction(),
          this.requestId,
          500
        );
      } catch (final IdValidityException e) {
        throw new IdCommandExecutionFailure(
          e.getMessage(),
          e,
          HTTP_PARAMETER_INVALID,
          Map.of(),
          Optional.empty(),
          this.requestId,
          500
        );
      }
    }

    private void resetBeginForUserName(
      final IdName idName)
      throws IdCommandExecutionFailure, IdDatabaseException
    {
      try (var connection =
             this.service.database.openConnection(IDSTORE)) {
        try (var transaction =
               connection.openTransaction()) {
          final var queries =
            transaction.queries(IdDatabaseUsersQueriesType.class);
          final var userOpt =
            queries.userGetForName(idName);

          if (userOpt.isEmpty()) {
            throw new IdCommandExecutionFailure(
              this.service.strings.format("userNonexistent"),
              USER_NONEXISTENT,
              Map.of(),
              Optional.empty(),
              this.requestId,
              400
            );
          }

          final var user = userOpt.get();
          this.createLinkAndSendEmails(
            transaction,
            user.id(),
            user.emails()
          );
        }
      }
    }

    private void createLinkAndSendEmails(
      final IdDatabaseTransactionType transaction,
      final UUID id,
      final IdNonEmptyList<IdEmail> emails)
      throws IdDatabaseException, IdCommandExecutionFailure
    {
      final var users =
        transaction.queries(IdDatabaseUsersQueriesType.class);

      final var timeNow =
        this.service.clock.now();
      final var timeExpires =
        timeNow.plus(
          this.service.configuration.mailConfiguration()
            .verificationExpiration()
        );

      final var reset =
        new IdUserPasswordReset(id, IdToken.generate(), timeExpires);

      users.userPasswordResetCreate(reset);
      transaction.commit();

      final var exceptions =
        new ExceptionTracker<IdCommandExecutionFailure>();

      for (final var email : emails) {
        try {
          this.sendMail(email, reset);
        } catch (final IdCommandExecutionFailure e) {
          exceptions.addException(e);
        }
      }

      exceptions.throwIfNecessary();
    }

    private void sendMail(
      final IdEmail email,
      final IdUserPasswordReset reset)
      throws IdCommandExecutionFailure
    {
      final var linkConfirm =
        this.service.configuration.userViewAddress()
          .externalAddress()
          .resolve("/password-reset-confirm/?token=%s".formatted(reset.token()))
          .normalize();

      final var writer = new StringWriter();
      try {
        this.service.emailTemplate.process(
          new IdFMEmailPasswordResetData(
            this.service.branding.title(),
            reset,
            this.sourceHost,
            this.userAgent,
            linkConfirm
          ),
          writer
        );
      } catch (final Exception e) {
        throw new IdCommandExecutionFailure(
          e.getMessage(),
          e,
          IO_ERROR,
          Map.of(),
          Optional.empty(),
          this.requestId,
          500
        );
      }

      final var mailHeaders =
        Map.ofEntries(
          Map.entry(
            "X-IDStore-PasswordReset-Token",
            reset.token().value()),
          Map.entry(
            "X-IDStore-PasswordReset-From-Request",
            this.requestId.toString()),
          Map.entry(
            "X-IDStore-PasswordReset-Confirm",
            linkConfirm.toString())
        );

      try {
        this.service.mailService.sendMail(
          Span.current(),
          this.requestId,
          email,
          mailHeaders,
          this.service.branding.emailSubject("Password reset request"),
          writer.toString()
        ).get();
      } catch (final Exception e) {
        throw new IdCommandExecutionFailure(
          e.getMessage(),
          e,
          MAIL_SYSTEM_FAILURE,
          Map.of(),
          Optional.empty(),
          this.requestId,
          500
        );
      }
    }

    private void resetBeginForEmail(
      final IdEmail email)
      throws IdDatabaseException, IdCommandExecutionFailure
    {
      try (var connection =
             this.service.database.openConnection(IDSTORE)) {
        try (var transaction =
               connection.openTransaction()) {
          final var queries =
            transaction.queries(IdDatabaseUsersQueriesType.class);
          final var userOpt =
            queries.userGetForEmail(email);

          if (userOpt.isEmpty()) {
            throw new IdCommandExecutionFailure(
              this.service.strings.format("userNonexistent"),
              USER_NONEXISTENT,
              Map.of(),
              Optional.empty(),
              this.requestId,
              400
            );
          }

          final var user = userOpt.get();
          this.createLinkAndSendEmails(
            transaction,
            user.id(),
            user.emails()
          );
        }
      }
    }
  }
}

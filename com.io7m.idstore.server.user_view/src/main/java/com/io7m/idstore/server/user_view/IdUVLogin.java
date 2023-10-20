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

package com.io7m.idstore.server.user_view;

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.server.api.IdServerRateLimitConfiguration;
import com.io7m.idstore.server.api.IdServerSessionConfiguration;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.controller.user.IdUserLoggedIn;
import com.io7m.idstore.server.controller.user.IdUserLoginService;
import com.io7m.idstore.server.http.IdHTTPCookieDeclaration;
import com.io7m.idstore.server.http.IdHTTPHandlerFunctional;
import com.io7m.idstore.server.http.IdHTTPHandlerFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPRequestInformation;
import com.io7m.idstore.server.http.IdHTTPResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPResponseRedirect;
import com.io7m.idstore.server.http.IdHTTPResponseType;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationService;
import com.io7m.idstore.server.service.sessions.IdSessionMessage;
import com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType;
import com.io7m.idstore.server.service.templating.IdFMLoginData;
import com.io7m.idstore.server.service.templating.IdFMTemplateServiceType;
import com.io7m.idstore.server.service.templating.IdFMTemplateType;
import com.io7m.idstore.strings.IdStrings;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import freemarker.template.TemplateException;
import io.helidon.common.parameters.Parameters;
import io.helidon.webserver.http.ServerRequest;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.model.IdLoginMetadataStandard.remoteHost;
import static com.io7m.idstore.model.IdLoginMetadataStandard.userAgent;
import static com.io7m.idstore.model.IdUserDomain.USER;
import static com.io7m.idstore.server.http.IdHTTPHandlerCoreInstrumented.withInstrumentation;
import static com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType.setSpanErrorCode;
import static com.io7m.idstore.server.user_view.IdUVHandlerCoreMaintenanceAware.withMaintenanceAwareness;
import static com.io7m.idstore.strings.IdStringConstants.ERROR_INVALID_USERNAME_PASSWORD;

/**
 * The page that displays the login form, or executes the login if a username
 * and password is provided.
 */

public final class IdUVLogin extends IdHTTPHandlerFunctional
{
  /**
   * The page that displays the login form, or executes the login if a username
   * and password is provided.
   *
   * @param services The services
   */

  public IdUVLogin(
    final RPServiceDirectoryType services)
  {
    super(createCore(services));
  }

  private static IdHTTPHandlerFunctionalCoreType createCore(
    final RPServiceDirectoryType services)
  {
    final var database =
      services.requireService(IdDatabaseType.class);
    final var branding =
      services.requireService(IdServerBrandingServiceType.class);
    final var logins =
      services.requireService(IdUserLoginService.class);
    final var strings =
      services.requireService(IdStrings.class);
    final var template =
      services.requireService(IdFMTemplateServiceType.class)
        .pageLoginTemplate();
    final var telemetry =
      services.requireService(IdServerTelemetryServiceType.class);
    final var rateLimit =
      services.requireService(IdServerConfigurationService.class)
        .configuration()
        .rateLimit();
    final var sessions =
      services.requireService(IdServerConfigurationService.class)
        .configuration()
        .sessions();

    final IdHTTPHandlerFunctionalCoreType main =
      (request, information) -> {
        return execute(
          database,
          branding,
          logins,
          strings,
          template,
          telemetry,
          rateLimit,
          sessions,
          request,
          information
        );
      };

    final var maintenanceAware =
      withMaintenanceAwareness(services, main);
    return withInstrumentation(services, USER, maintenanceAware);
  }

  private static IdHTTPResponseType execute(
    final IdDatabaseType database,
    final IdServerBrandingServiceType branding,
    final IdUserLoginService logins,
    final IdStrings strings,
    final IdFMTemplateType<IdFMLoginData> template,
    final IdServerTelemetryServiceType telemetry,
    final IdServerRateLimitConfiguration rateLimit,
    final IdServerSessionConfiguration sessions,
    final ServerRequest request,
    final IdHTTPRequestInformation information)
  {
    final var parameters =
      request.content().as(Parameters.class);
    final var username =
      parameters.first("username")
        .orElse(null);
    final var password =
      parameters.first("password")
        .orElse(null);

    if (username == null || password == null) {
      return showLoginForm(branding, template, Optional.empty(), 200);
    }

    applyFixedDelay(telemetry, rateLimit.userLoginDelay());

    try (var connection = database.openConnection(IDSTORE)) {
      try (var transaction = connection.openTransaction()) {
        final var metadata = new HashMap<String, String>(2);
        metadata.put(userAgent(), information.userAgent());
        metadata.put(remoteHost(), information.remoteAddress());

        final IdUserLoggedIn loggedIn;
        try {
          loggedIn = logins.userLogin(
            transaction,
            information.requestId(),
            information.remoteAddress(),
            username,
            password,
            metadata
          );
        } catch (final IdCommandExecutionFailure e) {
          setSpanErrorCode(e.errorCode());
          return showLoginForm(
            branding,
            template,
            Optional.of(new IdSessionMessage(
              information.requestId(),
              true,
              false,
              "",
              strings.format(ERROR_INVALID_USERNAME_PASSWORD),
              "/"
            )),
            401
          );
        }

        transaction.commit();
        return new IdHTTPResponseRedirect(
          Set.of(
            new IdHTTPCookieDeclaration(
              "IDSTORE_USER_VIEW_SESSION",
              loggedIn.session().id().value(),
              sessions.userSessionExpiration()
            )
          ),
          "/"
        );
      }
    } catch (final IdDatabaseException e) {
      setSpanErrorCode(e.errorCode());
      return showLoginForm(
        branding,
        template,
        Optional.of(new IdSessionMessage(
          information.requestId(),
          true,
          false,
          "",
          e.getMessage(),
          "/"
        )),
        401
      );
    }
  }

  private static void applyFixedDelay(
    final IdServerTelemetryServiceType telemetry,
    final Duration duration)
  {
    try {
      final var childSpan =
        telemetry.tracer()
          .spanBuilder("FixedDelay")
          .startSpan();

      try (var ignored = childSpan.makeCurrent()) {
        Thread.sleep(duration.toMillis());
      } finally {
        childSpan.end();
      }
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Display a login form.
   *
   * @param branding   The branding resources
   * @param template   The page template
   * @param message    The error message, if any
   * @param statusCode The status code
   *
   * @return A login form
   */

  public static IdHTTPResponseType showLoginForm(
    final IdServerBrandingServiceType branding,
    final IdFMTemplateType<IdFMLoginData> template,
    final Optional<IdSessionMessage> message,
    final int statusCode)
  {
    try (var writer = new StringWriter()) {
      template.process(
        new IdFMLoginData(
          branding.htmlTitle("Login"),
          branding.title(),
          true,
          Optional.empty(),
          message.map(IdSessionMessage::message),
          branding.loginExtraText()
        ),
        writer
      );

      writer.flush();
      return new IdHTTPResponseFixedSize(
        statusCode,
        Set.of(),
        IdUVContentTypes.xhtml(),
        writer.toString().getBytes(StandardCharsets.UTF_8)
      );
    } catch (final TemplateException e) {
      throw new IllegalStateException(e);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}

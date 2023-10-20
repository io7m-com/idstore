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

import com.io7m.idstore.model.IdValidityException;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.controller.user_pwreset.IdUserPasswordResetServiceType;
import com.io7m.idstore.server.http.IdHTTPHandlerFunctional;
import com.io7m.idstore.server.http.IdHTTPHandlerFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPRequestInformation;
import com.io7m.idstore.server.http.IdHTTPResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPResponseType;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.templating.IdFMMessageData;
import com.io7m.idstore.server.service.templating.IdFMTemplateServiceType;
import com.io7m.idstore.server.service.templating.IdFMTemplateType;
import com.io7m.idstore.strings.IdStrings;
import com.io7m.jvindicator.core.Vindication;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import freemarker.template.TemplateException;
import io.helidon.webserver.http.ServerRequest;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.Set;

import static com.io7m.idstore.model.IdUserDomain.USER;
import static com.io7m.idstore.server.http.IdHTTPHandlerCoreInstrumented.withInstrumentation;
import static com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType.setSpanErrorCode;
import static com.io7m.idstore.server.user_view.IdUVHandlerCoreMaintenanceAware.withMaintenanceAwareness;
import static com.io7m.idstore.strings.IdStringConstants.EMAIL_PASSWORD_RESET_SENT;
import static com.io7m.idstore.strings.IdStringConstants.EMAIL_PASSWORD_RESET_TITLE;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The page that triggers sending a reset link.
 */

public final class IdUVPasswordResetRun extends IdHTTPHandlerFunctional
{
  private static final String DESTINATION_ON_FAILURE = "/";

  /**
   * The page that triggers sending a reset link.
   *
   * @param services The services
   */

  public IdUVPasswordResetRun(
    final RPServiceDirectoryType services)
  {
    super(createCore(services));
  }

  private static IdHTTPHandlerFunctionalCoreType createCore(
    final RPServiceDirectoryType services)
  {
    final var userPasswordResets =
      services.requireService(IdUserPasswordResetServiceType.class);
    final var strings =
      services.requireService(IdStrings.class);
    final var branding =
      services.requireService(IdServerBrandingServiceType.class);
    final var errorTemplate =
      services.requireService(IdFMTemplateServiceType.class)
        .pageMessage();

    final IdHTTPHandlerFunctionalCoreType main =
      (request, information) -> {
        return execute(
          userPasswordResets,
          strings,
          branding,
          errorTemplate,
          request,
          information
        );
      };

    final var maintenanceAware = withMaintenanceAwareness(services, main);
    return withInstrumentation(services, USER, maintenanceAware);
  }

  private static IdHTTPResponseType execute(
    final IdUserPasswordResetServiceType userPasswordResets,
    final IdStrings strings,
    final IdServerBrandingServiceType branding,
    final IdFMTemplateType<IdFMMessageData> errorTemplate,
    final ServerRequest request,
    final IdHTTPRequestInformation information)
  {
    final var vindicator =
      Vindication.startWithExceptions(IdValidityException::new);
    final var emailParameter =
      vindicator.addRequiredParameter("email", x -> x);
    final var nameParameter =
      vindicator.addRequiredParameter("username", x -> x);

    try {
      vindicator.check(request.query().toMap());
    } catch (final IdValidityException e) {
      return IdUVErrorPage.showError(
        strings,
        branding,
        errorTemplate,
        information,
        400,
        e.getMessage(),
        DESTINATION_ON_FAILURE
      );
    }

    try {
      userPasswordResets.resetBegin(
        information.remoteAddress(),
        information.userAgent(),
        information.requestId(),
        Optional.ofNullable(emailParameter.get()),
        Optional.ofNullable(nameParameter.get())
      );
    } catch (final IdCommandExecutionFailure e) {
      setSpanErrorCode(e.errorCode());
      return IdUVErrorPage.showError(
        strings,
        branding,
        errorTemplate,
        information,
        e.httpStatusCode(),
        e.getMessage(),
        DESTINATION_ON_FAILURE
      );
    }

    return showSent(strings, branding, errorTemplate, information);
  }

  private static IdHTTPResponseType showSent(
    final IdStrings strings,
    final IdServerBrandingServiceType branding,
    final IdFMTemplateType<IdFMMessageData> errorTemplate,
    final IdHTTPRequestInformation information)
  {
    try (var writer = new StringWriter()) {
      errorTemplate.process(
        new IdFMMessageData(
          branding.htmlTitle(strings.format(EMAIL_PASSWORD_RESET_TITLE)),
          branding.title(),
          information.requestId(),
          false,
          false,
          strings.format(EMAIL_PASSWORD_RESET_TITLE),
          strings.format(EMAIL_PASSWORD_RESET_SENT),
          "/"
        ),
        writer
      );
      writer.flush();
      return new IdHTTPResponseFixedSize(
        200,
        Set.of(),
        IdUVContentTypes.xhtml(),
        writer.toString().getBytes(UTF_8)
      );
    } catch (final TemplateException e) {
      throw new IllegalStateException(e);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}

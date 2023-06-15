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
import com.io7m.idstore.server.controller.IdServerStrings;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.controller.user_pwreset.IdUserPasswordResetServiceType;
import com.io7m.idstore.server.http.IdHTTPServletFunctional;
import com.io7m.idstore.server.http.IdHTTPServletFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPServletRequestInformation;
import com.io7m.idstore.server.http.IdHTTPServletResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPServletResponseType;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.templating.IdFMMessageData;
import com.io7m.idstore.server.service.templating.IdFMTemplateServiceType;
import com.io7m.idstore.server.service.templating.IdFMTemplateType;
import com.io7m.jvindicator.core.Vindication;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import freemarker.template.TemplateException;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Optional;

import static com.io7m.idstore.model.IdUserDomain.USER;
import static com.io7m.idstore.server.http.IdHTTPServletCoreInstrumented.withInstrumentation;
import static com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType.setSpanErrorCode;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The page that triggers sending a reset link.
 */

public final class IdUVPasswordResetRun extends IdHTTPServletFunctional
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

  private static IdHTTPServletFunctionalCoreType createCore(
    final RPServiceDirectoryType services)
  {
    final var userPasswordResets =
      services.requireService(IdUserPasswordResetServiceType.class);
    final var strings =
      services.requireService(IdServerStrings.class);
    final var branding =
      services.requireService(IdServerBrandingServiceType.class);
    final var errorTemplate =
      services.requireService(IdFMTemplateServiceType.class)
        .pageMessage();

    return withInstrumentation(services, USER, (request, information) -> {
      return execute(
        userPasswordResets,
        strings,
        branding,
        errorTemplate,
        request,
        information
      );
    });
  }

  private static IdHTTPServletResponseType execute(
    final IdUserPasswordResetServiceType userPasswordResets,
    final IdServerStrings strings,
    final IdServerBrandingServiceType branding,
    final IdFMTemplateType<IdFMMessageData> errorTemplate,
    final HttpServletRequest request,
    final IdHTTPServletRequestInformation information)
  {
    final var vindicator =
      Vindication.startWithExceptions(IdValidityException::new);
    final var emailParameter =
      vindicator.addRequiredParameter("email", x -> x);
    final var nameParameter =
      vindicator.addRequiredParameter("username", x -> x);

    try {
      vindicator.check(request.getParameterMap());
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

  private static IdHTTPServletResponseType showSent(
    final IdServerStrings strings,
    final IdServerBrandingServiceType branding,
    final IdFMTemplateType<IdFMMessageData> errorTemplate,
    final IdHTTPServletRequestInformation information)
  {
    try (var writer = new StringWriter()) {
      errorTemplate.process(
        new IdFMMessageData(
          branding.htmlTitle(strings.format("emailPasswordResetTitle")),
          branding.title(),
          information.requestId(),
          false,
          false,
          strings.format("emailPasswordResetTitle"),
          strings.format("emailPasswordResetSent"),
          "/"
        ),
        writer
      );
      writer.flush();
      return new IdHTTPServletResponseFixedSize(
        200,
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

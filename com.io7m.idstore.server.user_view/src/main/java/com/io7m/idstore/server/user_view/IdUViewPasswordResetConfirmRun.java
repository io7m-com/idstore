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


package com.io7m.idstore.server.user_view;

import com.io7m.idstore.model.IdValidityException;
import com.io7m.idstore.server.controller.IdServerStrings;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.controller.user_pwreset.IdUserPasswordResetServiceType;
import com.io7m.idstore.server.http.IdCommonInstrumentedServlet;
import com.io7m.idstore.server.http.IdRequestUniqueIDs;
import com.io7m.idstore.server.http.IdRequestUserAgents;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.templating.IdFMMessageData;
import com.io7m.idstore.server.service.templating.IdFMTemplateServiceType;
import com.io7m.idstore.server.service.templating.IdFMTemplateType;
import com.io7m.idstore.services.api.IdServiceDirectoryType;
import com.io7m.jvindicator.core.Vindication;
import freemarker.template.TemplateException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;

/**
 * The page that completes a password reset.
 */

public final class IdUViewPasswordResetConfirmRun
  extends IdCommonInstrumentedServlet
{
  private final IdUserPasswordResetServiceType userPasswordResets;
  private final IdServerStrings strings;
  private final IdServerBrandingServiceType branding;
  private final IdFMTemplateType<IdFMMessageData> errorTemplate;

  /**
   * The page that completes a password reset.
   *
   * @param inServices The service directory
   */

  public IdUViewPasswordResetConfirmRun(
    final IdServiceDirectoryType inServices)
  {
    super(inServices);

    this.userPasswordResets =
      inServices.requireService(IdUserPasswordResetServiceType.class);
    this.strings =
      inServices.requireService(IdServerStrings.class);
    this.branding =
      inServices.requireService(IdServerBrandingServiceType.class);
    this.errorTemplate =
      inServices.requireService(IdFMTemplateServiceType.class)
        .pageMessage();
  }

  @Override
  protected void service(
    final HttpServletRequest request,
    final HttpServletResponse response)
    throws ServletException, IOException
  {
    try {
      final var vindicator =
        Vindication.startWithExceptions(IdValidityException::new);

      final var password0Parameter =
        vindicator.addRequiredParameter("password0", x -> x);
      final var password1Parameter =
        vindicator.addRequiredParameter("password1", x -> x);
      final var tokenParameter =
        vindicator.addRequiredParameter("token", x -> x);

      vindicator.check(request.getParameterMap());

      this.userPasswordResets.resetConfirm(
        request.getRemoteHost(),
        IdRequestUserAgents.requestUserAgent(request),
        IdRequestUniqueIDs.requestIdFor(request),
        Optional.ofNullable(password0Parameter.get()),
        Optional.ofNullable(password1Parameter.get()),
        Optional.ofNullable(tokenParameter.get())
      );

      this.showConfirmed(request, response);
    } catch (final IdCommandExecutionFailure e) {
      this.showError(
        request,
        response,
        e.httpStatusCode(),
        e.getMessage()
      );
    } catch (final IdValidityException e) {
      this.showError(request, response, 400, e.getMessage());
    }
  }

  private void showConfirmed(
    final HttpServletRequest request,
    final HttpServletResponse response)
    throws IOException
  {
    response.setStatus(200);

    try (var writer = response.getWriter()) {
      this.errorTemplate.process(
        new IdFMMessageData(
          this.branding.htmlTitle(this.strings.format("emailPasswordResetSuccessTitle")),
          this.branding.title(),
          IdRequestUniqueIDs.requestIdFor(request),
          false,
          false,
          this.strings.format("emailPasswordResetSuccessTitle"),
          this.strings.format("emailPasswordResetSuccess"),
          "/"
        ),
        writer
      );
    } catch (final TemplateException e) {
      throw new IOException(e);
    }
  }

  private void showError(
    final HttpServletRequest request,
    final HttpServletResponse response,
    final int statusCode,
    final String message)
    throws IOException
  {
    response.setStatus(statusCode);

    try (var writer = response.getWriter()) {
      this.errorTemplate.process(
        new IdFMMessageData(
          this.branding.htmlTitle(this.strings.format("error")),
          this.branding.title(),
          IdRequestUniqueIDs.requestIdFor(request),
          true,
          statusCode >= 500,
          this.strings.format("error"),
          message,
          "/"
        ),
        writer
      );
    } catch (final TemplateException e) {
      throw new IOException(e);
    }
  }
}

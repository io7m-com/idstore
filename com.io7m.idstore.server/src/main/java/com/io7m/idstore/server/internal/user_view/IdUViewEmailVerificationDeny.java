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


package com.io7m.idstore.server.internal.user_view;

import com.io7m.idstore.database.api.IdDatabaseEmailsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.model.IdToken;
import com.io7m.idstore.protocol.user_v1.IdU1CommandEmailAddDeny;
import com.io7m.idstore.protocol.user_v1.IdU1CommandEmailRemoveDeny;
import com.io7m.idstore.server.internal.IdServerBrandingService;
import com.io7m.idstore.server.internal.IdServerClock;
import com.io7m.idstore.server.internal.IdServerStrings;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.internal.freemarker.IdFMMessageData;
import com.io7m.idstore.server.internal.freemarker.IdFMTemplateService;
import com.io7m.idstore.server.internal.freemarker.IdFMTemplateType;
import com.io7m.idstore.server.internal.user_v1.IdU1CmdEmailAddDeny;
import com.io7m.idstore.server.internal.user_v1.IdU1CmdEmailRemoveDeny;
import com.io7m.idstore.server.internal.user_v1.IdU1CommandContext;
import com.io7m.idstore.services.api.IdServiceDirectoryType;
import freemarker.template.TemplateException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Objects;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.server.internal.IdRequests.requestUserAgent;
import static com.io7m.idstore.server.internal.IdServerRequestDecoration.requestIdFor;

/**
 * The endpoint that allows for completing email verification challenges.
 */

public final class IdUViewEmailVerificationDeny extends HttpServlet
{
  private final IdDatabaseType database;
  private final IdServerStrings strings;
  private final IdFMTemplateType<IdFMMessageData> template;
  private final IdServiceDirectoryType services;
  private final IdServerClock clock;
  private final IdServerBrandingService branding;

  /**
   * The endpoint that allows for completing email verification challenges.
   *
   * @param inServices The services
   */

  public IdUViewEmailVerificationDeny(
    final IdServiceDirectoryType inServices)
  {
    Objects.requireNonNull(inServices, "inServices");

    this.services =
      Objects.requireNonNull(inServices, "inServices");
    this.database =
      inServices.requireService(IdDatabaseType.class);
    this.strings =
      inServices.requireService(IdServerStrings.class);
    this.clock =
      inServices.requireService(IdServerClock.class);
    this.branding =
      inServices.requireService(IdServerBrandingService.class);
    this.template =
      inServices.requireService(IdFMTemplateService.class)
        .pageMessage();
  }

  @Override
  protected void service(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse)
    throws ServletException, IOException
  {
    final var tokenParameter =
      request.getParameter("token");

    if (tokenParameter == null) {
      this.showError(
        request,
        servletResponse,
        this.strings.format("missingParameter", "token"),
        false
      );
      return;
    }

    final IdToken token;
    try {
      token = new IdToken(tokenParameter);
    } catch (final Exception e) {
      this.showError(
        request,
        servletResponse,
        this.strings.format("invalidParameter", "token"),
        false
      );
      return;
    }

    try (var connection =
           this.database.openConnection(IDSTORE)) {
      try (var transaction =
             connection.openTransaction()) {
        final var emails =
          transaction.queries(IdDatabaseEmailsQueriesType.class);
        final var users =
          transaction.queries(IdDatabaseUsersQueriesType.class);
        final var verificationOpt =
          emails.emailVerificationGet(token);

        if (verificationOpt.isEmpty()) {
          this.showError(
            request,
            servletResponse,
            this.strings.format("notFound"),
            false
          );
          return;
        }

        final var verification =
          verificationOpt.get();
        final var user =
          users.userGetRequire(verification.user());

        final var commandContext =
          new IdU1CommandContext(
            this.services,
            this.strings,
            requestIdFor(request),
            transaction,
            this.clock,
            user,
            request.getRemoteHost(),
            requestUserAgent(request)
          );

        switch (verification.operation()) {
          case EMAIL_ADD -> {
            final var command =
              new IdU1CommandEmailAddDeny(token.value());
            new IdU1CmdEmailAddDeny()
              .execute(commandContext, command);
          }
          case EMAIL_REMOVE -> {
            final var command =
              new IdU1CommandEmailRemoveDeny(token.value());
            new IdU1CmdEmailRemoveDeny()
              .execute(commandContext, command);
          }
        }

        transaction.commit();
        this.showSuccess(request, servletResponse);
      }
    } catch (final IdDatabaseException e) {
      this.showError(
        request,
        servletResponse,
        e.getMessage(),
        true
      );
    } catch (final IdCommandExecutionFailure e) {
      this.showError(
        request,
        servletResponse,
        e.getMessage(),
        e.httpStatusCode() >= 500
      );
    }
  }

  private void showSuccess(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse)
    throws IOException
  {
    try (var writer = servletResponse.getWriter()) {
      this.template.process(
        new IdFMMessageData(
          this.branding.htmlTitle(this.strings.format("emailVerificationSuccessTitle")),
          this.branding.title(),
          requestIdFor(request),
          false,
          false,
          this.strings.format("emailVerificationSuccessTitle"),
          this.strings.format("emailVerificationSuccess"),
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
    final HttpServletResponse servletResponse,
    final String message,
    final boolean isServerError)
    throws IOException
  {
    try (var writer = servletResponse.getWriter()) {
      if (isServerError) {
        servletResponse.setStatus(500);
      } else {
        servletResponse.setStatus(400);
      }

      this.template.process(
        new IdFMMessageData(
          this.branding.htmlTitle(this.strings.format("error")),
          this.branding.title(),
          requestIdFor(request),
          true,
          isServerError,
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


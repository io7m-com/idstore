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

package com.io7m.idstore.server.controller.admin;

import com.io7m.idstore.error_codes.IdException;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdShortHumanToken;
import com.io7m.idstore.protocol.admin.IdACommandMailTest;
import com.io7m.idstore.protocol.admin.IdAResponseMailTest;
import com.io7m.idstore.protocol.admin.IdAResponseType;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.security.IdSecAdminActionMailTest;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.mail.IdServerMailServiceType;
import com.io7m.idstore.server.service.templating.IdFMEmailTestData;
import com.io7m.idstore.server.service.templating.IdFMTemplateServiceType;
import io.opentelemetry.api.trace.Span;

import java.io.StringWriter;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.IO_ERROR;

/**
 * IdACommandMailTest
 */

public final class IdACmdMailTest
  extends IdACmdAbstract<
  IdACommandContext, IdACommandMailTest, IdAResponseType>
{
  /**
   * IdACommandMailTest
   */

  public IdACmdMailTest()
  {

  }

  @Override
  protected IdAResponseType executeActual(
    final IdACommandContext context,
    final IdACommandMailTest command)
    throws IdException
  {
    final var services =
      context.services();
    final var templates =
      services.requireService(IdFMTemplateServiceType.class);
    final var mail =
      services.requireService(IdServerMailServiceType.class);
    final var branding =
      services.requireService(IdServerBrandingServiceType.class);
    final var admin =
      context.admin();

    context.securityCheck(new IdSecAdminActionMailTest(admin));

    sendTestEmail(
      context,
      templates,
      mail,
      branding,
      command
    );

    return new IdAResponseMailTest(
      UUID.randomUUID(),
      command.messageId(),
      command.token()
    );
  }

  private static void sendTestEmail(
    final IdACommandContext context,
    final IdFMTemplateServiceType templateService,
    final IdServerMailServiceType mailService,
    final IdServerBrandingServiceType brandingService,
    final IdACommandMailTest command)
    throws IdCommandExecutionFailure
  {
    final var token =
      command.token();
    final var email =
      command.address();
    final var template =
      templateService.emailTestTemplate();

    final var writer = new StringWriter();
    try {
      template.process(
        new IdFMEmailTestData(brandingService.title(), token),
        writer
      );
    } catch (final Exception e) {
      throw new IdCommandExecutionFailure(
        e.getMessage(),
        e,
        IO_ERROR,
        Map.of(),
        Optional.empty(),
        command.messageId(),
        500
      );
    }

    final var mailHeaders =
      Map.ofEntries(
        Map.entry(
          "X-IDStore-Test-Token",
          token.value())
      );

    try {
      mailService.sendMail(
        Span.current(),
        command.messageId(),
        email,
        mailHeaders,
        brandingService.emailSubject("Email test"),
        writer.toString()
      ).get();
    } catch (final Exception e) {
      throw context.failMail(command, email, e);
    }
  }
}

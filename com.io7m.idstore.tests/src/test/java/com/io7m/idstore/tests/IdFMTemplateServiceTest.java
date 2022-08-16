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


package com.io7m.idstore.tests;

import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdEmailVerification;
import com.io7m.idstore.model.IdEmailVerificationOperation;
import com.io7m.idstore.model.IdToken;
import com.io7m.idstore.server.internal.freemarker.IdFMEmailVerificationData;
import com.io7m.idstore.server.internal.freemarker.IdFMLoginData;
import com.io7m.idstore.server.internal.freemarker.IdFMTemplateService;
import freemarker.template.TemplateException;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class IdFMTemplateServiceTest
{
  @Test
  public void testGetLogin()
    throws IOException, TemplateException
  {
    final var service =
      IdFMTemplateService.create();

    final var template =
      service.pageLoginTemplate();

    final var writer =
      new BufferedWriter(new OutputStreamWriter(System.out, UTF_8));

    template.process(new IdFMLoginData(
      "idstore: Login",
      "Login",
      Optional.of("Error!")
    ), writer);

    writer.flush();
  }

  @Test
  public void testEmailVerification()
    throws IOException, TemplateException
  {
    final var service =
      IdFMTemplateService.create();

    final var template =
      service.emailVerificationTemplate();

    final var writer =
      new BufferedWriter(new OutputStreamWriter(System.out, UTF_8));

    final var token =
      IdToken.generate();

    final var verification =
      new IdEmailVerification(
        UUID.randomUUID(),
        new IdEmail("someone@example.com"),
        token,
        IdEmailVerificationOperation.EMAIL_ADD,
        OffsetDateTime.now().plusDays(1L)
      );

    template.process(
      new IdFMEmailVerificationData(
        verification,
        "[2610:1c1:1:606c::50:15]",
        "Mozilla/5.0 (Windows NT 10.0; rv:91.0) Gecko/20100101 Firefox/91.0",
        URI.create("https://id.example.com/allow?token=%s".formatted(token)),
        URI.create("https://id.example.com/deny?token=%s".formatted(token))
      ),
      writer
    );

    writer.flush();
  }
}

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


package com.io7m.idstore.tests.server.service.templating;

import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdEmailVerification;
import com.io7m.idstore.model.IdEmailVerificationOperation;
import com.io7m.idstore.model.IdLogin;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdNonEmptyList;
import com.io7m.idstore.model.IdPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdToken;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.server.api.IdServerColorScheme;
import com.io7m.idstore.server.service.templating.IdFMCSSData;
import com.io7m.idstore.server.service.templating.IdFMEmailVerificationData;
import com.io7m.idstore.server.service.templating.IdFMLoginData;
import com.io7m.idstore.server.service.templating.IdFMPasswordResetData;
import com.io7m.idstore.server.service.templating.IdFMTemplateService;
import com.io7m.idstore.server.service.templating.IdFMUserSelfData;
import com.io7m.idstore.tests.IdTestDirectories;
import freemarker.template.TemplateException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class IdFMTemplateServiceTest
{
  private Path directory;

  @BeforeEach
  public void setup()
    throws IOException
  {
    this.directory = IdTestDirectories.createTempDirectory();
  }

  @AfterEach
  public void tearDown()
    throws IOException
  {
    IdTestDirectories.deleteDirectory(this.directory);
  }

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
      "idstore",
      true,
      Optional.empty(),
      Optional.of("Error!"),
      Optional.empty()
    ), writer);

    writer.flush();
  }

  @Test
  public void testGetLoginExtraBranding()
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
      "idstore",
      true,
      Optional.empty(),
      Optional.of("Error!"),
      Optional.of(
        Files.readString(
          IdTestDirectories.resourceOf(
            IdFMTemplateServiceTest.class,
            this.directory,
            "loginExtra.xhtml"))
      )
    ), writer);

    writer.flush();
  }

  @Test
  public void testEmailVerification0()
    throws IOException, TemplateException
  {
    final var service =
      IdFMTemplateService.create();

    final var template =
      service.emailVerificationTemplate();

    final var writer =
      new BufferedWriter(new OutputStreamWriter(System.out, UTF_8));

    final var tokenPermit =
      IdToken.generate();
    final var tokenDeny =
      IdToken.generate();

    final var verification =
      new IdEmailVerification(
        UUID.randomUUID(),
        new IdEmail("someone@example.com"),
        tokenPermit,
        tokenDeny,
        IdEmailVerificationOperation.EMAIL_ADD,
        OffsetDateTime.now().plusDays(1L)
      );

    template.process(
      new IdFMEmailVerificationData(
        "idstore",
        verification,
        "[2610:1c1:1:606c::50:15]",
        "Mozilla/5.0 (Windows NT 10.0; rv:91.0) Gecko/20100101 Firefox/91.0",
        Optional.of(
          URI.create("https://id.example.com/allow?token=%s"
                       .formatted(tokenPermit))
        ),
        URI.create("https://id.example.com/deny?token=%s".formatted(tokenDeny))
      ),
      writer
    );

    writer.flush();
  }

  @Test
  public void testEmailVerification1()
    throws IOException, TemplateException
  {
    final var service =
      IdFMTemplateService.create();

    final var template =
      service.emailVerificationTemplate();

    final var writer =
      new BufferedWriter(new OutputStreamWriter(System.out, UTF_8));

    final var tokenPermit =
      IdToken.generate();
    final var tokenDeny =
      IdToken.generate();

    final var verification =
      new IdEmailVerification(
        UUID.randomUUID(),
        new IdEmail("someone@example.com"),
        tokenPermit,
        tokenDeny,
        IdEmailVerificationOperation.EMAIL_ADD,
        OffsetDateTime.now().plusDays(1L)
      );

    template.process(
      new IdFMEmailVerificationData(
        "idstore",
        verification,
        "[2610:1c1:1:606c::50:15]",
        "Mozilla/5.0 (Windows NT 10.0; rv:91.0) Gecko/20100101 Firefox/91.0",
        Optional.empty(),
        URI.create("https://id.example.com/deny?token=%s".formatted(tokenDeny))
      ),
      writer
    );

    writer.flush();
  }

  @Test
  public void testUser()
    throws IOException, TemplateException, IdPasswordException
  {
    final var service =
      IdFMTemplateService.create();

    final var template =
      service.pageUserSelfTemplate();

    final var writer =
      new BufferedWriter(new OutputStreamWriter(System.out, UTF_8));

    final var id = UUID.randomUUID();

    final var user = new IdUser(
      id,
      new IdName("someone"),
      new IdRealName("Someone X. Incognito"),
      new IdNonEmptyList<>(
        new IdEmail("someone@example.com"),
        List.of(
          new IdEmail("someone2@example.com"),
          new IdEmail("someone3@example.com"),
          new IdEmail("someone4@example.com")
        )
      ),
      OffsetDateTime.now(),
      OffsetDateTime.now(),
      IdPasswordAlgorithmPBKDF2HmacSHA256.create()
        .createHashed("12345678")
    );

    final var loginHistory =
      List.of(
      new IdLogin(
        id,
        OffsetDateTime.now(),
        "localhost",
        "Test"
      ),
      new IdLogin(
        id,
        OffsetDateTime.now(),
        "localhost",
        "Test"
      ), new IdLogin(
        id,
        OffsetDateTime.now(),
        "localhost",
        "Test"
      )
    );

    final var userData =
      new IdFMUserSelfData(
      "idstore: User",
      "idstore",
      user,
      loginHistory
    );

    template.process(userData, writer);
    writer.flush();

    dump("user.xhtml", w -> {
      try {
        template.process(userData, w);
      } catch (final Exception e) {
        // Don't care
      }
    });
  }

  private static void dump(
    final String name,
    final Consumer<Writer> consumer)
  {
    try {
      final var path = Paths.get("/shared-tmp/" + name);
      try (var writer = Files.newBufferedWriter(path)) {
        consumer.accept(writer);
      }
    } catch (final Exception e) {
      // Don't care
    }
  }

  @Test
  public void testCSS()
    throws IOException, TemplateException
  {
    final var service =
      IdFMTemplateService.create();

    final var template =
      service.cssTemplate();

    final var writer =
      new BufferedWriter(new OutputStreamWriter(System.out, UTF_8));

    template.process(
      new IdFMCSSData(IdServerColorScheme.defaults()),
      writer
    );

    writer.flush();
  }

  @Test
  public void testPasswordReset()
    throws IOException, TemplateException
  {
    final var service =
      IdFMTemplateService.create();

    final var template =
      service.pagePasswordResetTemplate();

    final var writer =
      new BufferedWriter(new OutputStreamWriter(System.out, UTF_8));

    template.process(
      new IdFMPasswordResetData(
        "idstore",
        "Header"
      ),
      writer
    );

    writer.flush();
  }
}

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

package com.io7m.idstore.tests.server.service.branding;

import com.io7m.idstore.server.api.IdServerBrandingConfiguration;
import com.io7m.idstore.server.service.branding.IdServerBrandingService;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.templating.IdFMTemplateService;
import com.io7m.idstore.server.service.templating.IdFMTemplateServiceType;
import com.io7m.idstore.server.service.templating.IdFMTemplateType;
import com.io7m.idstore.tests.IdTestDirectories;
import com.io7m.idstore.tests.server.service.IdServiceContract;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static com.helger.css.ECSSVersion.CSS21;
import static com.helger.css.reader.CSSReader.isValidCSS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class IdServerBrandingTest
  extends IdServiceContract<IdServerBrandingServiceType>
{
  private Path directory;

  @BeforeEach
  public void setup()
    throws IOException
  {
    this.directory =
      IdTestDirectories.createTempDirectory();
  }

  @AfterEach
  public void tearDown()
    throws IOException
  {
    IdTestDirectories.deleteDirectory(this.directory);
  }

  /**
   * Basic branding information works.
   *
   * @throws IOException On errors
   */

  @Test
  public void testBasic()
    throws IOException
  {
    final var templatesReal =
      IdFMTemplateService.create();

    final var branding =
      IdServerBrandingService.create(
        templatesReal,
        new IdServerBrandingConfiguration(
          "idstore",
          Optional.empty(),
          Optional.empty(),
          Optional.empty()
        )
      );

    assertEquals("idstore", branding.title());
    assertEquals("idstore: Main", branding.htmlTitle("Main"));
    assertEquals(Optional.empty(), branding.loginExtraText());
    assertEquals("[idstore] Main", branding.emailSubject("Main"));
    assertTrue(isValidCSS(branding.css(), CSS21));
    assertTrue(isValidCSS(branding.xButtonCSS(), CSS21));
  }

  /**
   * Extra login text can be read.
   *
   * @throws IOException On errors
   */

  @Test
  public void testLoginExtra()
    throws IOException
  {
    final var templatesReal =
      IdFMTemplateService.create();

    final var file = this.directory.resolve("file.txt");
    Files.writeString(file, "Extra text.");

    final var branding =
      IdServerBrandingService.create(
        templatesReal,
        new IdServerBrandingConfiguration(
          "idstore",
          Optional.empty(),
          Optional.of(file),
          Optional.empty()
        )
      );

    assertEquals("idstore", branding.title());
    assertEquals("idstore: Main", branding.htmlTitle("Main"));
    assertEquals(Optional.of("Extra text."), branding.loginExtraText());
    assertEquals("[idstore] Main", branding.emailSubject("Main"));
    assertTrue(isValidCSS(branding.css(), CSS21));
    assertTrue(isValidCSS(branding.xButtonCSS(), CSS21));
  }

  /**
   * External logos are loaded.
   *
   * @throws IOException On errors
   */

  @Test
  public void testLogoExternal()
    throws IOException
  {
    final var templatesReal =
      IdFMTemplateService.create();

    final var file = this.directory.resolve("image.png");
    Files.writeString(file, "PNG");

    final var branding =
      IdServerBrandingService.create(
        templatesReal,
        new IdServerBrandingConfiguration(
          "idstore",
          Optional.of(file),
          Optional.empty(),
          Optional.empty()
        )
      );

    assertEquals("idstore", branding.title());
    assertEquals("idstore: Main", branding.htmlTitle("Main"));
    assertEquals(Optional.empty(), branding.loginExtraText());
    assertEquals("[idstore] Main", branding.emailSubject("Main"));
    assertArrayEquals("PNG".getBytes(UTF_8), branding.logoImage());
    assertTrue(isValidCSS(branding.css(), CSS21));
    assertTrue(isValidCSS(branding.xButtonCSS(), CSS21));
  }

  @Override
  protected IdServerBrandingServiceType createInstanceA()
  {
    try {
      final var templatesMock =
        Mockito.mock(IdFMTemplateServiceType.class);
      final var cssTemplate =
        Mockito.mock(IdFMTemplateType.class);
      Mockito.when(templatesMock.cssTemplate())
        .thenReturn(cssTemplate);

      return IdServerBrandingService.create(
        templatesMock,
        new IdServerBrandingConfiguration(
          "idstore",
          Optional.empty(),
          Optional.empty(),
          Optional.empty()
        )
      );
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  protected IdServerBrandingServiceType createInstanceB()
  {
    try {
      final var templatesMock =
        Mockito.mock(IdFMTemplateServiceType.class);
      final var cssTemplate =
        Mockito.mock(IdFMTemplateType.class);
      Mockito.when(templatesMock.cssTemplate())
        .thenReturn(cssTemplate);

      return IdServerBrandingService.create(
        templatesMock,
        new IdServerBrandingConfiguration(
          "other",
          Optional.empty(),
          Optional.empty(),
          Optional.empty()
        )
      );
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}

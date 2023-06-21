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


package com.io7m.idstore.server.service.templating;

import freemarker.core.XMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Objects;

import static freemarker.template.Configuration.SQUARE_BRACKET_TAG_SYNTAX;

/**
 * A service supplying freemarker templates.
 */

public final class IdFMTemplateService implements IdFMTemplateServiceType
{
  private final Configuration configuration;

  private IdFMTemplateService(
    final Configuration inConfiguration)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
  }

  /**
   * @return A service supplying freemarker templates.
   */

  public static IdFMTemplateServiceType create()
  {
    final Configuration configuration =
      new Configuration(Configuration.VERSION_2_3_31);

    configuration.setTagSyntax(SQUARE_BRACKET_TAG_SYNTAX);
    configuration.setTemplateLoader(new IdFMTemplateLoader());
    return new IdFMTemplateService(configuration);
  }

  @Override
  public IdFMTemplateType<IdFMLoginData> pageLoginTemplate()
  {
    return new IdGenericTemplate<>(
      this.findTemplate("pageLogin")
    );
  }

  @Override
  public IdFMTemplateType<IdFMUserSelfData> pageUserSelfTemplate()
  {
    return new IdGenericTemplate<>(
      this.findTemplate("pageUserSelf")
    );
  }

  @Override
  public IdFMTemplateType<IdFMEmailVerificationData> emailVerificationTemplate()
  {
    return new IdGenericTemplate<>(
      this.findTemplate("emailVerification")
    );
  }

  @Override
  public IdFMTemplateType<IdFMEmailPasswordResetData> emailPasswordResetTemplate()
  {
    return new IdGenericTemplate<>(
      this.findTemplate("emailPasswordReset")
    );
  }

  private Template findTemplate(
    final String name)
  {
    Objects.requireNonNull(name, "name");
    try {
      return this.configuration.getTemplate(name);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public String description()
  {
    return "Freemarker template service.";
  }

  @Override
  public String toString()
  {
    return "[IdFMTemplateService 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode(), 16));
  }

  @Override
  public IdFMTemplateType<IdFMEmailAddData> pageEmailAddTemplate()
  {
    return new IdGenericTemplate<>(
      this.findTemplate("pageEmailAdd")
    );
  }

  @Override
  public IdFMTemplateType<IdFMMessageData> pageMessage()
  {
    return new IdGenericTemplate<>(
      this.findTemplate("pageMessage")
    );
  }

  @Override
  public IdFMTemplateType<IdFMCSSData> cssTemplate()
  {
    return new IdGenericTemplate<>(
      this.findTemplate("mainCss")
    );
  }

  @Override
  public IdFMTemplateType<IdFMRealNameUpdateData> pageRealnameUpdateTemplate()
  {
    return new IdGenericTemplate<>(
      this.findTemplate("pageRealNameUpdate")
    );
  }

  @Override
  public IdFMTemplateType<IdFMPasswordUpdateData> pagePasswordUpdateTemplate()
  {
    return new IdGenericTemplate<>(
      this.findTemplate("pagePasswordUpdate")
    );
  }

  @Override
  public IdFMTemplateType<IdFMPasswordResetData> pagePasswordResetTemplate()
  {
    return new IdGenericTemplate<>(
      this.findTemplate("pagePasswordReset")
    );
  }

  @Override
  public IdFMTemplateType<IdFMPasswordResetConfirmData> pagePasswordResetConfirmTemplate()
  {
    return new IdGenericTemplate<>(
      this.findTemplate("pagePasswordResetConfirm")
    );
  }

  @Override
  public IdFMTemplateType<IdFMEmailTestData> emailTestTemplate()
  {
    return new IdGenericTemplate<>(
      this.findTemplate("emailTest")
    );
  }

  private static final class IdGenericTemplate<T extends IdFMDataModelType>
    implements IdFMTemplateType<T>
  {
    private final Template template;

    IdGenericTemplate(
      final Template inTemplate)
    {
      this.template = Objects.requireNonNull(inTemplate, "template");
    }

    @Override
    public void process(
      final T value,
      final Writer output)
      throws TemplateException, IOException
    {
      this.template.process(value.toTemplateHash(), output);
    }
  }
}

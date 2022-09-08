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


package com.io7m.idstore.server.internal.freemarker;

import com.io7m.idstore.services.api.IdServiceType;
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

public final class IdFMTemplateService implements IdServiceType
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

  public static IdFMTemplateService create()
  {
    final Configuration configuration =
      new Configuration(Configuration.VERSION_2_3_31);

    configuration.setTagSyntax(SQUARE_BRACKET_TAG_SYNTAX);
    configuration.setTemplateLoader(new IdFMTemplateLoader());
    return new IdFMTemplateService(configuration);
  }

  /**
   * @return The login form template
   */

  public IdFMTemplateType<IdFMLoginData> pageLoginTemplate()
  {
    return new IdGenericTemplate<>(
      this.findTemplate("pageLogin")
    );
  }

  /**
   * @return The user profile "self" template
   */

  public IdFMTemplateType<IdFMUserSelfData> pageUserSelfTemplate()
  {
    return new IdGenericTemplate<>(
      this.findTemplate("pageUserSelf")
    );
  }

  /**
   * @return The email verification template
   */

  public IdFMTemplateType<IdFMEmailVerificationData> emailVerificationTemplate()
  {
    return new IdGenericTemplate<>(
      this.findTemplate("emailVerification")
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
      .formatted(Long.toUnsignedString(this.hashCode()));
  }

  /**
   * @return The email addition page template
   */

  public IdFMTemplateType<IdFMEmailAddData> pageEmailAddTemplate()
  {
    return new IdGenericTemplate<>(
      this.findTemplate("pageEmailAdd")
    );
  }

  /**
   * @return The generic message page template
   */

  public IdFMTemplateType<IdFMMessageData> pageMessage()
  {
    return new IdGenericTemplate<>(
      this.findTemplate("pageMessage")
    );
  }

  /**
   * @return The main CSS template
   */

  public IdFMTemplateType<IdFMCSSData> cssTemplate()
  {
    return new IdGenericTemplate<>(
      this.findTemplate("mainCss")
    );
  }

  /**
   * @return The realname update page template
   */

  public IdFMTemplateType<IdFMRealNameUpdateData> pageRealnameUpdateTemplate()
  {
    return new IdGenericTemplate<>(
      this.findTemplate("pageRealNameUpdate")
    );
  }

  /**
   * @return The admin main template
   */

  public IdFMTemplateType<IdFMAdminMainData> pageAdminMainTemplate()
  {
    return new IdGenericTemplate<>(
      this.findTemplate("pageAdminMain")
    );
  }

  /**
   * @return The admin user list template
   */

  public IdFMTemplateType<IdFMAdminUsersData> pageAdminUsersTemplate()
  {
    return new IdGenericTemplate<>(
      this.findTemplate("pageAdminUsers")
    );
  }

  /**
   * @return The admin user page template
   */

  public IdFMTemplateType<IdFMAdminUserData> pageAdminUserTemplate()
  {
    return new IdGenericTemplate<>(
      this.findTemplate("pageAdminUser")
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

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


package com.io7m.idstore.shell.admin.internal;

import com.io7m.idstore.model.IdEmail;
import com.io7m.quarrel.core.QException;
import com.io7m.quarrel.core.QValueConverterType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A value converter for emails.
 */

public final class IdAEmailConverter implements QValueConverterType<IdEmail>
{
  /**
   * A value converter for emails.
   */

  public IdAEmailConverter()
  {

  }

  @Override
  public IdEmail convertFromString(
    final String text)
    throws QException
  {
    try {
      return new IdEmail(text);
    } catch (final Exception e) {
      throw new QException(
        e.getMessage(),
        e,
        "email-invalid",
        Map.of(),
        Optional.empty(),
        List.of()
      );
    }
  }

  @Override
  public String convertToString(
    final IdEmail value)
  {
    return value.value();
  }

  @Override
  public IdEmail exampleValue()
  {
    return new IdEmail("someone@example.com");
  }

  @Override
  public String syntax()
  {
    return "<RFC 2822 Email Address>";
  }

  @Override
  public Class<IdEmail> convertedClass()
  {
    return IdEmail.class;
  }
}

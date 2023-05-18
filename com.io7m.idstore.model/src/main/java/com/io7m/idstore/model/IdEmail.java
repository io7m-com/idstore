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

package com.io7m.idstore.model;

import com.sanctionco.jmail.EmailValidator;
import com.sanctionco.jmail.JMail;

import java.util.Locale;
import java.util.Objects;

/**
 * A user email.
 *
 * @param value The email value
 */

public record IdEmail(String value)
{
  private static final EmailValidator VALIDATOR =
    JMail.strictValidator()
      .disallowObsoleteWhitespace()
      .disallowQuotedIdentifiers();

  /**
   * Check that a string is a valid email address.
   *
   * @param text The text
   *
   * @return The string
   */

  public static String check(
    final String text)
  {
    if (!VALIDATOR.isValid(text)) {
      throw new IdValidityException("Invalid email address.");
    }
    return text;
  }

  /**
   * A user email.
   *
   * @param value The email value
   */

  public IdEmail(final String value)
  {
    Objects.requireNonNull(value, "value");
    this.value = check(value).toLowerCase(Locale.getDefault());
  }

  @Override
  public String toString()
  {
    return this.value;
  }
}

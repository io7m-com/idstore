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

package com.io7m.idstore.admin_gui.internal.audit;

import javafx.util.StringConverter;

import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * A date string converter.
 */

public final class IdAGAuditDateStringConverter
  extends StringConverter<LocalDate>
{
  private static final Pattern DATE_PATTERN =
    Pattern.compile("([0-9]{4,5})-([0-9]{1,2})-([0-9]{1,2})");

  /**
   * A date string converter.
   */

  public IdAGAuditDateStringConverter()
  {

  }

  @Override
  public String toString(
    final LocalDate t)
  {
    if (t == null) {
      return "";
    }

    return String.format(
      "%04d-%02d-%02d",
      Integer.valueOf(t.getYear()),
      Integer.valueOf(t.getMonthValue()),
      Integer.valueOf(t.getDayOfMonth())
    );
  }

  @Override
  public LocalDate fromString(
    final String s)
  {
    final var matcher = DATE_PATTERN.matcher(s);
    if (matcher.matches()) {
      return LocalDate.of(
        Integer.valueOf(matcher.group(1)).intValue(),
        Integer.valueOf(matcher.group(2)).intValue(),
        Integer.valueOf(matcher.group(3)).intValue()
      );
    }

    return LocalDate.now();
  }
}

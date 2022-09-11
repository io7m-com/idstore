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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.regex.Pattern;

/**
 * A time-only date string converter.
 */

public final class IdAGAuditTimeOnlyStringConverter
  extends StringConverter<OffsetDateTime>
{
  private static final Pattern TIME_PATTERN =
    Pattern.compile("([0-9]{1,2}):([0-9]{1,2}):([0-9]{1,2})");

  /**
   * A time-only date string converter.
   */

  public IdAGAuditTimeOnlyStringConverter()
  {

  }

  @Override
  public String toString(
    final OffsetDateTime t)
  {
    if (t == null) {
      return "";
    }

    return String.format(
      "%02d:%02d:%02d",
      Integer.valueOf(t.getHour()),
      Integer.valueOf(t.getMinute()),
      Integer.valueOf(t.getSecond())
    );
  }

  @Override
  public OffsetDateTime fromString(
    final String s)
  {
    final var matcher = TIME_PATTERN.matcher(s);
    if (matcher.matches()) {
      return OffsetDateTime.of(
        2000,
        1,
        1,
        Integer.parseUnsignedInt(matcher.group(1)),
        Integer.parseUnsignedInt(matcher.group(2)),
        Integer.parseUnsignedInt(matcher.group(3)),
        0,
        ZoneOffset.UTC
      );
    }
    return OffsetDateTime.now();
  }
}

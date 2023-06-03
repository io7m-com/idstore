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

import com.io7m.idstore.protocol.admin.IdAPasswordExpirationSetNever;
import com.io7m.idstore.protocol.admin.IdAPasswordExpirationSetRefresh;
import com.io7m.idstore.protocol.admin.IdAPasswordExpirationSetSpecific;
import com.io7m.idstore.protocol.admin.IdAPasswordExpirationSetType;
import com.io7m.quarrel.core.QValueConverterType;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * A value converter for password expiration sets.
 */

public final class IdAPasswordExpirationSetConverter
  implements QValueConverterType<IdAPasswordExpirationSetType>
{
  /**
   * A value converter for password expiration sets.
   */

  public IdAPasswordExpirationSetConverter()
  {

  }

  @Override
  public IdAPasswordExpirationSetType convertFromString(
    final String text)
  {
    if (Objects.equals(text, "never")) {
      return new IdAPasswordExpirationSetNever();
    }
    if (Objects.equals(text, "default")) {
      return new IdAPasswordExpirationSetRefresh();
    }
    return new IdAPasswordExpirationSetSpecific(OffsetDateTime.parse(text));
  }

  @Override
  public String convertToString(
    final IdAPasswordExpirationSetType value)
  {
    if (value instanceof IdAPasswordExpirationSetNever) {
      return "never";
    }
    if (value instanceof IdAPasswordExpirationSetRefresh) {
      return "default";
    }
    if (value instanceof IdAPasswordExpirationSetSpecific s) {
      return s.time().toString();
    }

    throw new IllegalStateException();
  }

  @Override
  public IdAPasswordExpirationSetType exampleValue()
  {
    return new IdAPasswordExpirationSetNever();
  }

  @Override
  public String syntax()
  {
    return "never | default | <offset-date-time>";
  }

  @Override
  public Class<IdAPasswordExpirationSetType> convertedClass()
  {
    return IdAPasswordExpirationSetType.class;
  }
}

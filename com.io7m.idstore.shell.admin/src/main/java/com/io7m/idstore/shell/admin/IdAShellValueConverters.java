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


package com.io7m.idstore.shell.admin;

import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdShortHumanToken;
import com.io7m.idstore.protocol.admin.IdAPasswordExpirationSetType;
import com.io7m.idstore.shell.admin.internal.IdAEmailConverter;
import com.io7m.idstore.shell.admin.internal.IdAPasswordExpirationSetConverter;
import com.io7m.idstore.shell.admin.internal.IdAShortHumanTokenConverter;
import com.io7m.quarrel.core.QValueConverterDirectory;
import com.io7m.quarrel.core.QValueConverterDirectoryType;

/**
 * Value converters for the shell commands.
 */

public final class IdAShellValueConverters
{
  private static final QValueConverterDirectoryType CONVERTERS =
    QValueConverterDirectory.core()
      .with(
        IdShortHumanToken.class,
        new IdAShortHumanTokenConverter())
      .with(
        IdEmail.class,
        new IdAEmailConverter())
      .with(
        IdAPasswordExpirationSetType.class,
        new IdAPasswordExpirationSetConverter()
      );

  private IdAShellValueConverters()
  {

  }

  /**
   * @return The value converters
   */

  public static QValueConverterDirectoryType get()
  {
    return CONVERTERS;
  }
}

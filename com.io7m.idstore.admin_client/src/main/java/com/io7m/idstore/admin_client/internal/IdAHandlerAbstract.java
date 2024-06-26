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

package com.io7m.idstore.admin_client.internal;

import com.io7m.idstore.admin_client.api.IdAClientConfiguration;
import com.io7m.idstore.admin_client.api.IdAClientException;
import com.io7m.idstore.strings.IdStrings;

import java.util.Objects;
import java.util.function.Function;

/**
 * The abstract base class for handlers.
 */

abstract class IdAHandlerAbstract
  implements IdAHandlerType
{
  private final IdStrings strings;
  private final IdAClientConfiguration configuration;

  protected IdAHandlerAbstract(
    final IdAClientConfiguration inConfiguration,
    final IdStrings inStrings)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
  }

  @Override
  public final Function<Throwable, IdAClientException> exceptionTransformer()
  {
    return IdAClientException::ofException;
  }

  protected final IdStrings strings()
  {
    return this.strings;
  }

  protected final IdAClientConfiguration configuration()
  {
    return this.configuration;
  }
}

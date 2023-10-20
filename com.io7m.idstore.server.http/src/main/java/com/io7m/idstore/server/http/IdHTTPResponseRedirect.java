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

package com.io7m.idstore.server.http;

import io.helidon.http.Status;

import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;

/**
 * A redirect response.
 *
 * @param cookies The cookies
 * @param path    The path
 */

public record IdHTTPResponseRedirect(
  Set<IdHTTPCookieDeclaration> cookies,
  String path)
  implements IdHTTPResponseType
{
  /**
   * A redirect response.
   *
   * @param cookies The cookies
   * @param path    The path
   */

  public IdHTTPResponseRedirect
  {
    Objects.requireNonNull(cookies, "cookies");
    Objects.requireNonNull(path, "path");
  }

  @Override
  public int statusCode()
  {
    return Status.MOVED_PERMANENTLY_301.code();
  }

  @Override
  public OptionalLong contentLengthOptional()
  {
    return OptionalLong.empty();
  }
}

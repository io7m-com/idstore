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

package com.io7m.idstore.protocol.admin;

import com.io7m.idstore.model.IdName;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * A request to log in.
 *
 * @param messageId The message ID
 * @param userName  The username
 * @param password  The password
 * @param metadata  Extra metadata included with the request
 */

public record IdACommandLogin(
  UUID messageId,
  IdName userName,
  String password,
  Map<String, String> metadata)
  implements IdACommandType<IdAResponseLogin>
{
  /**
   * A request to log in.
   *
   * @param userName The username
   * @param password The password
   * @param metadata Extra metadata included with the request
   */

  public IdACommandLogin
  {
    Objects.requireNonNull(messageId, "messageId");
    Objects.requireNonNull(userName, "userName");
    Objects.requireNonNull(password, "password");
    Objects.requireNonNull(metadata, "metadata");
  }

  @Override
  public Class<IdAResponseLogin> responseClass()
  {
    return IdAResponseLogin.class;
  }
}

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

import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdShortHumanToken;

import java.util.Objects;
import java.util.UUID;

/**
 * A request to send a test email to an address.
 *
 * @param messageId The message ID
 * @param address   The target address
 * @param token     The token to be placed in the email
 */

public record IdACommandMailTest(
  UUID messageId,
  IdEmail address,
  IdShortHumanToken token)
  implements IdACommandType<IdAResponseMailTest>
{
  /**
   * A request to send a test email to an address.
   *
   * @param address The target address
   * @param token   The token to be placed in the email
   */

  public IdACommandMailTest
  {
    Objects.requireNonNull(messageId, "messageId");
    Objects.requireNonNull(address, "address");
    Objects.requireNonNull(token, "token");
  }

  @Override
  public Class<IdAResponseMailTest> responseClass()
  {
    return IdAResponseMailTest.class;
  }
}

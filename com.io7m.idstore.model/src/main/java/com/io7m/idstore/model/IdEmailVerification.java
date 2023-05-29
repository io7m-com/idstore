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

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * An email verification operation.
 *
 * @param email       The email address
 * @param operation   The operation
 * @param tokenPermit The verification token to permit the operation
 * @param tokenDeny   The verification token to deny the operation
 * @param expires     The expiration date
 * @param user        The user
 */

public record IdEmailVerification(
  UUID user,
  IdEmail email,
  IdToken tokenPermit,
  IdToken tokenDeny,
  IdEmailVerificationOperation operation,
  OffsetDateTime expires)
{
  /**
   * An email verification operation.
   *
   * @param email       The email address
   * @param operation   The operation
   * @param tokenPermit The verification token to permit the operation
   * @param tokenDeny   The verification token to deny the operation
   * @param expires     The expiration date
   * @param user        The user
   */

  public IdEmailVerification
  {
    Objects.requireNonNull(user, "user");
    Objects.requireNonNull(email, "email");
    Objects.requireNonNull(tokenPermit, "tokenPermit");
    Objects.requireNonNull(tokenDeny, "tokenDeny");
    Objects.requireNonNull(operation, "operation");
    Objects.requireNonNull(expires, "expires");
  }
}

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

package com.io7m.idstore.server.security;

import java.util.Objects;

/**
 * A security policy that denies all actions.
 */

public final class IdSecPolicyDenyAll implements IdSecPolicyType
{
  private static final IdSecPolicyDenyAll INSTANCE =
    new IdSecPolicyDenyAll();

  private IdSecPolicyDenyAll()
  {

  }

  /**
   * @return A reference to this policy
   */

  public static IdSecPolicyType get()
  {
    return INSTANCE;
  }

  @Override
  public IdSecPolicyResultType check(
    final IdSecActionType action)
  {
    Objects.requireNonNull(action, "action");
    return new IdSecPolicyResultDenied("All actions are denied.");
  }
}

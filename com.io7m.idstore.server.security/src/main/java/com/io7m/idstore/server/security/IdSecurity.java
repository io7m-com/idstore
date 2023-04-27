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
 * The main API for performing security policy checks.
 */

public final class IdSecurity
{
  private static volatile IdSecPolicyType POLICY =
    IdSecPolicyDefault.get();

  private IdSecurity()
  {

  }

  /**
   * Set the currently loaded policy.
   *
   * @param policy The policy
   */

  public static void setPolicy(
    final IdSecPolicyType policy)
  {
    POLICY = Objects.requireNonNull(policy, "policy");
  }

  /**
   * Check that a user is allowed to perform an action by the current policy.
   *
   * @param action The action
   *
   * @return A value indicating if the action is permitted
   *
   * @throws IdSecurityException On evaluation errors
   */

  public static IdSecPolicyResultType check(
    final IdSecActionType action)
    throws IdSecurityException
  {
    Objects.requireNonNull(action, "action");
    return POLICY.check(action);
  }
}

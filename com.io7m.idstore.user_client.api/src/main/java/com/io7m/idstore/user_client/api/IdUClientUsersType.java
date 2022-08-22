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


package com.io7m.idstore.user_client.api;

import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdToken;
import com.io7m.idstore.model.IdUser;

/**
 * Commands related to users.
 */

public interface IdUClientUsersType
{
  /**
   * Retrieve the current user profile.
   *
   * @return The current user
   *
   * @throws IdUClientException   On errors
   * @throws InterruptedException On interruption
   */

  IdUser userSelf()
    throws IdUClientException, InterruptedException;

  /**
   * Start adding the given email address to the current user.
   *
   * @param email The email address
   *
   * @throws IdUClientException   On errors
   * @throws InterruptedException On interruption
   */

  void userEmailAddBegin(IdEmail email)
    throws IdUClientException, InterruptedException;

  /**
   * Complete a challenge that adds an email address to the given user.
   *
   * @param token The challenge token
   *
   * @throws IdUClientException   On errors
   * @throws InterruptedException On interruption
   */

  void userEmailAddPermit(IdToken token)
    throws IdUClientException, InterruptedException;

  /**
   * Reject a challenge that adds an email address to the given user.
   *
   * @param token The challenge token
   *
   * @throws IdUClientException   On errors
   * @throws InterruptedException On interruption
   */

  void userEmailAddDeny(IdToken token)
    throws IdUClientException, InterruptedException;

  /**
   * Start removing the given email address to the current user.
   *
   * @param email The email address
   *
   * @throws IdUClientException   On errors
   * @throws InterruptedException On interruption
   */

  void userEmailRemoveBegin(IdEmail email)
    throws IdUClientException, InterruptedException;

  /**
   * Complete a challenge that removes an email address to the given user.
   *
   * @param token The challenge token
   *
   * @throws IdUClientException   On errors
   * @throws InterruptedException On interruption
   */

  void userEmailRemovePermit(IdToken token)
    throws IdUClientException, InterruptedException;

  /**
   * Reject a challenge that removes an email address to the given user.
   *
   * @param token The challenge token
   *
   * @throws IdUClientException   On errors
   * @throws InterruptedException On interruption
   */

  void userEmailRemoveDeny(IdToken token)
    throws IdUClientException, InterruptedException;

  /**
   * Update the user's realname.
   *
   * @param realName The new realname
   *
   * @throws IdUClientException   On errors
   * @throws InterruptedException On interruption
   */

  void userRealNameUpdate(IdRealName realName)
    throws IdUClientException, InterruptedException;
}

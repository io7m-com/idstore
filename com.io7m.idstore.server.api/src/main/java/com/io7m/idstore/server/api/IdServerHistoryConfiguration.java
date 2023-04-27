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

package com.io7m.idstore.server.api;

/**
 * Configuration information related to keeping history.
 *
 * @param userLoginHistoryLimit  The limit on the number of user login records
 * @param adminLoginHistoryLimit The limit on the number of admin login records
 */

public record IdServerHistoryConfiguration(
  int userLoginHistoryLimit,
  int adminLoginHistoryLimit)
  implements IdServerJSONConfigurationElementType
{
  /*
   * Logging in twice a day, every day for ten years.
   */

  private static final int HISTORY_MAX =
    2 * 365 * 10;

  /**
   * Configuration information related to keeping history.
   *
   * @param userLoginHistoryLimit  The limit on the number of user login
   *                               records
   * @param adminLoginHistoryLimit The limit on the number of admin login
   *                               records
   */

  public IdServerHistoryConfiguration(
    final int userLoginHistoryLimit,
    final int adminLoginHistoryLimit)
  {
    this.userLoginHistoryLimit =
      Math.min(HISTORY_MAX, Math.max(1, userLoginHistoryLimit));
    this.adminLoginHistoryLimit =
      Math.min(HISTORY_MAX, Math.max(1, adminLoginHistoryLimit));
  }
}

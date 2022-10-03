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


package com.io7m.idstore.database.postgres.internal;

import javax.management.MXBean;

/**
 * Database server metrics.
 */

@MXBean
public interface IdDatabaseMetricsMXBean
{
  /**
   * @return The number of transactions committed since the application started
   */

  long getTransactionCommits();

  /**
   * @return The number of transactions rolled back since the application
   * started
   */

  long getTransactionRollbacks();

  /**
   * @return The mean transaction time
   */

  double getTransactionMeanSeconds();

  /**
   * @return The max transaction time
   */

  double getTransactionMaxSeconds();

  /**
   * @return The min transaction time
   */

  double getTransactionMinSeconds();
}

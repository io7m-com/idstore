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

import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;

/**
 * An MX bean for database metrics.
 */

public final class IdDatabaseMetrics
  implements IdDatabaseMetricsMXBean
{
  private final SynchronizedDescriptiveStatistics transactionStats;
  private volatile long transactionCommits;
  private volatile long transactionRollbacks;

  /**
   * An MX bean for database metrics.
   */

  public IdDatabaseMetrics()
  {
    this.transactionCommits = 0L;
    this.transactionRollbacks = 0L;
    this.transactionStats = new SynchronizedDescriptiveStatistics(8192);
  }

  @Override
  public long getTransactionCommits()
  {
    return this.transactionCommits;
  }

  @Override
  public long getTransactionRollbacks()
  {
    return this.transactionRollbacks;
  }

  @Override
  public double getTransactionMeanSeconds()
  {
    return this.transactionStats.getMean();
  }

  @Override
  public double getTransactionMaxSeconds()
  {
    return this.transactionStats.getMax();
  }

  @Override
  public double getTransactionMinSeconds()
  {
    return this.transactionStats.getMin();
  }

  /**
   * Add a (committed) transaction time.
   *
   * @param seconds The transaction time in seconds
   */

  public void addTransactionTimeCommitted(
    final double seconds)
  {
    ++this.transactionCommits;
    this.transactionStats.addValue(seconds);
  }

  /**
   * Add a (rolled-back) transaction time.
   *
   * @param seconds The transaction time in seconds
   */

  public void addTransactionTimeRolledBack(
    final double seconds)
  {
    ++this.transactionCommits;
    this.transactionStats.addValue(seconds);
  }
}

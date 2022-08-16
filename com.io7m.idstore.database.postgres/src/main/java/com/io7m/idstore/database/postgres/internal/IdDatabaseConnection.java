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

import com.io7m.idstore.database.api.IdDatabaseConnectionType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseRole;
import com.io7m.idstore.database.api.IdDatabaseTransactionType;

import java.sql.Connection;
import java.sql.SQLException;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SQL_ERROR;

record IdDatabaseConnection(
  IdDatabase database,
  Connection connection,
  IdDatabaseRole role)
  implements IdDatabaseConnectionType
{
  @Override
  public IdDatabaseTransactionType openTransaction()
    throws IdDatabaseException
  {
    try {
      final var t =
        new IdDatabaseTransaction(
          this,
          this.database.clock().instant()
        );

      t.setRole(this.role);
      t.commit();
      return t;
    } catch (final SQLException e) {
      throw new IdDatabaseException(e.getMessage(), e, SQL_ERROR);
    }
  }

  @Override
  public void close()
    throws IdDatabaseException
  {
    try {
      if (!this.connection.isClosed()) {
        this.connection.close();
      }
    } catch (final SQLException e) {
      throw new IdDatabaseException(e.getMessage(), e, SQL_ERROR);
    }
  }
}

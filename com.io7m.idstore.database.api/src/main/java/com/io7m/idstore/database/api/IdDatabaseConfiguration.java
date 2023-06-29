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

package com.io7m.idstore.database.api;

import com.io7m.idstore.strings.IdStrings;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

/**
 * The server database configuration.
 *
 * @param ownerRoleName      The name of the role that owns the database; used for database setup and migrations
 * @param ownerRolePassword  The password of the role that owns the database
 * @param workerRolePassword The password of the worker role used for normal database operation
 * @param readerRolePassword The password of the role used for read-only database access
 * @param port               The database TCP/IP port
 * @param upgrade            The upgrade specification
 * @param create             The creation specification
 * @param address            The database address
 * @param databaseName       The database name
 * @param strings            The string resources
 * @param clock              A clock for time retrievals
 */

public record IdDatabaseConfiguration(
  String ownerRoleName,
  String ownerRolePassword,
  String workerRolePassword,
  Optional<String> readerRolePassword,
  String address,
  int port,
  String databaseName,
  IdDatabaseCreate create,
  IdDatabaseUpgrade upgrade,
  IdStrings strings,
  Clock clock)
{
  /**
   * The server database configuration.
   *
   * @param ownerRoleName      The name of the role that owns the database; used for database setup and migrations
   * @param ownerRolePassword  The password of the role that owns the database
   * @param workerRolePassword The password of the worker role used for normal database operation
   * @param readerRolePassword The password of the role used for read-only database access
   * @param port               The database TCP/IP port
   * @param upgrade            The upgrade specification
   * @param create             The creation specification
   * @param address            The database address
   * @param databaseName       The database name
   * @param strings            The string resources
   * @param clock              A clock for time retrievals
   */

  public IdDatabaseConfiguration
  {
    Objects.requireNonNull(ownerRoleName, "ownerRoleName");
    Objects.requireNonNull(ownerRolePassword, "ownerRolePassword");
    Objects.requireNonNull(workerRolePassword, "workerRolePassword");
    Objects.requireNonNull(readerRolePassword, "readerRolePassword");
    Objects.requireNonNull(address, "address");
    Objects.requireNonNull(databaseName, "databaseName");
    Objects.requireNonNull(create, "create");
    Objects.requireNonNull(upgrade, "upgrade");
    Objects.requireNonNull(strings, "strings");
    Objects.requireNonNull(clock, "clock");
  }

  /**
   * @return this, but with DO_NOT_CREATE_DATABASE and DO_NOT_UPGRADE_DATABASE.
   */

  public IdDatabaseConfiguration withoutUpgradeOrCreate()
  {
    return new IdDatabaseConfiguration(
      this.ownerRoleName(),
      this.ownerRolePassword(),
      this.workerRolePassword(),
      this.readerRolePassword(),
      this.address(),
      this.port(),
      this.databaseName(),
      IdDatabaseCreate.DO_NOT_CREATE_DATABASE,
      IdDatabaseUpgrade.DO_NOT_UPGRADE_DATABASE,
      this.strings(),
      this.clock()
    );
  }
}

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

package com.io7m.idstore.tests.database;

import com.io7m.ervilla.api.EContainerSupervisorType;
import com.io7m.ervilla.test_extension.ErvillaCloseAfterSuite;
import com.io7m.ervilla.test_extension.ErvillaConfiguration;
import com.io7m.ervilla.test_extension.ErvillaExtension;
import com.io7m.idstore.database.api.IdDatabaseConnectionType;
import com.io7m.idstore.database.api.IdDatabaseTransactionType;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.tests.extensions.IdTestDatabases;
import com.io7m.zelador.test_extension.CloseableResourcesType;
import com.io7m.zelador.test_extension.ZeladorExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.DriverManager;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({ErvillaExtension.class, ZeladorExtension.class})
@ErvillaConfiguration(disabledIfUnsupported = true, projectName = "com.io7m.idstore")
public final class IdDatabasePasswordsTest
{
  private static IdTestDatabases.IdDatabaseFixture DATABASE_FIXTURE;
  private IdDatabaseConnectionType connection;
  private IdDatabaseTransactionType transaction;
  private IdDatabaseType database;

  @BeforeAll
  public static void setupOnce(
    final @ErvillaCloseAfterSuite EContainerSupervisorType containers)
    throws Exception
  {
    DATABASE_FIXTURE =
      IdTestDatabases.createWithHostilePasswords(containers, 15433);
  }

  @BeforeEach
  public void setup(
    final CloseableResourcesType closeables)
    throws Exception
  {
    this.database =
      closeables.addPerTestResource(DATABASE_FIXTURE.createDatabase());
    this.connection =
      closeables.addPerTestResource(this.database.openConnection(IDSTORE));
    this.transaction =
      closeables.addPerTestResource(this.connection.openTransaction());
  }

  /**
   * Accessing the database works with hostile passwords. This tests that
   * passwords are correctly escaped.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUsers()
    throws Exception
  {
    final var dbConfig =
      DATABASE_FIXTURE.databaseConfiguration();

    final var url =
      "jdbc:postgresql://%s:%d/%s"
        .formatted(
          dbConfig.address(),
          Integer.valueOf(dbConfig.port()),
          dbConfig.databaseName()
        );

    try (var c =
           DriverManager.getConnection(
             url,
             dbConfig.ownerRoleName(),
             dbConfig.ownerRolePassword()
           )) {
      assertTrue(c.isValid(1000));
    }

    try (var c =
           DriverManager.getConnection(
             url,
             "idstore",
             dbConfig.workerRolePassword()
           )) {
      assertTrue(c.isValid(1000));
    }

    try (var c =
           DriverManager.getConnection(
             url,
             "idstore_read_only",
             dbConfig.readerRolePassword().get()
           )) {
      assertTrue(c.isValid(1000));
    }
  }
}

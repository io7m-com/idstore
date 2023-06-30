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
import com.io7m.ervilla.test_extension.ErvillaCloseAfterAll;
import com.io7m.ervilla.test_extension.ErvillaConfiguration;
import com.io7m.ervilla.test_extension.ErvillaExtension;
import com.io7m.idstore.database.api.IdDatabaseConnectionType;
import com.io7m.idstore.database.api.IdDatabaseEmailsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseMaintenanceQueriesType;
import com.io7m.idstore.database.api.IdDatabaseTransactionType;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.model.IdBan;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdEmailVerification;
import com.io7m.idstore.model.IdEmailVerificationOperation;
import com.io7m.idstore.model.IdToken;
import com.io7m.idstore.tests.extensions.IdTestDatabases;
import com.io7m.zelador.test_extension.CloseableResourcesType;
import com.io7m.zelador.test_extension.ZeladorExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Optional;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static java.util.Optional.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({ErvillaExtension.class, ZeladorExtension.class})
@ErvillaConfiguration(disabledIfUnsupported = true)
public final class IdDatabaseMaintenanceTest
{
  private static IdTestDatabases.IdDatabaseFixture DATABASE_FIXTURE;
  private IdDatabaseConnectionType connection;
  private IdDatabaseTransactionType transaction;
  private IdDatabaseType database;

  @BeforeAll
  public static void setupOnce(
    final @ErvillaCloseAfterAll EContainerSupervisorType containers)
    throws Exception
  {
    DATABASE_FIXTURE =
      IdTestDatabases.create(containers, 15432);
  }

  @BeforeEach
  public void setup(
    final CloseableResourcesType closeables)
    throws Exception
  {
    DATABASE_FIXTURE.reset();

    this.database =
      closeables.addPerTestResource(DATABASE_FIXTURE.createDatabase());
    this.connection =
      closeables.addPerTestResource(this.database.openConnection(IDSTORE));
    this.transaction =
      closeables.addPerTestResource(this.connection.openTransaction());
  }

  @Test
  public void testExpiration()
    throws Exception
  {
    final var admin =
      IdTestDatabases.createAdminInitial(
        this.transaction, "admin", "12345678");
    final var user =
      IdTestDatabases.createUser(
        this.transaction, admin, "someone", "12345678");

    final var emails =
      this.transaction.queries(IdDatabaseEmailsQueriesType.class);
    final var users =
      this.transaction.queries(IdDatabaseUsersQueriesType.class);
    final var maintenance =
      this.transaction.queries(IdDatabaseMaintenanceQueriesType.class);

    this.transaction.userIdSet(user);

    final var emailToken0 =
      IdToken.generate();
    final var emailToken1 =
      IdToken.generate();

    emails.emailVerificationCreate(
      new IdEmailVerification(
        user,
        new IdEmail("someone@example.com"),
        emailToken0,
        emailToken1,
        IdEmailVerificationOperation.EMAIL_ADD,
        IdTestDatabases.timeNow().minusYears(30L)
      )
    );

    this.transaction.adminIdSet(admin);

    users.userBanCreate(
      new IdBan(user, "Spite", Optional.of(
        IdTestDatabases.timeNow().minusYears(30L)
      ))
    );

    maintenance.runMaintenance();
    assertEquals(empty(), emails.emailVerificationGetPermit(emailToken0));
    assertEquals(empty(), emails.emailVerificationGetDeny(emailToken1));
    assertEquals(empty(), users.userBanGet(user));
  }
}

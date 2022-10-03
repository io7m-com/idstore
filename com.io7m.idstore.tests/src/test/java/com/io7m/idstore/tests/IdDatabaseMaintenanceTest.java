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

package com.io7m.idstore.tests;

import com.io7m.idstore.database.api.IdDatabaseEmailsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseMaintenanceQueriesType;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.model.IdBan;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdEmailVerification;
import com.io7m.idstore.model.IdEmailVerificationOperation;
import com.io7m.idstore.model.IdToken;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static java.util.Optional.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers(disabledWithoutDocker = true)
public final class IdDatabaseMaintenanceTest
  extends IdWithDatabaseContract
{
  @Test
  public void testExpiration()
    throws Exception
  {
    final var admin =
      this.databaseCreateAdminInitial("admin", "12345678");
    final var user =
      this.databaseCreateUserInitial(admin, "someone", "12345678");

    final var transaction =
      this.transactionOf(IDSTORE);
    final var emails =
      transaction.queries(IdDatabaseEmailsQueriesType.class);
    final var users =
      transaction.queries(IdDatabaseUsersQueriesType.class);
    final var maintenance =
      transaction.queries(IdDatabaseMaintenanceQueriesType.class);

    transaction.userIdSet(user);

    final var emailToken = IdToken.generate();
    emails.emailVerificationCreate(
      new IdEmailVerification(
        user,
        new IdEmail("someone@example.com"),
        emailToken,
        IdEmailVerificationOperation.EMAIL_ADD,
        timeNow().minusYears(30L)
      )
    );

    transaction.adminIdSet(admin);

    users.userBanCreate(
      new IdBan(user, "Spite", Optional.of(
        timeNow().minusYears(30L)
      ))
    );

    maintenance.runMaintenance();
    assertEquals(empty(), emails.emailVerificationGet(emailToken));
    assertEquals(empty(), users.userBanGet(user));
  }
}

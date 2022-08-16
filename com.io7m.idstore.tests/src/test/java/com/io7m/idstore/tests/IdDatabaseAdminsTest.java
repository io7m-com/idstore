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

import com.io7m.idstore.database.api.IdDatabaseAdminsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.model.IdAdminSummary;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPassword;
import com.io7m.idstore.model.IdPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdRealName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Set;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_DUPLICATE_EMAIL;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_DUPLICATE_ID;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_DUPLICATE_ID_NAME;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_NONEXISTENT;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_NOT_INITIAL;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_UNSET;
import static java.time.OffsetDateTime.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
public final class IdDatabaseAdminsTest extends IdWithDatabaseContract
{
  /**
   * Setting the transaction admin to a nonexistent admin fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminSetNonexistent()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var transaction =
      this.transactionOf(IDSTORE);

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        transaction.adminIdSet(randomUUID());
      });
    assertEquals(ADMIN_NONEXISTENT, ex.errorCode());
  }

  /**
   * Creating an admin works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdmin()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var transaction =
      this.transactionOf(IDSTORE);
    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);
    final var adminId =
      this.databaseCreateAdminInitial("admin", "12345678");

    final var reqId =
      randomUUID();
    final var now =
      now();

    final var password =
      createBadPassword();

    transaction.adminIdSet(adminId);

    var admin =
      admins.adminCreate(
        reqId,
        new IdName("someone"),
        new IdRealName("Someone R. Admin"),
        new IdEmail("someone@example.com"),
        now,
        password,
        Set.of()
      );

    assertEquals("someone", admin.idName().value());
    assertEquals(reqId, admin.id());
    assertEquals("someone@example.com", admin.emails().first().value());
    assertEquals(now.toEpochSecond(), admin.timeCreated().toEpochSecond());
    assertEquals(now.toEpochSecond(), admin.timeUpdated().toEpochSecond());

    admin = admins.adminGet(reqId).orElseThrow();
    assertEquals("someone", admin.idName().value());
    assertEquals(reqId, admin.id());
    assertEquals("someone@example.com", admin.emails().first().value());
    assertEquals(now.toEpochSecond(), admin.timeCreated().toEpochSecond());
    assertEquals(now.toEpochSecond(), admin.timeUpdated().toEpochSecond());

    admin = admins.adminGetForEmail(new IdEmail("someone@example.com")).orElseThrow();
    assertEquals("someone", admin.idName().value());
    assertEquals(reqId, admin.id());
    assertEquals("someone@example.com", admin.emails().first().value());
    assertEquals(now.toEpochSecond(), admin.timeCreated().toEpochSecond());
    assertEquals(now.toEpochSecond(), admin.timeUpdated().toEpochSecond());

    admin = admins.adminGetForName(new IdName("someone")).orElseThrow();
    assertEquals("someone", admin.idName().value());
    assertEquals(reqId, admin.id());
    assertEquals("someone@example.com", admin.emails().first().value());
    assertEquals(now.toEpochSecond(), admin.timeCreated().toEpochSecond());
    assertEquals(now.toEpochSecond(), admin.timeUpdated().toEpochSecond());

    checkAuditLog(
      transaction,
      new ExpectedEvent("ADMIN_CREATED", adminId.toString()),
      new ExpectedEvent("ADMIN_CREATED", reqId.toString())
    );
  }

  /**
   * Trying to create an initial admin when one already exists, fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminNotInitial()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var transaction =
      this.transactionOf(IDSTORE);
    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);
    final var adminId =
      this.databaseCreateAdminInitial("admin", "12345678");

    final var reqId =
      randomUUID();
    final var now =
      now();

    final var password =
      createBadPassword();

    transaction.adminIdSet(adminId);

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        admins.adminCreateInitial(
          reqId,
          new IdName("someone"),
          new IdRealName("Someone R. Admin"),
          new IdEmail("someone@example.com"),
          now,
          password
        );
      });

    assertEquals(ADMIN_NOT_INITIAL, ex.errorCode());
  }

  /**
   * Creating an admin requires an admin.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminRequiresAdmin()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var transaction =
      this.transactionOf(IDSTORE);
    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();
    final var password =
      createBadPassword();

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        admins.adminCreate(
          reqId,
          new IdName("someone2"),
          new IdRealName("Someone R. Admin"),
          new IdEmail("someone2@example.com"),
          now,
          password,
          Set.of()
        );
      });

    assertEquals(ADMIN_UNSET, ex.errorCode());
  }

  /**
   * Creating an admin with a duplicate ID fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminDuplicateId()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var transaction =
      this.transactionOf(IDSTORE);
    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);
    final var adminId =
      this.databaseCreateAdminInitial("admin", "12345678");

    final var reqId =
      randomUUID();
    final var now =
      now();

    final var password =
      createBadPassword();

    transaction.adminIdSet(adminId);

    final var admin =
      admins.adminCreate(
        reqId,
        new IdName("someone"),
        new IdRealName("Someone R. Admin"),
        new IdEmail("someone@example.com"),
        now,
        password,
        Set.of()
      );

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        admins.adminCreate(
          reqId,
          new IdName("someone2"),
          new IdRealName("Someone R. Admin"),
          new IdEmail("someone2@example.com"),
          now,
          password,
          Set.of()
        );
      });

    assertEquals(ADMIN_DUPLICATE_ID, ex.errorCode());
  }

  /**
   * Creating an admin with a duplicate email fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminDuplicateEmail()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var transaction =
      this.transactionOf(IDSTORE);
    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);
    final var adminId =
      this.databaseCreateAdminInitial("admin", "12345678");

    final var reqId =
      randomUUID();
    final var now =
      now();

    final var password =
      createBadPassword();

    transaction.adminIdSet(adminId);

    final var admin =
      admins.adminCreate(
        reqId,
        new IdName("someone"),
        new IdRealName("Someone R. Admin"),
        new IdEmail("someone@example.com"),
        now,
        password,
        Set.of()
      );

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        admins.adminCreate(
          randomUUID(),
          new IdName("someone2"),
          new IdRealName("Someone R. Admin"),
          new IdEmail("someone@example.com"),
          now,
          password,
          Set.of()
        );
      });

    assertEquals(ADMIN_DUPLICATE_EMAIL, ex.errorCode());
  }

  /**
   * Creating an admin with a duplicate name fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminDuplicateName()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var transaction =
      this.transactionOf(IDSTORE);
    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);
    final var adminId =
      this.databaseCreateAdminInitial("admin", "12345678");

    final var reqId =
      randomUUID();
    final var now =
      now();

    final var password =
      createBadPassword();

    transaction.adminIdSet(adminId);

    final var admin =
      admins.adminCreate(
        reqId,
        new IdName("someone"),
        new IdRealName("Someone R. Admin"),
        new IdEmail("someone@example.com"),
        now,
        password,
        Set.of()
      );

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        admins.adminCreate(
          randomUUID(),
          new IdName("someone"),
          new IdRealName("Someone R. Admin"),
          new IdEmail("someone2@example.com"),
          now,
          password,
          Set.of()
        );
      });

    assertEquals(ADMIN_DUPLICATE_ID_NAME, ex.errorCode());
  }

  /**
   * Logging in works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminLogin()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var transaction =
      this.transactionOf(IDSTORE);
    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);
    final var adminId =
      this.databaseCreateAdminInitial("admin", "12345678");

    final var id =
      randomUUID();
    final var now =
      now();

    final var password =
      createBadPassword();

    transaction.adminIdSet(adminId);

    final var admin =
      admins.adminCreate(
        id,
        new IdName("someone"),
        new IdRealName("Someone R. Admin"),
        new IdEmail("someone@example.com"),
        now,
        password,
        Set.of()
      );

    admins.adminLogin(
      admin.id(),
      "Mozilla/5.0 (X11; Linux x86_64)",
      "127.0.0.1"
    );

    checkAuditLog(
      transaction,
      new ExpectedEvent("ADMIN_CREATED", adminId.toString()),
      new ExpectedEvent("ADMIN_CREATED", id.toString()),
      new ExpectedEvent("ADMIN_LOGGED_IN", "127.0.0.1")
    );
  }

  /**
   * Logging in fails for nonexistent admins.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminLoginNonexistent()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var transaction =
      this.transactionOf(IDSTORE);
    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        admins.adminLogin(
          randomUUID(),
          "Mozilla/5.0 (X11; Linux x86_64)",
          "127.0.0.1");
      });
    assertEquals(ADMIN_NONEXISTENT, ex.errorCode());
  }

  /**
   * Searching works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminSearch()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var transaction =
      this.transactionOf(IDSTORE);
    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);
    final var adminId =
      this.databaseCreateAdminInitial("admin", "12345678");

    assertEquals(
      List.of(),
      admins.adminSearch("matches nothing")
    );
    assertEquals(
      List.of(adminId),
      admins.adminSearch(" Real Name")
        .stream()
        .map(IdAdminSummary::id)
        .toList()
    );
    assertEquals(
      List.of(adminId),
      admins.adminSearch("admin")
        .stream()
        .map(IdAdminSummary::id)
        .toList()
    );
  }

  private static IdPassword createBadPassword()
    throws IdPasswordException
  {
    return IdPasswordAlgorithmPBKDF2HmacSHA256.create()
      .createHashed("12345678");
  }
}

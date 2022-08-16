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

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseUserListPaging;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdTimeRange;
import com.io7m.idstore.model.IdUserColumnOrdering;
import com.io7m.idstore.model.IdUserListParameters;
import com.io7m.idstore.model.IdUserOrdering;
import com.io7m.idstore.model.IdUserSummary;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.EMAIL_ONE_REQUIRED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.USER_DUPLICATE_EMAIL;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.USER_DUPLICATE_ID;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.USER_DUPLICATE_ID_NAME;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.USER_NONEXISTENT;
import static com.io7m.idstore.model.IdUserColumn.BY_IDNAME;
import static com.io7m.idstore.model.IdUserColumn.BY_REALNAME;
import static com.io7m.idstore.model.IdUserColumn.BY_TIME_CREATED;
import static com.io7m.idstore.model.IdUserColumn.BY_TIME_UPDATED;
import static java.time.OffsetDateTime.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
public final class IdDatabaseUsersTest extends IdWithDatabaseContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdDatabaseUsersTest.class);

  /**
   * Setting the transaction user to a nonexistent user fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserSetNonexistent()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var transaction =
      this.transactionOf(IDSTORE);

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        transaction.userIdSet(randomUUID());
      });
    assertEquals(USER_NONEXISTENT, ex.errorCode());
  }

  /**
   * Creating a user works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUser()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var transaction =
      this.transactionOf(IDSTORE);

    transaction.adminIdSet(adminId);

    final var users =
      transaction.queries(IdDatabaseUsersQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();

    final var password =
      databaseGenerateBadPassword();

    var user =
      users.userCreate(
        reqId,
        new IdName("someone"),
        new IdRealName("someone"),
        new IdEmail("someone@example.com"),
        now,
        password
      );

    assertEquals("someone", user.realName().value());
    assertEquals(reqId, user.id());
    assertEquals("someone@example.com", user.emails().first().value());
    assertEquals(now.toEpochSecond(), user.timeCreated().toEpochSecond());
    assertEquals(now.toEpochSecond(), user.timeUpdated().toEpochSecond());

    user = users.userGet(reqId).orElseThrow();
    assertEquals("someone", user.realName().value());
    assertEquals(reqId, user.id());
    assertEquals("someone@example.com", user.emails().first().value());
    assertEquals(now.toEpochSecond(), user.timeCreated().toEpochSecond());
    assertEquals(now.toEpochSecond(), user.timeUpdated().toEpochSecond());

    user = users.userGetForEmail(new IdEmail("someone@example.com")).orElseThrow();
    assertEquals("someone", user.realName().value());
    assertEquals(reqId, user.id());
    assertEquals("someone@example.com", user.emails().first().value());
    assertEquals(now.toEpochSecond(), user.timeCreated().toEpochSecond());
    assertEquals(now.toEpochSecond(), user.timeUpdated().toEpochSecond());

    user = users.userGetForName(new IdName("someone")).orElseThrow();
    assertEquals("someone", user.realName().value());
    assertEquals(reqId, user.id());
    assertEquals("someone@example.com", user.emails().first().value());
    assertEquals(now.toEpochSecond(), user.timeCreated().toEpochSecond());
    assertEquals(now.toEpochSecond(), user.timeUpdated().toEpochSecond());

    checkAuditLog(
      transaction,
      new ExpectedEvent("ADMIN_CREATED", adminId.toString()),
      new ExpectedEvent("USER_CREATED", user.id().toString())
    );
  }

  /**
   * Creating a user with a duplicate ID fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserDuplicateId()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var transaction =
      this.transactionOf(IDSTORE);

    transaction.adminIdSet(adminId);

    final var users =
      transaction.queries(IdDatabaseUsersQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();

    final var password =
      databaseGenerateBadPassword();

    final var user =
      users.userCreate(
        reqId,
        new IdName("someone"),
        new IdRealName("someone"),
        new IdEmail("someone@example.com"),
        now,
        password
      );

    {
      final var ex =
        assertThrows(IdDatabaseException.class, () -> {
          users.userCreate(
            reqId,
            new IdName("someone"),
            new IdRealName("someoneElse"),
            new IdEmail("someone2@example.com"),
            now,
            password
          );
        });
      assertEquals(USER_DUPLICATE_ID, ex.errorCode());
    }

    {
      final var ex =
        assertThrows(IdDatabaseException.class, () -> {
          users.userCreate(
            randomUUID(),
            new IdName("someone"),
            new IdRealName("someoneElse"),
            new IdEmail("someone2@example.com"),
            now,
            password
          );
        });
      assertEquals(USER_DUPLICATE_ID_NAME, ex.errorCode());
    }

    {
      final var ex =
        assertThrows(IdDatabaseException.class, () -> {
          users.userCreate(
            randomUUID(),
            new IdName("someone2"),
            new IdRealName("someoneElse"),
            new IdEmail("someone@example.com"),
            now,
            password
          );
        });
      assertEquals(USER_DUPLICATE_EMAIL, ex.errorCode());
    }
  }

  /**
   * Creating a user with a duplicate email fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserDuplicateEmail()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var transaction =
      this.transactionOf(IDSTORE);

    transaction.adminIdSet(adminId);

    final var users =
      transaction.queries(IdDatabaseUsersQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();

    final var password =
      databaseGenerateBadPassword();

    final var user =
      users.userCreate(
        reqId,
        new IdName("someone"),
        new IdRealName("someone"),
        new IdEmail("someone@example.com"),
        now,
        password
      );

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        users.userCreate(
          randomUUID(),
          new IdName("someone2"),
          new IdRealName("someoneElse"),
          new IdEmail("someone@example.com"),
          now,
          password
        );
      });

    assertEquals(USER_DUPLICATE_EMAIL, ex.errorCode());
  }

  /**
   * Logging in works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserLogin()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var transaction =
      this.transactionOf(IDSTORE);

    transaction.adminIdSet(adminId);

    final var users =
      transaction.queries(IdDatabaseUsersQueriesType.class);

    final var id =
      randomUUID();
    final var now =
      now();

    final var password =
      databaseGenerateBadPassword();

    final var user =
      users.userCreate(
        id,
        new IdName("someone"),
        new IdRealName("someone"),
        new IdEmail("someone@example.com"),
        now,
        password
      );

    users.userLogin(
      user.id(),
      "Mozilla/5.0 (X11; Linux x86_64)",
      "127.0.0.1"
    );

    checkAuditLog(
      transaction,
      new ExpectedEvent("ADMIN_CREATED", adminId.toString()),
      new ExpectedEvent("USER_CREATED", id.toString()),
      new ExpectedEvent("USER_LOGGED_IN", "127.0.0.1")
    );
  }

  /**
   * Logging in fails for nonexistent users.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserLoginNonexistent()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var transaction =
      this.transactionOf(IDSTORE);

    final var users =
      transaction.queries(IdDatabaseUsersQueriesType.class);

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        users.userLogin(
          randomUUID(),
          "Mozilla/5.0 (X11; Linux x86_64)",
          "127.0.0.1");
      });
    assertEquals(USER_NONEXISTENT, ex.errorCode());
  }

  /**
   * Users can be listed.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserList()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var transaction =
      this.transactionOf(IDSTORE);

    transaction.adminIdSet(adminId);

    final var users =
      transaction.queries(IdDatabaseUsersQueriesType.class);

    final var now =
      now();
    final var password =
      databaseGenerateBadPassword();

    final var userList = new ArrayList<>();
    for (int index = 0; index < 500; ++index) {
      userList.add(
        users.userCreate(
          randomUUID(),
          new IdName("someone_%03d".formatted(index)),
          new IdRealName("someone %03d".formatted(index)),
          new IdEmail("someone_%03d@example.com".formatted(index)),
          now,
          password
        )
      );
    }

    {
      final var usersListed =
        users.userList(
          new IdTimeRange(now.minusDays(1L), now.plusDays(1L)),
          new IdTimeRange(now.minusDays(1L), now.plusDays(1L)),
          new IdUserOrdering(List.of(new IdUserColumnOrdering(
            BY_IDNAME,
            true))),
          600,
          Optional.empty()
        );

      assertEquals(500, usersListed.size());

      for (int index = 0; index < 500; ++index) {
        final var name =
          new IdName("someone_%03d".formatted(index));
        final var realName =
          new IdRealName("someone %03d".formatted(index));
        final var email =
          new IdEmail("someone_%03d@example.com".formatted(index));

        final var u = usersListed.get(index);
        assertEquals(name, u.idName());
        assertEquals(realName, u.realName());
      }
    }

    {
      final var usersListed =
        users.userList(
          new IdTimeRange(now.minusDays(1L), now.plusDays(1L)),
          new IdTimeRange(now.minusDays(1L), now.plusDays(1L)),
          new IdUserOrdering(List.of(new IdUserColumnOrdering(
            BY_IDNAME,
            false))),
          600,
          Optional.empty()
        );

      assertEquals(500, usersListed.size());

      for (int index = 0; index < 500; ++index) {
        final var id =
          500 - (index + 1);
        final var name =
          new IdName("someone_%03d".formatted(id));
        final var realName =
          new IdRealName("someone %03d".formatted(id));
        final var email =
          new IdEmail("someone_%03d@example.com".formatted(id));

        final var u = usersListed.get(index);
        assertEquals(name, u.idName());
        assertEquals(realName, u.realName());
      }
    }
  }

  /**
   * Users can be listed and paging works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserListPaging()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var transaction =
      this.transactionOf(IDSTORE);

    transaction.adminIdSet(adminId);

    final var users =
      transaction.queries(IdDatabaseUsersQueriesType.class);

    final var now =
      now();
    final var password =
      databaseGenerateBadPassword();

    final var userList = new ArrayList<>();
    for (int index = 0; index < 500; ++index) {
      userList.add(
        users.userCreate(
          randomUUID(),
          new IdName("someone_%03d".formatted(index)),
          new IdRealName("someone %03d".formatted(index)),
          new IdEmail("someone_%03d@example.com".formatted(index)),
          now,
          password
        )
      );
    }

    final var paging =
      IdDatabaseUserListPaging.create(
        new IdUserListParameters(
          new IdTimeRange(now.minusDays(1L), now.plusDays(1L)),
          new IdTimeRange(now.minusDays(1L), now.plusDays(1L)),
          new IdUserOrdering(List.of(
            new IdUserColumnOrdering(BY_IDNAME, true),
            new IdUserColumnOrdering(BY_REALNAME, true),
            new IdUserColumnOrdering(BY_TIME_UPDATED, true),
            new IdUserColumnOrdering(BY_TIME_CREATED, true)
          )),
          150
        ));

    List<IdUserSummary> items;
    assertEquals(0, paging.pageNumber());
    items = paging.pageCurrent(users);
    assertEquals(150, items.size());
    checkPage(0, 150, items);
    assertTrue(paging.pageNextAvailable());

    items = paging.pageNext(users);
    assertEquals(150, items.size());
    assertEquals(1, paging.pageNumber());
    checkPage(150, 300, items);
    assertTrue(paging.pageNextAvailable());

    items = paging.pageNext(users);
    assertEquals(150, items.size());
    assertEquals(2, paging.pageNumber());
    checkPage(300, 450, items);
    assertTrue(paging.pageNextAvailable());

    items = paging.pageNext(users);
    assertEquals(50, items.size());
    assertEquals(3, paging.pageNumber());
    checkPage(450, 500, items);
    assertFalse(paging.pageNextAvailable());

    items = paging.pagePrevious(users);
    assertEquals(150, items.size());
    assertEquals(2, paging.pageNumber());
    checkPage(300, 450, items);
    assertTrue(paging.pageNextAvailable());

    items = paging.pagePrevious(users);
    assertEquals(150, items.size());
    assertEquals(1, paging.pageNumber());
    checkPage(150, 300, items);
    assertTrue(paging.pageNextAvailable());

    items = paging.pagePrevious(users);
    assertEquals(150, items.size());
    assertEquals(0, paging.pageNumber());
    checkPage(0, 150, items);
    assertTrue(paging.pageNextAvailable());
  }

  /**
   * Updating a user works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserUpdate()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var transaction =
      this.transactionOf(IDSTORE);

    transaction.adminIdSet(adminId);

    final var users =
      transaction.queries(IdDatabaseUsersQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();
    final var password =
      databaseGenerateBadPassword();

    var user =
      users.userCreate(
        reqId,
        new IdName("someone"),
        new IdRealName("someone"),
        new IdEmail("someone@example.com"),
        now,
        password
      );

    assertEquals("someone", user.realName().value());
    assertEquals(reqId, user.id());
    assertEquals("someone@example.com", user.emails().first().value());
    assertEquals(now.toEpochSecond(), user.timeCreated().toEpochSecond());
    assertEquals(now.toEpochSecond(), user.timeUpdated().toEpochSecond());
    assertEquals(password, user.password());

    transaction.userIdSet(user.id());

    final var otherPassword = databaseGenerateDifferentBadPassword();
    users.userUpdate(
      user.id(),
      Optional.of(new IdName("newIdName")),
      Optional.of(new IdRealName("newRealName")),
      Optional.of(otherPassword)
    );

    user = users.userGetRequire(user.id());

    assertEquals("newRealName", user.realName().value());
    assertEquals(reqId, user.id());
    assertEquals("someone@example.com", user.emails().first().value());
    assertEquals("newIdName", user.idName().value());
    assertEquals(now.toEpochSecond(), user.timeCreated().toEpochSecond());
    assertEquals(now.toEpochSecond(), user.timeUpdated().toEpochSecond());
    assertEquals(otherPassword, user.password());

    checkAuditLog(
      transaction,
      new ExpectedEvent("ADMIN_CREATED", adminId.toString()),
      new ExpectedEvent("USER_CREATED", user.id().toString())
    );
  }

  /**
   * Updating a user fails for nonexistent users.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserUpdateNonexistent()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var transaction =
      this.transactionOf(IDSTORE);

    transaction.adminIdSet(adminId);

    final var users =
      transaction.queries(IdDatabaseUsersQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();
    final var password =
      databaseGenerateBadPassword();

    final var user =
      users.userCreate(
        reqId,
        new IdName("someone"),
        new IdRealName("someone"),
        new IdEmail("someone@example.com"),
        now,
        password
      );

    assertEquals("someone", user.realName().value());
    assertEquals(reqId, user.id());
    assertEquals("someone@example.com", user.emails().first().value());
    assertEquals(now.toEpochSecond(), user.timeCreated().toEpochSecond());
    assertEquals(now.toEpochSecond(), user.timeUpdated().toEpochSecond());
    assertEquals(password, user.password());

    transaction.userIdSet(user.id());

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        final var otherPassword = databaseGenerateDifferentBadPassword();
        users.userUpdate(
          randomUUID(),
          Optional.of(new IdName("newIdName")),
          Optional.of(new IdRealName("newRealName")),
          Optional.of(otherPassword)
        );
      });

    assertEquals(USER_NONEXISTENT, ex.errorCode());
  }


  /**
   * Adding and removing email addresses works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserEmailAddresses()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var transaction =
      this.transactionOf(IDSTORE);

    transaction.adminIdSet(adminId);

    final var users =
      transaction.queries(IdDatabaseUsersQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();
    final var password =
      databaseGenerateBadPassword();

    var user =
      users.userCreate(
        reqId,
        new IdName("someone"),
        new IdRealName("someone"),
        new IdEmail("someone@example.com"),
        now,
        password
      );

    users.userEmailAdd(reqId, new IdEmail("someone2@example.com"));
    user = users.userGetRequire(reqId);

    assertTrue(
      user.emails().contains(new IdEmail("someone@example.com")));
    assertTrue(
      user.emails().contains(new IdEmail("someone2@example.com")));
    assertEquals(2, user.emails().toList().size());

    users.userEmailRemove(reqId, new IdEmail("someone@example.com"));
    user = users.userGetRequire(reqId);
    assertEquals(1, user.emails().toList().size());

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        users.userEmailRemove(reqId, new IdEmail("someone2@example.com"));
      });
    assertEquals(EMAIL_ONE_REQUIRED, ex.errorCode());
  }


  private static void checkPage(
    final int indexLow,
    final int indexHigh,
    final List<IdUserSummary> items)
  {
    var index = indexLow;
    final var iter = items.iterator();
    while (index < indexHigh) {
      final var item = iter.next();
      final var name =
        new IdName("someone_%03d".formatted(index));
      final var realName =
        new IdRealName("someone %03d".formatted(index));
      final var email =
        new IdEmail("someone_%03d@example.com".formatted(index));

      assertEquals(name, item.idName());
      assertEquals(realName, item.realName());
      ++index;
    }
  }
}

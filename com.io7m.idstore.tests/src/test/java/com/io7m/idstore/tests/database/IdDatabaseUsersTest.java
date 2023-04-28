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

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseTransactionType;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.model.IdBan;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdTimeRange;
import com.io7m.idstore.model.IdUserColumnOrdering;
import com.io7m.idstore.model.IdUserSearchByEmailParameters;
import com.io7m.idstore.model.IdUserSearchParameters;
import com.io7m.idstore.model.IdUserSummary;
import com.io7m.idstore.tests.IdTestUserSet;
import com.io7m.idstore.tests.database.IdDatabaseExtension.ExpectedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.EMAIL_DUPLICATE;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.EMAIL_ONE_REQUIRED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.USER_DUPLICATE_ID;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.USER_DUPLICATE_ID_NAME;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.USER_NONEXISTENT;
import static com.io7m.idstore.model.IdLoginMetadataStandard.remoteHost;
import static com.io7m.idstore.model.IdLoginMetadataStandard.remoteHostProxied;
import static com.io7m.idstore.model.IdUserColumn.BY_IDNAME;
import static com.io7m.idstore.tests.database.IdDatabaseExtension.checkAuditLog;
import static com.io7m.idstore.tests.database.IdDatabaseExtension.databaseCreateAdminInitial;
import static com.io7m.idstore.tests.database.IdDatabaseExtension.databaseGenerateBadPassword;
import static com.io7m.idstore.tests.database.IdDatabaseExtension.databaseGenerateDifferentBadPassword;
import static com.io7m.idstore.tests.database.IdDatabaseExtension.timeNow;
import static java.time.OffsetDateTime.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
@ExtendWith(IdDatabaseExtension.class)
public final class IdDatabaseUsersTest
{
  /**
   * Setting the transaction user to a nonexistent user fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserSetNonexistent(
    final IdDatabaseTransactionType t)
    throws Exception
  {
    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        t.userIdSet(randomUUID());
      });
    assertEquals(USER_NONEXISTENT, ex.errorCode());
  }

  /**
   * Creating a user works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserCreate0(
    final IdDatabaseTransactionType t)
    throws Exception
  {
    final var adminId =
      databaseCreateAdminInitial(t, "admin", "12345678");

    t.adminIdSet(adminId);

    final var users =
      t.queries(IdDatabaseUsersQueriesType.class);

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
      t,
      new ExpectedEvent("ADMIN_CREATED", adminId.toString()),
      new ExpectedEvent("USER_CREATED", user.id().toString())
    );
  }

  /**
   * Creating a user works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserCreate1(
    final IdDatabaseTransactionType t)
    throws Exception
  {
    final var adminId =
      databaseCreateAdminInitial(t, "admin", "12345678");

    t.adminIdSet(adminId);

    final var users =
      t.queries(IdDatabaseUsersQueriesType.class);

    final var password =
      databaseGenerateBadPassword();

    var user =
      users.userCreate(
        new IdName("someone"),
        new IdRealName("someone"),
        new IdEmail("someone@example.com"),
        password
      );

    assertEquals("someone", user.realName().value());
    assertEquals("someone@example.com", user.emails().first().value());

    user = users.userGet(user.id()).orElseThrow();
    assertEquals("someone", user.realName().value());
    assertEquals("someone@example.com", user.emails().first().value());

    user = users.userGetForEmail(new IdEmail("someone@example.com")).orElseThrow();
    assertEquals("someone", user.realName().value());
    assertEquals("someone@example.com", user.emails().first().value());

    user = users.userGetForName(new IdName("someone")).orElseThrow();
    assertEquals("someone", user.realName().value());
    assertEquals("someone@example.com", user.emails().first().value());

    checkAuditLog(
      t,
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
  public void testUserDuplicateId(
    final IdDatabaseTransactionType t)
    throws Exception
  {
    final var adminId =
      databaseCreateAdminInitial(t, "admin", "12345678");

    t.adminIdSet(adminId);

    final var users =
      t.queries(IdDatabaseUsersQueriesType.class);

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

    t.commit();

    {
      final var ex =
        assertThrows(IdDatabaseException.class, () -> {
          users.userCreate(
            reqId,
            user.idName(),
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
            user.idName(),
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
      assertEquals(EMAIL_DUPLICATE, ex.errorCode());
    }
  }

  /**
   * Creating a user with a duplicate email fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserDuplicateEmail(
    final IdDatabaseTransactionType t)
    throws Exception
  {
    final var adminId =
      databaseCreateAdminInitial(t, "admin", "12345678");

    t.adminIdSet(adminId);

    final var users =
      t.queries(IdDatabaseUsersQueriesType.class);

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

    assertEquals(EMAIL_DUPLICATE, ex.errorCode());
  }

  /**
   * Logging in works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserLogin(
    final IdDatabaseTransactionType t)
    throws Exception
  {
    final var adminId =
      databaseCreateAdminInitial(t, "admin", "12345678");

    t.adminIdSet(adminId);

    final var users =
      t.queries(IdDatabaseUsersQueriesType.class);

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
      Map.of(remoteHost(), "127.0.0.1"),
      100
    );

    checkAuditLog(
      t,
      new ExpectedEvent("ADMIN_CREATED", adminId.toString()),
      new ExpectedEvent("USER_CREATED", id.toString()),
      new ExpectedEvent("USER_LOGGED_IN", "127.0.0.1")
    );
  }

  /**
   * Logging in works for proxied hosts.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserLoginProxied(
    final IdDatabaseTransactionType t)
    throws Exception
  {
    final var adminId =
      databaseCreateAdminInitial(t, "admin", "12345678");

    t.adminIdSet(adminId);

    final var users =
      t.queries(IdDatabaseUsersQueriesType.class);

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
      Map.ofEntries(
        Map.entry(remoteHost(), "127.0.0.1"),
        Map.entry(remoteHostProxied(), "fe80:0:0:0:18c6:61ff:fedb:dfed")
      ),
      100
    );

    checkAuditLog(
      t,
      new ExpectedEvent("ADMIN_CREATED", adminId.toString()),
      new ExpectedEvent("USER_CREATED", id.toString()),
      new ExpectedEvent(
        "USER_LOGGED_IN",
        "127.0.0.1 (fe80:0:0:0:18c6:61ff:fedb:dfed)")
    );
  }

  /**
   * Logging in fails for nonexistent users.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserLoginNonexistent(
    final IdDatabaseTransactionType t)
    throws Exception
  {
    final var users =
      t.queries(IdDatabaseUsersQueriesType.class);

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        users.userLogin(
          randomUUID(),
          Map.of(),
          100
        );
      });
    assertEquals(USER_NONEXISTENT, ex.errorCode());
  }

  /**
   * Users can be listed.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserList(
    final IdDatabaseTransactionType t)
    throws Exception
  {
    final var adminId =
      databaseCreateAdminInitial(t, "admin", "12345678");

    t.adminIdSet(adminId);

    final var users =
      t.queries(IdDatabaseUsersQueriesType.class);

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
      final var parameters =
        new IdUserSearchParameters(
          new IdTimeRange(now.minusDays(1L), now.plusDays(1L)),
          new IdTimeRange(now.minusDays(1L), now.plusDays(1L)),
          Optional.empty(),
          new IdUserColumnOrdering(BY_IDNAME, true),
          600
        );

      final var search =
        users.userSearch(parameters);
      final var page =
        search.pageCurrent(users);

      assertEquals(500, page.items().size());

      for (int index = 0; index < 500; ++index) {
        final var name =
          new IdName("someone_%03d".formatted(index));
        final var realName =
          new IdRealName("someone %03d".formatted(index));

        final var u = page.items().get(index);
        assertEquals(name, u.idName());
        assertEquals(realName, u.realName());
      }
    }

    {
      final var parameters =
        new IdUserSearchParameters(
          new IdTimeRange(now.minusDays(1L), now.plusDays(1L)),
          new IdTimeRange(now.minusDays(1L), now.plusDays(1L)),
          Optional.empty(),
          new IdUserColumnOrdering(BY_IDNAME, false),
          600
        );

      final var search =
        users.userSearch(parameters);
      final var page =
        search.pageCurrent(users);

      assertEquals(500, page.items().size());

      for (int index = 0; index < 500; ++index) {
        final var id =
          500 - (index + 1);
        final var name =
          new IdName("someone_%03d".formatted(id));
        final var realName =
          new IdRealName("someone %03d".formatted(id));

        final var u = page.items().get(index);
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
  public void testUserSearchPaging(
    final IdDatabaseTransactionType t)
    throws Exception
  {
    final var adminId =
      databaseCreateAdminInitial(t, "admin", "12345678");

    t.adminIdSet(adminId);

    final var users =
      t.queries(IdDatabaseUsersQueriesType.class);

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

    final var search =
      users.userSearch(
        new IdUserSearchParameters(
          new IdTimeRange(now.minusDays(1L), now.plusDays(1L)),
          new IdTimeRange(now.minusDays(1L), now.plusDays(1L)),
          Optional.empty(),
          new IdUserColumnOrdering(BY_IDNAME, true),
          150
        )
      );

    var page = search.pageCurrent(users);
    assertEquals(1, page.pageIndex());
    assertEquals(150, page.items().size());
    checkPage(0, 150, page.items());

    page = search.pageNext(users);
    assertEquals(150, page.items().size());
    assertEquals(2, page.pageIndex());
    checkPage(150, 300, page.items());

    page = search.pageNext(users);
    assertEquals(150, page.items().size());
    assertEquals(3, page.pageIndex());
    checkPage(300, 450, page.items());

    page = search.pageNext(users);
    assertEquals(50, page.items().size());
    assertEquals(4, page.pageIndex());
    checkPage(450, 500, page.items());

    page = search.pagePrevious(users);
    assertEquals(150, page.items().size());
    assertEquals(3, page.pageIndex());
    checkPage(300, 450, page.items());

    page = search.pagePrevious(users);
    assertEquals(150, page.items().size());
    assertEquals(2, page.pageIndex());
    checkPage(150, 300, page.items());

    page = search.pagePrevious(users);
    assertEquals(150, page.items().size());
    assertEquals(1, page.pageIndex());
    checkPage(0, 150, page.items());
  }

  /**
   * Users can be listed and paging works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserSearchByEmailPaging(
    final IdDatabaseTransactionType t)
    throws Exception
  {
    final var adminId =
      databaseCreateAdminInitial(t, "admin", "12345678");

    t.adminIdSet(adminId);

    final var users =
      t.queries(IdDatabaseUsersQueriesType.class);

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

    final var search =
      users.userSearchByEmail(
        new IdUserSearchByEmailParameters(
          new IdTimeRange(now.minusDays(1L), now.plusDays(1L)),
          new IdTimeRange(now.minusDays(1L), now.plusDays(1L)),
          "0@example.com",
          new IdUserColumnOrdering(BY_IDNAME, true),
          150
        ));

    final var page = search.pageCurrent(users);
    final List<IdUserSummary> items = page.items();
    assertEquals(1, page.pageIndex());
    assertEquals(1, page.pageCount());
    assertEquals(50, items.size());

    for (final var item : items) {
      assertTrue(item.idName().value().endsWith("0"));
    }
  }

  /**
   * Users can be listed/searched and paging works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserListSearchPaging(
    final IdDatabaseTransactionType t)
    throws Exception
  {
    final var adminId =
      databaseCreateAdminInitial(t, "admin", "12345678");

    t.adminIdSet(adminId);

    final var users =
      t.queries(IdDatabaseUsersQueriesType.class);

    final var now =
      now();
    final var password =
      databaseGenerateBadPassword();
    final var userSet =
      IdTestUserSet.users();

    for (final var u : userSet) {
      users.userCreate(
        u.id(),
        u.idName(),
        u.realName(),
        new IdEmail(u.idName() + "@example.com"),
        now,
        password
      );
    }

    final var search =
      users.userSearch(new IdUserSearchParameters(
        new IdTimeRange(now.minusDays(1L), now.plusDays(1L)),
        new IdTimeRange(now.minusDays(1L), now.plusDays(1L)),
        Optional.of("od"),
        new IdUserColumnOrdering(BY_IDNAME, true),
        150
      ));


    final var page = search.pageCurrent(users);
    assertEquals(1, page.pageIndex());
    assertEquals(1, page.pageCount());
    assertEquals(39, page.items().size());

    final var expected = Set.of(
      "Gabfests Godlessness",
      "Neurotics Odorous",
      "Herniates Monodies",
      "Leaks Floodwater",
      "Remarriage Orthodox",
      "Moodiest Desiccants",
      "Tipples Iodising",
      "Modifiable Fortieths",
      "Angola Bodging",
      "Sorts Produces",
      "Doodlebugs Jibbed",
      "Widely Warmblooded",
      "Proposes Eurodollar",
      "Boyhood Wheeziness",
      "Ploughs Modules",
      "Desertification Oddments",
      "Egotist Bloodsucker",
      "Litmus Sod",
      "Stranding Methodicalness",
      "Booing Nickelodeons",
      "Roomers Antipodean",
      "Cruets Biodiversity",
      "Personage Bloodthirstier",
      "Wormwood Soundproofs",
      "Embodies Incinerator",
      "Unicode Mahatmas",
      "Shoddiness Bestride",
      "Ghettoises Geodesy",
      "Voodooing Calculating",
      "Podiatrist Brexit",
      "Hodges Sloucher",
      "Powhatan Disembodied",
      "Productive Matriarchy",
      "Cantor Bloodline",
      "Goldenrod Treacherous",
      "Lunchroom Nodes",
      "Flammability Modernly",
      "Strip Nobody",
      "Spume Podding"
    );

    for (final var e : expected) {
      assertTrue(
        page.items().stream()
          .anyMatch(u -> Objects.equals(u.realName().value(), e)),
        "Must contain " + e
      );
    }
  }

  /**
   * Updating a user works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserUpdate(
    final IdDatabaseTransactionType t)
    throws Exception
  {
    final var adminId =
      databaseCreateAdminInitial(t, "admin", "12345678");

    t.adminIdSet(adminId);

    final var users =
      t.queries(IdDatabaseUsersQueriesType.class);

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

    t.userIdSet(user.id());

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
      t,
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
  public void testUserUpdateNonexistent(
    final IdDatabaseTransactionType t)
    throws Exception
  {
    final var adminId =
      databaseCreateAdminInitial(t, "admin", "12345678");

    t.adminIdSet(adminId);

    final var users =
      t.queries(IdDatabaseUsersQueriesType.class);

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

    t.userIdSet(user.id());

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
  public void testUserEmailAddresses(
    final IdDatabaseTransactionType t)
    throws Exception
  {
    final var adminId =
      databaseCreateAdminInitial(t, "admin", "12345678");

    t.adminIdSet(adminId);

    final var users =
      t.queries(IdDatabaseUsersQueriesType.class);

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

  /**
   * Deleting a user fails for nonexistent users.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserDeleteNonexistent(
    final IdDatabaseTransactionType t)
    throws Exception
  {
    final var adminId =
      databaseCreateAdminInitial(t, "admin", "12345678");

    t.adminIdSet(adminId);

    final var users =
      t.queries(IdDatabaseUsersQueriesType.class);

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        users.userDelete(randomUUID());
      });

    assertEquals(USER_NONEXISTENT, ex.errorCode());
  }

  /**
   * Deleting a user works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserDelete(
    final IdDatabaseTransactionType t)
    throws Exception
  {
    final var adminId =
      databaseCreateAdminInitial(t, "admin", "12345678");

    t.adminIdSet(adminId);

    final var users =
      t.queries(IdDatabaseUsersQueriesType.class);

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

    t.commit();

    users.userDelete(reqId);

    t.commit();

    {
      final var ex =
        assertThrows(IdDatabaseException.class, () -> {
          users.userGetRequire(reqId);
        });

      assertEquals(USER_NONEXISTENT, ex.errorCode());
    }

    {
      final var ex =
        assertThrows(IdDatabaseException.class, () -> {
          users.userCreate(
            reqId,
            new IdName("someone"),
            new IdRealName("someone"),
            new IdEmail("someone@example.com"),
            now,
            password
          );
        });

      assertEquals(USER_DUPLICATE_ID, ex.errorCode());
    }

    checkAuditLog(
      t,
      new ExpectedEvent("ADMIN_CREATED", adminId.toString()),
      new ExpectedEvent("USER_CREATED", user.id().toString()),
      new ExpectedEvent(
        "USER_EMAIL_REMOVED",
        user.id() + "|someone@example.com"),
      new ExpectedEvent("USER_DELETED", user.id().toString())
    );
  }

  /**
   * Bans work.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserBan(
    final IdDatabaseTransactionType t)
    throws Exception
  {
    final var adminId =
      databaseCreateAdminInitial(t, "admin", "12345678");

    final var user =
      IdDatabaseExtension.databaseCreateUserInitial(t, adminId, "someone", "12345678");

    t.adminIdSet(adminId);

    final var users =
      t.queries(IdDatabaseUsersQueriesType.class);

    final var now =
      timeNow();

    t.adminIdSet(adminId);

    final var ban = new IdBan(user, "No reason.", Optional.of(now));
    users.userBanCreate(ban);
    assertEquals(Optional.of(ban), users.userBanGet(user));
    users.userBanDelete(ban);
    assertEquals(Optional.empty(), users.userBanGet(user));

    checkAuditLog(
      t,
      new ExpectedEvent("ADMIN_CREATED", adminId.toString()),
      new ExpectedEvent("USER_CREATED", user.toString()),
      new ExpectedEvent("USER_BANNED", user.toString()),
      new ExpectedEvent("USER_BAN_REMOVED", user.toString())
    );
  }

  /**
   * Emails are case-insensitive.
   *
   * @throws Exception On errors
   */

  @Test
  public void testEmailCaseInsensitive(
    final IdDatabaseTransactionType t)
    throws Exception
  {
    final var adminId =
      databaseCreateAdminInitial(t, "admin", "12345678");

    final var user =
      IdDatabaseExtension.databaseCreateUserInitial(t, adminId, "someone", "12345678");

    t.adminIdSet(adminId);

    final var users =
      t.queries(IdDatabaseUsersQueriesType.class);

    users.userEmailAdd(user, new IdEmail("SOMEONE@EXAMPLE.ORG"));

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        users.userEmailAdd(user, new IdEmail("someone@example.org"));
      });
    assertEquals(EMAIL_DUPLICATE, ex.errorCode());
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

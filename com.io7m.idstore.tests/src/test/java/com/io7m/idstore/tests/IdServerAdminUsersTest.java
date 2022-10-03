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

import com.io7m.idstore.admin_client.IdAClients;
import com.io7m.idstore.admin_client.api.IdAClientException;
import com.io7m.idstore.admin_client.api.IdAClientType;
import com.io7m.idstore.model.IdBan;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdNonEmptyList;
import com.io7m.idstore.model.IdPage;
import com.io7m.idstore.model.IdPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdTimeRange;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.model.IdUserColumnOrdering;
import com.io7m.idstore.model.IdUserOrdering;
import com.io7m.idstore.model.IdUserSearchByEmailParameters;
import com.io7m.idstore.model.IdUserSearchParameters;
import com.io7m.idstore.model.IdUserSummary;
import com.io7m.idstore.user_client.IdUClients;
import com.io7m.idstore.user_client.api.IdUClientException;
import com.io7m.idstore.user_client.api.IdUClientType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.idstore.model.IdUserColumn.BY_IDNAME;
import static java.util.Optional.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class IdServerAdminUsersTest extends IdWithServerContract
{
  private static final IdTimeRange TIME_LARGE_RANGE =
    new IdTimeRange(
      OffsetDateTime.now().minusDays(30L),
      OffsetDateTime.now().plusDays(30L)
    );

  private static final IdUserOrdering ORDER_BY_IDNAME =
    new IdUserOrdering(
      List.of(new IdUserColumnOrdering(BY_IDNAME, true))
    );

  private IdAClients clients;
  private IdAClientType client;
  private IdUClients userClients;
  private IdUClientType userClient;

  @BeforeEach
  public void setup()
  {
    this.clients = new IdAClients();
    this.client = this.clients.create(Locale.getDefault());
    this.userClients = new IdUClients();
    this.userClient = this.userClients.create(Locale.getDefault());
  }

  @AfterEach
  public void tearDown()
    throws Exception
  {
    this.client.close();
  }

  /**
   * Logging in works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginSelf()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");

    this.client.login("admin", "12345678", this.serverAdminAPIURL());

    final var self = this.client.adminSelf();
    assertEquals("admin", self.realName().value());
  }

  /**
   * Logging in fails with the wrong username.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginFailsUsername()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    final var userId =
      this.serverCreateUser(admin, "someone");

    final var ex =
      Assertions.assertThrows(IdAClientException.class, () -> {
        this.client.login("admin1", "12345678", this.serverAdminAPIURL());
      });

    assertTrue(
      ex.getMessage().contains("error-authentication"),
      ex.getMessage());
  }

  /**
   * Logging in fails with the wrong password.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginFailsPassword()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    final var userId =
      this.serverCreateUser(admin, "someone");

    final var ex =
      Assertions.assertThrows(IdAClientException.class, () -> {
        this.client.login("admin", "123456789", this.serverAdminAPIURL());
      });

    assertTrue(
      ex.getMessage().contains("error-authentication"),
      ex.getMessage());
  }

  /**
   * Searching users works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserSearch()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");

    for (int index = 0; index < 1033; ++index) {
      this.serverCreateUser(admin, "user-%04d".formatted(index));
    }

    this.client.login(
      "admin",
      "12345678",
      this.serverAdminAPIURL()
    );

    {
      final var p =
        this.client.userSearchBegin(
          new IdUserSearchParameters(
            TIME_LARGE_RANGE,
            TIME_LARGE_RANGE,
            empty(),
            ORDER_BY_IDNAME,
            100
          )
        );

      assertEquals(0, p.pageIndex());
      assertEquals(0, p.pageFirstOffset());
      assertEquals(10, p.pageCount());
      checkItems(p, 0, 100);
    }

    for (int page = 1; page < 10; ++page) {
      final var p = this.client.userSearchNext();
      assertEquals(page, p.pageIndex());
      assertEquals(page * 100, p.pageFirstOffset());
      assertEquals(10, p.pageCount());
      checkItems(p, page * 100, 100);
    }

    {
      final var p = this.client.userSearchNext();
      assertEquals(10, p.pageIndex());
      assertEquals(1000, p.pageFirstOffset());
      assertEquals(10, p.pageCount());
      checkItems(p, 1000, 33);
    }

    for (int page = 9; page >= 0; --page) {
      final var p = this.client.userSearchPrevious();
      assertEquals(page, p.pageIndex());
      assertEquals(page * 100, p.pageFirstOffset());
      assertEquals(10, p.pageCount());
      checkItems(p, page * 100, 100);
    }
  }

  /**
   * Searching users works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserSearchByEmail()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");

    for (int index = 0; index < 50; ++index) {
      this.serverCreateUser(admin, "user-%04d".formatted(index));
    }

    this.client.login(
      "admin",
      "12345678",
      this.serverAdminAPIURL()
    );

    {
      final var p =
        this.client.userSearchByEmailBegin(
          new IdUserSearchByEmailParameters(
            TIME_LARGE_RANGE,
            TIME_LARGE_RANGE,
            "@example.com",
            ORDER_BY_IDNAME,
            10
          )
        );

      assertEquals(0, p.pageIndex());
      assertEquals(0, p.pageFirstOffset());
      assertEquals(5, p.pageCount());
      assertEquals(10, p.items().size());
      checkItems(p, 0, 10);
    }

    {
      final var p = this.client.userSearchByEmailNext();
      assertEquals(1, p.pageIndex());
      assertEquals(10, p.pageFirstOffset());
      assertEquals(5, p.pageCount());
      assertEquals(10, p.items().size());
      checkItems(p, 10, 10);
    }

    {
      final var p = this.client.userSearchByEmailNext();
      assertEquals(2, p.pageIndex());
      assertEquals(20, p.pageFirstOffset());
      assertEquals(5, p.pageCount());
      assertEquals(10, p.items().size());
      checkItems(p, 20, 10);
    }

    {
      final var p = this.client.userSearchByEmailNext();
      assertEquals(3, p.pageIndex());
      assertEquals(30, p.pageFirstOffset());
      assertEquals(5, p.pageCount());
      assertEquals(10, p.items().size());
      checkItems(p, 30, 10);
    }

    {
      final var p = this.client.userSearchByEmailNext();
      assertEquals(4, p.pageIndex());
      assertEquals(40, p.pageFirstOffset());
      assertEquals(5, p.pageCount());
      assertEquals(10, p.items().size());
      checkItems(p, 40, 10);
    }

    {
      final var p = this.client.userSearchByEmailNext();
      assertEquals(5, p.pageIndex());
      assertEquals(50, p.pageFirstOffset());
      assertEquals(5, p.pageCount());
      assertEquals(0, p.items().size());
    }

    {
      final var p = this.client.userSearchByEmailPrevious();
      assertEquals(4, p.pageIndex());
      assertEquals(40, p.pageFirstOffset());
      assertEquals(5, p.pageCount());
      assertEquals(10, p.items().size());
      checkItems(p, 40, 10);
    }

    {
      final var p = this.client.userSearchByEmailPrevious();
      assertEquals(3, p.pageIndex());
      assertEquals(30, p.pageFirstOffset());
      assertEquals(5, p.pageCount());
      assertEquals(10, p.items().size());
      checkItems(p, 30, 10);
    }

    {
      final var p = this.client.userSearchByEmailPrevious();
      assertEquals(2, p.pageIndex());
      assertEquals(20, p.pageFirstOffset());
      assertEquals(5, p.pageCount());
      assertEquals(10, p.items().size());
      checkItems(p, 20, 10);
    }

    {
      final var p = this.client.userSearchByEmailPrevious();
      assertEquals(1, p.pageIndex());
      assertEquals(10, p.pageFirstOffset());
      assertEquals(5, p.pageCount());
      assertEquals(10, p.items().size());
      checkItems(p, 10, 10);
    }

    {
      final var p = this.client.userSearchByEmailPrevious();
      assertEquals(0, p.pageIndex());
      assertEquals(0, p.pageFirstOffset());
      assertEquals(5, p.pageCount());
      assertEquals(10, p.items().size());
      checkItems(p, 0, 10);
    }
  }

  /**
   * Creating, updating, and retrieving users works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserUpdate()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");

    this.client.login(
      "admin",
      "12345678",
      this.serverAdminAPIURL()
    );

    final var id =
      UUID.randomUUID();
    final var password0 =
      IdPasswordAlgorithmPBKDF2HmacSHA256.create()
        .createHashed("12345678");
    final var password1 =
      IdPasswordAlgorithmPBKDF2HmacSHA256.create()
        .createHashed("abcdefgh");

    final var user0 =
      this.client.userCreate(
        Optional.of(id),
        new IdName("someone-0"),
        new IdRealName("Someone R. Incognito"),
        new IdEmail("someone-0@example.com"),
        password0
      );

    final var user0r =
      new IdUser(
        id,
        new IdName("someone-1"),
        new IdRealName("Someone R. Else"),
        IdNonEmptyList.single(new IdEmail("someone-1@example.com")),
        OffsetDateTime.now(this.clock()),
        OffsetDateTime.now(this.clock()),
        password1
      );

    final var user1 =
      this.client.userUpdate(
        user0.id(),
        Optional.of(user0r.idName()),
        Optional.of(user0r.realName()),
        Optional.of(user0r.password())
      );

    assertEquals(user0r.id(), user1.id());
    assertEquals(user0r.idName(), user1.idName());
    assertEquals(user0r.realName(), user1.realName());
    assertEquals(user0r.password(), user1.password());
  }

  /**
   * Deleting users works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserDeleting()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");

    this.client.login(
      "admin",
      "12345678",
      this.serverAdminAPIURL()
    );

    final var id =
      UUID.randomUUID();
    final var password0 =
      IdPasswordAlgorithmPBKDF2HmacSHA256.create()
        .createHashed("12345678");

    final var user0 =
      this.client.userCreate(
        Optional.of(id),
        new IdName("someone-0"),
        new IdRealName("Someone R. Incognito"),
        new IdEmail("someone-0@example.com"),
        password0
      );

    this.client.userDelete(id);

    final var ex =
      Assertions.assertThrows(IdAClientException.class, () -> {
        this.client.userUpdate(
          user0.id(),
          empty(),
          empty(),
          empty());
      });

    assertTrue(
      ex.getMessage().contains("error-user-nonexistent"),
      ex.getMessage());
  }


  /**
   * Banning causes logins to fail.
   *
   * @throws Exception On errors
   */

  @Test
  public void testBanLogin()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");

    final var other =
      this.serverCreateUser(admin, "other");

    this.client.login("admin", "12345678", this.serverAdminAPIURL());

    this.client.userBanCreate(new IdBan(
      other,
      "Spite",
      Optional.of(timeNow().plusDays(1L))
    ));

    final var ex =
      assertThrows(IdUClientException.class, () -> {
        this.userClient.login("other", "12345678", this.serverUserAPIURL());
      });

    assertTrue(
      ex.getMessage().contains("error-banned"),
      ex.getMessage());
  }

  /**
   * Banning causes logins to fail.
   *
   * @throws Exception On errors
   */

  @Test
  public void testBanLoginPermanent()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");

    final var other =
      this.serverCreateUser(admin, "other");

    this.client.login("admin", "12345678", this.serverAdminAPIURL());

    this.client.userBanCreate(new IdBan(
      other,
      "Spite",
      Optional.empty()
    ));

    final var ex =
      assertThrows(IdUClientException.class, () -> {
        this.userClient.login("other", "12345678", this.serverUserAPIURL());
      });

    assertTrue(
      ex.getMessage().contains("error-banned"),
      ex.getMessage());
  }

  /**
   * Expired bans don't cause logins to fail.
   *
   * @throws Exception On errors
   */

  @Test
  public void testBanLoginExpired()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");

    final var other =
      this.serverCreateUser(admin, "other");

    this.client.login("admin", "12345678", this.serverAdminAPIURL());

    this.client.userBanCreate(new IdBan(
      other,
      "Spite",
      Optional.of(timeNow().minusYears(1000L))
    ));

    this.userClient.login("other", "12345678", this.serverUserAPIURL());
  }

  private static void checkItems(
    final IdPage<IdUserSummary> p,
    final int start,
    final int count)
  {
    final var u = p.items();
    for (int index = 0; index < count; ++index) {
      assertEquals(
        "user-%04d".formatted(Integer.valueOf(start + index)),
        u.get(index).idName().value()
      );
    }
  }
}

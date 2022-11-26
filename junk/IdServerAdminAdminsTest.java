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
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdAdminColumnOrdering;
import com.io7m.idstore.model.IdAdminPermissionSet;
import com.io7m.idstore.model.IdAdminSearchByEmailParameters;
import com.io7m.idstore.model.IdAdminSearchParameters;
import com.io7m.idstore.model.IdAdminSummary;
import com.io7m.idstore.model.IdBan;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdNonEmptyList;
import com.io7m.idstore.model.IdPage;
import com.io7m.idstore.model.IdPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.idstore.model.IdPasswordAlgorithmRedacted;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdTimeRange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.OPERATION_NOT_PERMITTED;
import static com.io7m.idstore.model.IdAdminColumn.BY_IDNAME;
import static com.io7m.idstore.model.IdAdminPermission.AUDIT_READ;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class IdServerAdminAdminsTest extends IdWithServerContract
{
  private static final IdTimeRange TIME_LARGE_RANGE =
    new IdTimeRange(
      OffsetDateTime.now().minusDays(30L),
      OffsetDateTime.now().plusDays(30L)
    );

  private static final IdAdminColumnOrdering ORDER_BY_IDNAME =
    new IdAdminColumnOrdering(BY_IDNAME, true);

  private IdAClients clients;
  private IdAClientType client;

  @BeforeEach
  public void setup()
  {
    this.clients = new IdAClients();
    this.client = this.clients.create(Locale.getDefault());
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
   * Logging in fails with the wrong adminname.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginFailsAdminname()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");

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

    final var ex =
      Assertions.assertThrows(IdAClientException.class, () -> {
        this.client.login("admin", "123456789", this.serverAdminAPIURL());
      });

    assertTrue(
      ex.getMessage().contains("error-authentication"),
      ex.getMessage());
  }

  /**
   * Searching admins works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminSearch()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");

    for (int index = 0; index < 1033; ++index) {
      this.serverCreateAdmin(admin, "admin-%04d".formatted(index));
    }

    this.client.login(
      "admin",
      "12345678",
      this.serverAdminAPIURL()
    );

    {
      var offset = 0;
      for (int pageIndex = 1; pageIndex <= 11; ++pageIndex) {
        final IdPage<IdAdminSummary> page;
        if (pageIndex == 1) {
          page = this.client.adminSearchBegin(
            new IdAdminSearchParameters(
              TIME_LARGE_RANGE,
              TIME_LARGE_RANGE,
              Optional.of("admin-"),
              ORDER_BY_IDNAME,
              100
            )
          );
        } else {
          page = this.client.adminSearchNext();
        }

        assertEquals(pageIndex, page.pageIndex());
        assertEquals(offset, page.pageFirstOffset());
        assertEquals(11, page.pageCount());

        if (pageIndex == 11) {
          checkItems(page, offset, 33);
        } else {
          checkItems(page, offset, 100);
        }
        offset += 100;
      }
    }

    {
      var offset = 900;
      for (int pageIndex = 10; pageIndex >= 1; --pageIndex) {
        final IdPage<IdAdminSummary> page =
          this.client.adminSearchPrevious();

        assertEquals(pageIndex, page.pageIndex());
        assertEquals(offset, page.pageFirstOffset());
        assertEquals(11, page.pageCount());

        checkItems(page, offset, 100);
        offset -= 100;
      }
    }
  }

  /**
   * Searching admins works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminSearchByEmail()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");

    for (int index = 0; index < 50; ++index) {
      this.serverCreateAdmin(admin, "admin-%04d".formatted(index));
    }

    this.client.login(
      "admin",
      "12345678",
      this.serverAdminAPIURL()
    );

    {
      final var p =
        this.client.adminSearchByEmailBegin(
          new IdAdminSearchByEmailParameters(
            TIME_LARGE_RANGE,
            TIME_LARGE_RANGE,
            "extra_",
            ORDER_BY_IDNAME,
            10
          )
        );

      assertEquals(1, p.pageIndex());
      assertEquals(0, p.pageFirstOffset());
      assertEquals(6, p.pageCount());
      assertEquals(10, p.items().size());
      checkItems(p, 0, 10);
    }

    {
      final var p = this.client.adminSearchByEmailNext();
      assertEquals(2, p.pageIndex());
      assertEquals(10, p.pageFirstOffset());
      assertEquals(6, p.pageCount());
      assertEquals(10, p.items().size());
      checkItems(p, 10, 10);
    }

    {
      final var p = this.client.adminSearchByEmailNext();
      assertEquals(3, p.pageIndex());
      assertEquals(20, p.pageFirstOffset());
      assertEquals(6, p.pageCount());
      assertEquals(10, p.items().size());
      checkItems(p, 20, 10);
    }

    {
      final var p = this.client.adminSearchByEmailNext();
      assertEquals(4, p.pageIndex());
      assertEquals(30, p.pageFirstOffset());
      assertEquals(6, p.pageCount());
      assertEquals(10, p.items().size());
      checkItems(p, 30, 10);
    }

    {
      final var p = this.client.adminSearchByEmailNext();
      assertEquals(5, p.pageIndex());
      assertEquals(40, p.pageFirstOffset());
      assertEquals(6, p.pageCount());
      assertEquals(10, p.items().size());
      checkItems(p, 40, 10);
    }

    {
      final var p = this.client.adminSearchByEmailNext();
      assertEquals(6, p.pageIndex());
      assertEquals(50, p.pageFirstOffset());
      assertEquals(6, p.pageCount());
      assertEquals(0, p.items().size());
    }

    {
      final var p = this.client.adminSearchByEmailPrevious();
      assertEquals(5, p.pageIndex());
      assertEquals(40, p.pageFirstOffset());
      assertEquals(6, p.pageCount());
      assertEquals(10, p.items().size());
      checkItems(p, 40, 10);
    }

    {
      final var p = this.client.adminSearchByEmailPrevious();
      assertEquals(4, p.pageIndex());
      assertEquals(30, p.pageFirstOffset());
      assertEquals(6, p.pageCount());
      assertEquals(10, p.items().size());
      checkItems(p, 30, 10);
    }

    {
      final var p = this.client.adminSearchByEmailPrevious();
      assertEquals(3, p.pageIndex());
      assertEquals(20, p.pageFirstOffset());
      assertEquals(6, p.pageCount());
      assertEquals(10, p.items().size());
      checkItems(p, 20, 10);
    }

    {
      final var p = this.client.adminSearchByEmailPrevious();
      assertEquals(2, p.pageIndex());
      assertEquals(10, p.pageFirstOffset());
      assertEquals(6, p.pageCount());
      assertEquals(10, p.items().size());
      checkItems(p, 10, 10);
    }

    {
      final var p = this.client.adminSearchByEmailPrevious();
      assertEquals(1, p.pageIndex());
      assertEquals(0, p.pageFirstOffset());
      assertEquals(6, p.pageCount());
      assertEquals(10, p.items().size());
      checkItems(p, 0, 10);
    }
  }

  /**
   * Creating, updating, and retrieving admins works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminUpdate()
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

    final var admin0 =
      this.client.adminCreate(
        Optional.of(id),
        new IdName("someone-0"),
        new IdRealName("Someone R. Incognito"),
        new IdEmail("someone-0@example.com"),
        password0,
        IdAdminPermissionSet.all()
      );

    final var admin0r =
      new IdAdmin(
        id,
        new IdName("someone-1"),
        new IdRealName("Someone R. Else"),
        IdNonEmptyList.single(new IdEmail("someone-1@example.com")),
        OffsetDateTime.now(this.clock()),
        OffsetDateTime.now(this.clock()),
        password1,
        IdAdminPermissionSet.empty()
      );

    final var admin1 =
      this.client.adminUpdate(
        admin0r.id(),
        Optional.of(admin0r.idName()),
        Optional.of(admin0r.realName()),
        Optional.of(admin0r.password()));

    assertEquals(admin0r.id(), admin1.id());
    assertEquals(admin0r.idName(), admin1.idName());
    assertEquals(admin0r.realName(), admin1.realName());
    assertEquals(
      IdPasswordAlgorithmRedacted.create().createHashed(""),
      admin1.password());
  }

  /**
   * Deleting admins works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminDeleting()
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

    final var admin0 =
      this.client.adminCreate(
        Optional.of(id),
        new IdName("someone-0"),
        new IdRealName("Someone R. Incognito"),
        new IdEmail("someone-0@example.com"),
        password0,
        IdAdminPermissionSet.empty()
      );

    this.client.adminDelete(id);

    final var ex =
      assertThrows(IdAClientException.class, () -> {
        this.client.adminUpdate(
          admin0.id(),
          Optional.of(admin0.idName()),
          Optional.of(admin0.realName()),
          Optional.of(admin0.password())
        );
      });

    assertTrue(
      ex.getMessage().contains("error-admin-nonexistent"),
      ex.getMessage());
  }

  /**
   * Adding and removing emails works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminEmailAddRemove()
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

    final var admin0 =
      this.client.adminCreate(
        Optional.of(id),
        new IdName("someone-0"),
        new IdRealName("Someone R. Incognito"),
        new IdEmail("someone-0@example.com"),
        password0,
        IdAdminPermissionSet.all()
      );

    final var admin1 =
      this.client.adminEmailAdd(
        admin0.id(), new IdEmail("someone-1@example.com"));

    assertTrue(
      admin1.emails().contains(new IdEmail("someone-1@example.com"))
    );

    final var admin2 =
      this.client.adminEmailRemove(
        admin0.id(), new IdEmail("someone-1@example.com"));

    assertFalse(
      admin2.emails().contains(new IdEmail("someone-1@example.com"))
    );
  }

  /**
   * Adding and removing emails works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminEmailAddRemoveSelf()
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

    final var admin1 =
      this.client.adminEmailAdd(
        admin, new IdEmail("someone-x@example.com"));

    assertTrue(
      admin1.emails().contains(new IdEmail("someone-x@example.com"))
    );

    final var admin2 =
      this.client.adminEmailRemove(
        admin, new IdEmail("someone-x@example.com"));

    assertFalse(
      admin2.emails().contains(new IdEmail("someone-x@example.com"))
    );
  }

  /**
   * Adding and removing permissions works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminPermissionGrantRemove()
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

    final var admin0 =
      this.client.adminCreate(
        Optional.of(id),
        new IdName("someone-0"),
        new IdRealName("Someone R. Incognito"),
        new IdEmail("someone-0@example.com"),
        password0,
        IdAdminPermissionSet.empty()
      );

    final var admin1 =
      this.client.adminPermissionGrant(admin0.id(), AUDIT_READ);

    assertTrue(
      admin1.permissions().implies(AUDIT_READ)
    );

    final var admin2 =
      this.client.adminPermissionRevoke(admin0.id(), AUDIT_READ);

    assertFalse(
      admin2.permissions().implies(AUDIT_READ)
    );
  }

  /**
   * Adding and removing permissions works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminPermissionGrantRemoveSelf()
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

    final var admin2 =
      this.client.adminPermissionRevoke(admin, AUDIT_READ);

    assertFalse(
      admin2.permissions().implies(AUDIT_READ)
    );
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
      this.serverCreateAdmin(admin, "other");

    this.client.login("admin", "12345678", this.serverAdminAPIURL());

    this.client.adminBanCreate(new IdBan(
      other,
      "Spite",
      Optional.of(timeNow().plusDays(1L))
    ));

    final var ex =
      assertThrows(IdAClientException.class, () -> {
        this.client.login("other", "12345678", this.serverAdminAPIURL());
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
      this.serverCreateAdmin(admin, "other");

    this.client.login("admin", "12345678", this.serverAdminAPIURL());

    this.client.adminBanCreate(new IdBan(
      other,
      "Spite",
      Optional.empty()
    ));

    final var ex =
      assertThrows(IdAClientException.class, () -> {
        this.client.login("other", "12345678", this.serverAdminAPIURL());
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
      this.serverCreateAdmin(admin, "other");

    this.client.login("admin", "12345678", this.serverAdminAPIURL());

    this.client.adminBanCreate(new IdBan(
      other,
      "Spite",
      Optional.of(timeNow().minusYears(1000L))
    ));

    this.client.login("other", "12345678", this.serverAdminAPIURL());
  }

  /**
   * Deleting admins works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminDelete()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin0 =
      this.serverCreateAdminInitial("admin", "12345678");
    final var admin1 =
      this.serverCreateAdmin(admin0, "other");

    this.client.login(
      "admin",
      "12345678",
      this.serverAdminAPIURL()
    );

    assertTrue(this.client.adminGet(admin1).isPresent());
    this.client.adminDelete(admin1);
    assertTrue(this.client.adminGet(admin1).isEmpty());
  }

  /**
   * An admin can't delete itself.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminDeleteSelf()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin0 =
      this.serverCreateAdminInitial("admin", "12345678");
    final var admin1 =
      this.serverCreateAdmin(admin0, "other");

    this.client.login(
      "other",
      "12345678",
      this.serverAdminAPIURL()
    );

    final var ex =
      assertThrows(IdAClientException.class, () -> {
        this.client.adminDelete(admin1);
      });

    assertEquals(OPERATION_NOT_PERMITTED, ex.errorCode());
  }

  private static void checkItems(
    final IdPage<IdAdminSummary> p,
    final int start,
    final int count)
  {
    final var u = p.items();
    for (int index = 0; index < count; ++index) {
      assertEquals(
        "admin-%04d".formatted(Integer.valueOf(start + index)),
        u.get(index).idName().value()
      );
    }
  }
}

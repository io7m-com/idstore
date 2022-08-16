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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class IdServerAdminUsersTest extends IdWithServerContract
{
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
    final var userId =
      this.serverCreateUser(admin, "someone");

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
}

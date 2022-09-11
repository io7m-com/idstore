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
import com.io7m.idstore.admin_client.api.IdAClientType;
import com.io7m.idstore.model.IdTimeRange;
import com.io7m.idstore.model.IdUserColumnOrdering;
import com.io7m.idstore.model.IdUserOrdering;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static com.io7m.idstore.model.IdUserColumn.BY_IDNAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class IdServerAdminAuditTest extends IdWithServerContract
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
   * Listing audit records works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAuditList()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");

    for (int index = 0; index < 100; ++index) {
      this.serverCreateUser(admin, "user-%04d".formatted(index));
    }

    this.client.login(
      "admin",
      "12345678",
      this.serverAdminAPIURL()
    );

    {
      this.client.auditSearchBegin(
        IdTimeRange.largest(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        10
      );
    }
  }
}

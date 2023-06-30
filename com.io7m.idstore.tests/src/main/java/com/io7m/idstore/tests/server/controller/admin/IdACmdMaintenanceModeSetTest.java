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

package com.io7m.idstore.tests.server.controller.admin;

import com.io7m.idstore.model.IdAdminPermissionSet;
import com.io7m.idstore.protocol.admin.IdACommandMaintenanceModeSet;
import com.io7m.idstore.protocol.admin.IdAResponseMaintenanceModeSet;
import com.io7m.idstore.server.controller.admin.IdACmdMaintenanceModeSet;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SECURITY_POLICY_DENIED;
import static com.io7m.idstore.model.IdAdminPermission.MAINTENANCE_MODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

public final class IdACmdMaintenanceModeSetTest
  extends IdACmdAbstractContract
{
  /**
   * Sending requires the MAINTENANCE_MODE permission.
   *
   * @throws Exception On errors
   */

  @Test
  public void testNotAllowed()
    throws Exception
  {
    /* Arrange. */

    final var admin =
      this.createAdmin("admin", IdAdminPermissionSet.empty());
    final var context =
      this.createContextAndSession(admin);

    /* Act. */

    final var handler = new IdACmdMaintenanceModeSet();

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(context, new IdACommandMaintenanceModeSet(
          Optional.of("Message!")
        ));
      });

    /* Assert. */

    assertEquals(SECURITY_POLICY_DENIED, ex.errorCode());
  }

  /**
   * Setting maintenance mode works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testSetOK()
    throws Exception
  {
    /* Arrange. */

    final var admin =
      this.createAdmin("admin", IdAdminPermissionSet.of(MAINTENANCE_MODE));
    final var context =
      this.createContextAndSession(admin);

    /* Act. */

    final var handler =
      new IdACmdMaintenanceModeSet();
    final var response =
      handler.execute(context, new IdACommandMaintenanceModeSet(
        Optional.of("Message!")
      ));

    /* Assert. */

    assertEquals(
      new IdAResponseMaintenanceModeSet(
        context.requestId(),
        "Server is in maintenance mode with message \"Message!\""
      ),
      response
    );

    verify(this.maintenance())
      .closeForMaintenance("Message!");
  }

  /**
   * Unsetting maintenance mode works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUnsetOK()
    throws Exception
  {
    /* Arrange. */

    final var admin =
      this.createAdmin("admin", IdAdminPermissionSet.of(MAINTENANCE_MODE));
    final var context =
      this.createContextAndSession(admin);

    /* Act. */

    final var handler =
      new IdACmdMaintenanceModeSet();
    final var response =
      handler.execute(context, new IdACommandMaintenanceModeSet(
        Optional.empty()
      ));

    /* Assert. */

    assertEquals(
      new IdAResponseMaintenanceModeSet(
        context.requestId(),
        "Server is now actively serving requests."
      ),
      response
    );

    verify(this.maintenance())
      .openForBusiness();
  }
}

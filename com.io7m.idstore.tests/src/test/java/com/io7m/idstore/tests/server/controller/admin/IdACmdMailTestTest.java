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
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdShortHumanToken;
import com.io7m.idstore.protocol.admin.IdACommandMailTest;
import com.io7m.idstore.protocol.admin.IdAResponseMailTest;
import com.io7m.idstore.server.controller.admin.IdACmdMailTest;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.service.templating.IdFMEmailTestData;
import com.io7m.idstore.server.service.templating.IdFMTemplateType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SECURITY_POLICY_DENIED;
import static com.io7m.idstore.model.IdAdminPermission.MAIL_TEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public final class IdACmdMailTestTest
  extends IdACmdAbstractContract
{
  /**
   * Sending requires the MAIL_TEST permission.
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

    final var handler = new IdACmdMailTest();

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(context, new IdACommandMailTest(
          new IdEmail("example@example.com"),
          new IdShortHumanToken("123456")
        ));
      });

    /* Assert. */

    assertEquals(SECURITY_POLICY_DENIED, ex.errorCode());
  }

  /**
   * Sending a test email works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testSendsOK()
    throws Exception
  {
    /* Arrange. */

    final var admin =
      this.createAdmin("admin", IdAdminPermissionSet.of(MAIL_TEST));
    final var context =
      this.createContextAndSession(admin);

    final var mail = this.mail();
    when(mail.sendMail(
      any(),
      any(),
      any(),
      any(),
      any(),
      any()
    )).thenReturn(CompletableFuture.completedFuture(null));

    final IdFMTemplateType<IdFMEmailTestData> template =
      mock(IdFMTemplateType.class);

    when(this.branding().title())
      .thenReturn("Example");
    when(this.templates().emailTestTemplate())
      .thenReturn(template);

    /* Act. */

    final var handler =
      new IdACmdMailTest();
    final var response =
      handler.execute(context, new IdACommandMailTest(
        new IdEmail("example@example.com"),
        new IdShortHumanToken("123456")
      ));

    /* Assert. */

    assertEquals(
      new IdAResponseMailTest(
        context.requestId(),
        new IdShortHumanToken("123456")
      ),
      response
    );

    verify(mail)
      .sendMail(
        any(),
        any(),
        eq(new IdEmail("example@example.com")),
        argThat(map -> {
          return Objects.equals(map.get("X-IDStore-Test-Token"), "123456");
        }),
        any(),
        any()
      );
  }

  /**
   * Sending a test email fails if the mail system fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testSendFails()
    throws Exception
  {
    /* Arrange. */

    final var admin =
      this.createAdmin("admin", IdAdminPermissionSet.of(MAIL_TEST));
    final var context =
      this.createContextAndSession(admin);

    final var mail = this.mail();
    when(mail.sendMail(
      any(),
      any(),
      any(),
      any(),
      any(),
      any()
    )).thenReturn(CompletableFuture.failedFuture(new IOException("Not good.")));

    final IdFMTemplateType<IdFMEmailTestData> template =
      mock(IdFMTemplateType.class);

    when(this.branding().title())
      .thenReturn("Example");
    when(this.templates().emailTestTemplate())
      .thenReturn(template);

    /* Act. */

    final var handler =
      new IdACmdMailTest();

    assertThrows(IdCommandExecutionFailure.class, () -> {
      handler.execute(context, new IdACommandMailTest(
        new IdEmail("example@example.com"),
        new IdShortHumanToken("123456")
      ));
    });

    /* Assert. */

    verify(mail)
      .sendMail(
        any(),
        any(),
        eq(new IdEmail("example@example.com")),
        argThat(map -> {
          return Objects.equals(map.get("X-IDStore-Test-Token"), "123456");
        }),
        any(),
        any()
      );
  }

  /**
   * Sending a test email fails if the mail system fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testSendFailsThrows()
    throws Exception
  {
    /* Arrange. */

    final var admin =
      this.createAdmin("admin", IdAdminPermissionSet.of(MAIL_TEST));
    final var context =
      this.createContextAndSession(admin);

    final var mail = this.mail();
    when(mail.sendMail(
      any(),
      any(),
      any(),
      any(),
      any(),
      any()
    )).thenThrow(new RuntimeException("Not good."));

    final IdFMTemplateType<IdFMEmailTestData> template =
      mock(IdFMTemplateType.class);

    when(this.branding().title())
      .thenReturn("Example");
    when(this.templates().emailTestTemplate())
      .thenReturn(template);

    /* Act. */

    final var handler =
      new IdACmdMailTest();

    assertThrows(IdCommandExecutionFailure.class, () -> {
      handler.execute(context, new IdACommandMailTest(
        new IdEmail("example@example.com"),
        new IdShortHumanToken("123456")
      ));
    });

    /* Assert. */

    verify(mail)
      .sendMail(
        any(),
        any(),
        eq(new IdEmail("example@example.com")),
        argThat(map -> {
          return Objects.equals(map.get("X-IDStore-Test-Token"), "123456");
        }),
        any(),
        any()
      );
  }
}
